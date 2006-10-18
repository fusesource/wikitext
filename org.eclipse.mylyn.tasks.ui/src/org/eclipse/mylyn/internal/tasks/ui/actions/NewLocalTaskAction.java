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

package org.eclipse.mylar.internal.tasks.ui.actions;

import java.util.Calendar;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylar.internal.tasks.ui.TaskListImages;
import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.internal.tasks.ui.TaskUiUtil;
import org.eclipse.mylar.internal.tasks.ui.views.TaskInputDialog;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.Task;
import org.eclipse.mylar.tasks.core.TaskCategory;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * @author Mik Kersten
 */
public class NewLocalTaskAction extends Action implements IViewActionDelegate {

	public static final String DESCRIPTION_DEFAULT = "New Task";

	public static final String ID = "org.eclipse.mylar.tasks.ui.actions.create.task";

	public NewLocalTaskAction() {
		this(null);
	}

	public NewLocalTaskAction(TaskListView view) {
		setText(TaskInputDialog.LABEL_SHELL);
		setToolTipText(TaskInputDialog.LABEL_SHELL);
		setId(ID);
		setImageDescriptor(TaskListImages.TASK_NEW);
	}

	public void init(IViewPart view) {
	}

	public void run(IAction action) {
		run();
	}
	
	@Override
	public void run() {
		Task newTask = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), DESCRIPTION_DEFAULT, true);

		scheduleNewTask(newTask);

		Object selectedObject = null;
		TaskListView view = TaskListView.getFromActivePerspective();
		if (view != null) {
			selectedObject = ((IStructuredSelection) view.getViewer().getSelection()).getFirstElement();
		}
		if (selectedObject instanceof TaskCategory) {
			TasksUiPlugin.getTaskListManager().getTaskList().addTask(newTask, (TaskCategory) selectedObject);
		} else if (selectedObject instanceof ITask) {
			ITask task = (ITask) selectedObject;
			if (task.getContainer() instanceof TaskCategory) {
				TasksUiPlugin.getTaskListManager().getTaskList().addTask(newTask, (TaskCategory) task.getContainer());
			} else if (view != null && view.getDrilledIntoCategory() instanceof TaskCategory) {
				TasksUiPlugin.getTaskListManager().getTaskList().addTask(newTask,
						(TaskCategory) view.getDrilledIntoCategory());
			} else {
				TasksUiPlugin.getTaskListManager().getTaskList().addTask(newTask,
						TasksUiPlugin.getTaskListManager().getTaskList().getRootCategory());
			}
		} else if (view != null && view.getDrilledIntoCategory() instanceof TaskCategory) {
			TasksUiPlugin.getTaskListManager().getTaskList().addTask(newTask,
					(TaskCategory) view.getDrilledIntoCategory());
		} else {
			if (view != null && view.getDrilledIntoCategory() != null) {
				MessageDialog
						.openInformation(Display.getCurrent().getActiveShell(), TasksUiPlugin.TITLE_DIALOG,
								"The new task has been added to the root of the list, since tasks can not be added to a query.");
			}
			TasksUiPlugin.getTaskListManager().getTaskList().addTask(newTask,
					TasksUiPlugin.getTaskListManager().getTaskList().getRootCategory());
		}

		TaskUiUtil.openEditor(newTask, true);

		if (view != null) {
			view.getViewer().refresh();
			view.setInRenameAction(true);
			view.getViewer().editElement(newTask, 4);
			view.setInRenameAction(false);
		}
	}

	public static void scheduleNewTask(ITask newTask) {		
		Calendar newTaskSchedule = Calendar.getInstance();
		int scheduledEndHour = TasksUiPlugin.getDefault().getPreferenceStore().getInt(
				TaskListPreferenceConstants.PLANNING_ENDHOUR);
		// If past scheduledEndHour set for following day
		if(newTaskSchedule.get(Calendar.HOUR_OF_DAY) >= scheduledEndHour) {	
			TasksUiPlugin.getTaskListManager().setSecheduledIn(newTaskSchedule, 1);
		} else {
			TasksUiPlugin.getTaskListManager().setScheduledToday(newTaskSchedule);
		}		
		TasksUiPlugin.getTaskListManager().setReminder(newTask, newTaskSchedule.getTime());
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}
}
