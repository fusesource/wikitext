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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.mylar.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * @author Mik Kersten
 */
public class TaskActivateAction extends Action implements IViewActionDelegate {

	public static final String ID = "org.eclipse.mylar.tasklist.actions.context.activate";

	public TaskActivateAction() {
		setId(ID);
		setText("Activate");
		setImageDescriptor(TasksUiImages.TASK_ACTIVE_CENTERED);
	}

	public void init(IViewPart view) {
		// ignore
	}

	@Override
	public void run() {
		run(TaskListView.getFromActivePerspective().getSelectedTask());
	}

	public void run(ITask task) {
		if (task != null && !task.isActive()) {
			TasksUiPlugin.getTaskListManager().activateTask(task);
			if (TaskListView.getFromActivePerspective() != null) {
				TaskListView.getFromActivePerspective().refreshAndFocus(false);
			}
		}
	}

	public void run(IAction action) {
		run();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
	}
}
