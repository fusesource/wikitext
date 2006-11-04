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

package org.eclipse.mylar.internal.tasks.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylar.context.core.MylarStatusHandler;
import org.eclipse.mylar.internal.tasks.ui.editors.CategoryEditorInput;
import org.eclipse.mylar.internal.tasks.ui.editors.MylarTaskEditor;
import org.eclipse.mylar.internal.tasks.ui.editors.TaskEditorInput;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.AbstractTaskContainer;
import org.eclipse.mylar.tasks.core.DateRangeActivityDelegate;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskListElement;
import org.eclipse.mylar.tasks.core.Task;
import org.eclipse.mylar.tasks.core.TaskCategory;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.WebBrowserPreference;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;

/**
 * @author Mik Kersten
 */
public class TaskUiUtil {

	/**
	 * TODO: move
	 */
	public static Image getImageForPriority(Task.PriorityLevel priorityLevel) {
		if (priorityLevel == null) {
			return null;
		}
		switch (priorityLevel) {
		case P1:
			return TaskListImages.getImage(TaskListImages.PRIORITY_1);
		case P2:
			return TaskListImages.getImage(TaskListImages.PRIORITY_2);
		case P3:
			return TaskListImages.getImage(TaskListImages.PRIORITY_3);
		case P4:
			return TaskListImages.getImage(TaskListImages.PRIORITY_4);
		case P5:
			return TaskListImages.getImage(TaskListImages.PRIORITY_5);
		default:
			return null;
		}
	}

	public static void closeEditorInActivePage(ITask task) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return;
		}
		IEditorInput input = new TaskEditorInput(task, false);
		IEditorPart editor = page.findEditor(input);
		if (editor != null) {
			page.closeEditor(editor, false);
		}
	}

	public static boolean openRepositoryTask(TaskRepository repository, String taskId) {
		boolean opened = false;
		ITask task = TasksUiPlugin.getTaskListManager().getTaskList().getTask(
				AbstractRepositoryTask.getHandle(repository.getUrl(), taskId));
		if (task != null) {
			TaskUiUtil.refreshAndOpenTaskListElement(task);
			opened = true;
		} else {
			AbstractRepositoryConnectorUi connectorUi = TasksUiPlugin.getRepositoryUi(repository.getKind());
			if (connectorUi != null) {
				connectorUi.openRemoteTask(repository.getUrl(), taskId);
				opened = true;
			}
		}
		return opened;
	}

	/**
	 * Either pass in a repository and id, or fullUrl, or all of them
	 */
	public static boolean openRepositoryTask(String repositoryUrl, String taskId, String fullUrl) {
		boolean opened = false;
		ITask task = null;
		if (taskId != null) {
			String handle = AbstractRepositoryTask.getHandle(repositoryUrl, taskId);
			task = TasksUiPlugin.getTaskListManager().getTaskList().getTask(handle);
		}
		if (task == null) {
			// search for it
			for (ITask currTask : TasksUiPlugin.getTaskListManager().getTaskList().getAllTasks()) {
				if (currTask instanceof AbstractRepositoryTask) {
					String currUrl = ((AbstractRepositoryTask) currTask).getUrl();
					if (currUrl != null && !currUrl.equals("") && currUrl.equals(fullUrl)) {
						task = currTask;
						break;
					}
				}
			}
		}
		if (task != null) {
			TaskUiUtil.refreshAndOpenTaskListElement(task);
			opened = true;
		} else {
			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getConnectorForRepositoryTaskUrl(
					fullUrl);
			AbstractRepositoryConnectorUi connectorUi = TasksUiPlugin.getRepositoryUi(connector.getRepositoryType());

			if (connector != null) {
				opened = connectorUi.openRemoteTask(repositoryUrl, taskId);
			} 
		}
		if (!opened) {
			TaskUiUtil.openUrl(fullUrl);
			opened = true;
		}
		return opened;
	}

	public static void refreshAndOpenTaskListElement(ITaskListElement element) {
		if (element instanceof ITask || element instanceof AbstractQueryHit
				|| element instanceof DateRangeActivityDelegate) {
			final ITask task;
			if (element instanceof AbstractQueryHit) {
				task = ((AbstractQueryHit) element).getOrCreateCorrespondingTask();
			} else if (element instanceof DateRangeActivityDelegate) {
				task = ((DateRangeActivityDelegate) element).getCorrespondingTask();
			} else {
				task = (ITask) element;
			}

			if (task instanceof AbstractRepositoryTask) {
				final AbstractRepositoryTask repositoryTask = (AbstractRepositoryTask) task;
				String repositoryKind = repositoryTask.getRepositoryKind();
				final AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager()
						.getRepositoryConnector(repositoryKind);

				TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(repositoryKind,
						repositoryTask.getRepositoryUrl());
				if (repository == null) { // ||
											// !connector.validate(repository))
											// {
					return;
				}

				if (connector != null)
					if (repositoryTask.getTaskData() != null) {
						TaskUiUtil.openEditor(task, false, false);
						TasksUiPlugin.getSynchronizationManager().setTaskRead(repositoryTask, true);
						TasksUiPlugin.getSynchronizationManager().synchronize(connector, repositoryTask, false, null);
					} else {
						Job refreshJob = TasksUiPlugin.getSynchronizationManager().synchronize(connector,
								repositoryTask, true, new JobChangeAdapter() {
									public void done(IJobChangeEvent event) {
										TaskUiUtil.openEditor(task, false);
									}
								});
						if (refreshJob == null) {
							TaskUiUtil.openEditor(task, false);
						}
					}
			} else {
				TaskUiUtil.openEditor(task, false);
			}
		} else if (element instanceof TaskCategory) {
			TaskUiUtil.openEditor((AbstractTaskContainer) element);
		} else if (element instanceof AbstractRepositoryQuery) {
			AbstractRepositoryQuery query = (AbstractRepositoryQuery) element;
			AbstractRepositoryConnectorUi connectorUi = TasksUiPlugin.getRepositoryUi(query.getRepositoryKind());
			connectorUi.openEditQueryDialog(query);
		}
	}

	public static void openEditor(final ITask task, boolean newTask) {
		openEditor(task, true, newTask);
	}

	/**
	 * Set asyncExec false for testing purposes.
	 */
	public static void openEditor(final ITask task, boolean asyncExec, boolean newTask) {

		final IEditorInput editorInput = new TaskEditorInput(task, newTask);

		if (asyncExec) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					openEditor(editorInput, TaskListPreferenceConstants.TASK_EDITOR_ID, page);
				}
			});
		} else {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			openEditor(editorInput, TaskListPreferenceConstants.TASK_EDITOR_ID, page);
		}
	}

	public static IEditorPart openEditor(IEditorInput input, String editorId, IWorkbenchPage page) {
		try {
			return page.openEditor(input, editorId);
		} catch (PartInitException e) {
			MylarStatusHandler.fail(e, "Open for editor failed: " + input + ", id: " + editorId, true);
		}
		return null;
	}

	public static void openEditor(AbstractTaskContainer category) {
		final IEditorInput input = new CategoryEditorInput(category);
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				openEditor(input, TaskListPreferenceConstants.CATEGORY_EDITOR_ID, page);
			}
		});
	}

	public static void openUrl(String url) {
		try {
			if (WebBrowserPreference.getBrowserChoice() == WebBrowserPreference.EXTERNAL) {
				try {
					IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
					support.getExternalBrowser().openURL(new URL(url));
				} catch (Exception e) {
					MylarStatusHandler.fail(e, "could not open task url", true);
				}
			} else {
				IWebBrowser browser = null;
				int flags = 0;
				if (WorkbenchBrowserSupport.getInstance().isInternalWebBrowserAvailable()) {
					flags = WorkbenchBrowserSupport.AS_EDITOR | WorkbenchBrowserSupport.LOCATION_BAR
							| WorkbenchBrowserSupport.NAVIGATION_BAR;

				} else {
					flags = WorkbenchBrowserSupport.AS_EXTERNAL | WorkbenchBrowserSupport.LOCATION_BAR
							| WorkbenchBrowserSupport.NAVIGATION_BAR;
				}
				String title = "Browser";
				browser = WorkbenchBrowserSupport.getInstance().createBrowser(flags, TasksUiPlugin.PLUGIN_ID + title,
						null, null);
				browser.openURL(new URL(url));
			}
		} catch (PartInitException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Browser init error",
					"Browser could not be initiated");
		} catch (MalformedURLException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "URL not found", "URL Could not be opened");
		}
	}

	public static List<MylarTaskEditor> getActiveRepositoryTaskEditors() {
		List<MylarTaskEditor> repositoryTaskEditors = new ArrayList<MylarTaskEditor>();
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow window : windows) {
			IEditorReference[] editorReferences = window.getActivePage().getEditorReferences();
			for (int i = 0; i < editorReferences.length; i++) {
				IEditorPart editor = editorReferences[i].getEditor(false);
				if (editor instanceof MylarTaskEditor) {
					MylarTaskEditor taskEditor = (MylarTaskEditor) editor;
					if (taskEditor.getEditorInput() instanceof TaskEditorInput) {
						TaskEditorInput input = (TaskEditorInput) taskEditor.getEditorInput();
						if (input.getTask() instanceof AbstractRepositoryTask) {
							repositoryTaskEditors.add((MylarTaskEditor) editor);
						}
					}
				}
			}
		}
		return repositoryTaskEditors;
	}
}