/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.ui.views;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.mylar.context.core.ContextCorePlugin;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.internal.tasks.ui.ITasksUiConstants;
import org.eclipse.mylar.internal.tasks.ui.RetrieveTitleFromUrlJob;
import org.eclipse.mylar.internal.tasks.ui.actions.NewLocalTaskAction;
import org.eclipse.mylar.internal.tasks.ui.actions.TaskActivateAction;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.DateRangeContainer;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskListElement;
import org.eclipse.mylar.tasks.core.Task;
import org.eclipse.mylar.tasks.core.TaskCategory;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TaskTransfer;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.TasksUiUtil;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;

/**
 * @author Mik Kersten
 * @author Rob Elves (added URL based task creation support)
 */
public class TaskListDropAdapter extends ViewerDropAdapter {

	private Task newTask = null;

	private TransferData currentTransfer;

	public TaskListDropAdapter(Viewer viewer) {
		super(viewer);
		setFeedbackEnabled(true);
	}

	@Override
	public void dragOver(DropTargetEvent event) {
		// support dragging from sources only supporting DROP_LINK
		if (event.detail == DND.DROP_NONE && (event.operations & DND.DROP_LINK) == DND.DROP_LINK) {
			event.detail = DND.DROP_LINK;
		}
		super.dragOver(event);
	}

	@Override
	public boolean performDrop(Object data) {
		Object currentTarget = getCurrentTarget();
		List<ITask> tasksToMove = new ArrayList<ITask>();
		ISelection selection = ((TreeViewer) getViewer()).getSelection();
		if (isUrl(data) && createTaskFromUrl(data)) {
			tasksToMove.add(newTask);
		} else if (TaskTransfer.getInstance().isSupportedType(currentTransfer)) {
			for (Object selectedObject : ((IStructuredSelection) selection).toList()) {
				ITask toMove = null;
				if (selectedObject instanceof ITask) {
					toMove = (ITask) selectedObject;
				} 
				if (toMove != null) {
					tasksToMove.add(toMove);
				}
			}
		} else if (data instanceof String && createTaskFromString((String) data)) {
			tasksToMove.add(newTask);
		} else if (FileTransfer.getInstance().isSupportedType(currentTransfer)) {
			ITask targetTask = null;
			if (getCurrentTarget() instanceof ITask) {
				targetTask = (ITask) getCurrentTarget();
			}
			if (targetTask != null) {
				final String[] names = (String[]) data;
				boolean confirmed = MessageDialog.openConfirm(getViewer().getControl().getShell(),
						ITasksUiConstants.TITLE_DIALOG, "Overwrite the context of the target task with the source's?");
				if (confirmed) {
					String path = names[0];
					File file = new File(path);
					if (ContextCorePlugin.getContextManager().isValidContextFile(file)) {
						ContextCorePlugin.getContextManager().transferContextAndActivate(
								targetTask.getHandleIdentifier(), file);
						new TaskActivateAction().run(targetTask);
					}
				}
			}
		}

		for (ITask task : tasksToMove) {
			if (currentTarget instanceof TaskCategory) {
				TasksUiPlugin.getTaskListManager().getTaskList().moveToContainer((TaskCategory) currentTarget, task);
			} else if (currentTarget instanceof ITask) {
				ITask targetTask = (ITask) currentTarget;
				if (targetTask.getContainer() == null) {
					TasksUiPlugin.getTaskListManager().getTaskList().moveToRoot(task);
				} else {
					TasksUiPlugin.getTaskListManager().getTaskList().moveToContainer(targetTask.getContainer(), task);
				}
			} else if (currentTarget instanceof DateRangeContainer) {
				DateRangeContainer container = (DateRangeContainer)currentTarget;
				Calendar newSchedule = Calendar.getInstance();
				newSchedule.setTimeInMillis(container.getStart().getTimeInMillis());				
				TasksUiPlugin.getTaskListManager().setScheduledEndOfDay(newSchedule);
				TasksUiPlugin.getTaskListManager().setScheduledFor(task, newSchedule.getTime());
			} else if (currentTarget == null) {
				TasksUiPlugin.getTaskListManager().getTaskList().moveToRoot(newTask);
			}
		}

		// Make new task the current selection in the view
		if (newTask != null) {
			StructuredSelection ss = new StructuredSelection(newTask);
			getViewer().setSelection(ss);
			getViewer().refresh();
		}

		return true;

	}

	/**
	 * @return true if string is a http(s) url
	 */
	public boolean isUrl(Object data) {
		String uri = "";
		if (data instanceof String) {
			uri = (String) data;
			if ((uri.startsWith("http://") || uri.startsWith("https://"))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param data
	 *            string containing url and title separated by <quote>\n</quote>
	 * @return true if task succesfully created, false otherwise
	 */
	public boolean createTaskFromUrl(Object data) {
		if (!(data instanceof String))
			return false;

		String[] urlTransfer = ((String) data).split("\n");

		String url = "";
		String urlTitle = "<retrieving from URL>";

		if (urlTransfer.length > 0) {
			url = urlTransfer[0];
		} else {
			return false;
		}

		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getConnectorForRepositoryTaskUrl(
				url);
		if (connector != null) {
			String repositoryUrl = connector.getRepositoryUrlFromTaskUrl(url);
			String id = connector.getTaskIdFromTaskUrl(url);
			if (repositoryUrl == null || id == null) {
				return false;
			}
			for (TaskRepository repository : TasksUiPlugin.getRepositoryManager().getRepositories(
					connector.getRepositoryType())) {
				if (repository.getUrl().equals(repositoryUrl)) {
					try {
						newTask = connector.createTaskFromExistingId(repository, id, new NullProgressMonitor());

//						if (newTask instanceof AbstractRepositoryTask) {
//							// TODO: encapsulate in abstract connector
//							AbstractRepositoryTask repositoryTask = (AbstractRepositoryTask) newTask;
//							TasksUiPlugin.getDefault().getTaskDataManager().push(
//									RepositoryTaskHandleUtil.getHandle(repository.getUrl(), id),
//									repositoryTask.getTaskData());
//						}
						TasksUiUtil.refreshAndOpenTaskListElement(newTask);
						return true;
					} catch (CoreException e) {
						MylarStatusHandler.fail(e, "could not create task", false);
						return false;
					}
				}
			}
			return false;
		} else {
			// Removed in order to default to retrieving title from url rather
			// than
			// accepting what was sent by the brower's DnD code. (see bug
			// 114401)
			// If a Title is provided, use it.
			// if (urlTransfer.length > 1) {
			// urlTitle = urlTransfer[1];
			// }
			// if (urlTransfer.length < 2) { // no title provided
			// retrieveTaskDescription(url);
			// }
			retrieveTaskDescription(url);

			newTask = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), urlTitle);
			NewLocalTaskAction.scheduleNewTask(newTask);

			if (newTask == null) {
				return false;
			}

			newTask.setPriority(Task.PriorityLevel.P3.toString());
			newTask.setTaskUrl(url);

			// NOTE: setting boolean param as false so that we go directly to
			// the
			// browser tab as with a previously-created task
			TasksUiUtil.openEditor(newTask, false);
			return true;
		}
	}

	public boolean createTaskFromString(String title) {
		newTask = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), title);
		NewLocalTaskAction.scheduleNewTask(newTask);

		if (newTask == null) {
			return false;
		} else {
			newTask.setPriority(Task.PriorityLevel.P3.toString());
			TasksUiUtil.openEditor(newTask, false);
			return true;
		}
	}

	@Override
	public boolean validateDrop(Object targetObject, int operation, TransferData transferType) {
		currentTransfer = transferType;

		Object selectedObject = ((IStructuredSelection) ((TreeViewer) getViewer()).getSelection()).getFirstElement();
		if (FileTransfer.getInstance().isSupportedType(currentTransfer)) {
			if (getCurrentTarget() instanceof ITask) {
				return true;
			}
		} else if (selectedObject != null && !(selectedObject instanceof AbstractRepositoryQuery)) {
			if (getCurrentTarget() instanceof TaskCategory) {
				return true;
			} else if (getCurrentTarget() instanceof ITaskListElement
					&& (getCurrentLocation() == ViewerDropAdapter.LOCATION_AFTER || getCurrentLocation() == ViewerDropAdapter.LOCATION_BEFORE)) {
				return true;
			} else {
				return false;
			}
		}

		return TextTransfer.getInstance().isSupportedType(transferType);
	}

	/**
	 * Attempts to set the task pageTitle to the title from the specified url
	 */
	protected void retrieveTaskDescription(final String url) {

		try {
			RetrieveTitleFromUrlJob job = new RetrieveTitleFromUrlJob(url) {
				@Override
				protected void setTitle(final String pageTitle) {
					newTask.setSummary(pageTitle);
					TasksUiPlugin.getTaskListManager().getTaskList().notifyLocalInfoChanged(newTask);
				}
			};
			job.schedule();
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "could not open task web page", false);
		}
	}
}
