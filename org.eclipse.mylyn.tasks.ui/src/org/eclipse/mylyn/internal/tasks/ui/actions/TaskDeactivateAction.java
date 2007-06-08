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

package org.eclipse.mylyn.internal.tasks.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.mylyn.core.MylarStatusHandler;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;

/**
 * @author Mik Kersten
 */
public class TaskDeactivateAction extends Action {

	public static final String ID = "org.eclipse.mylyn.tasklist.actions.context.deactivate";

	public TaskDeactivateAction() {
		setId(ID);
		setText("Deactivate");
		setImageDescriptor(TasksUiImages.TASK_INACTIVE);
	}

	public void run(ITask task) {
		try {
			if (task != null) {
				TasksUiPlugin.getTaskListManager().deactivateTask(task);
//				TaskListView.getFromActivePerspective().refreshAndFocus();
				TasksUiUtil.closeEditorInActivePage(task);
			}
		} catch (Exception e) {
			MylarStatusHandler.log(e, " Closing task editor on task deactivation failed");
		}
	}

	@Override
	public void run() {
		run(TaskListView.getFromActivePerspective().getSelectedTask());
	}
}
