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

package org.eclipse.mylar.internal.tasklist.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.internal.core.util.MylarStatusHandler;
import org.eclipse.mylar.internal.tasklist.ui.TaskListImages;
import org.eclipse.mylar.internal.tasklist.ui.TaskListUiUtil;
import org.eclipse.mylar.internal.tasklist.ui.views.TaskListView;
import org.eclipse.mylar.tasklist.ITask;
import org.eclipse.mylar.tasklist.MylarTaskListPlugin;

/**
 * @author Mik Kersten
 */
public class TaskDeactivateAction extends Action {

	public static final String ID = "org.eclipse.mylar.tasklist.actions.context.deactivate";

	public TaskDeactivateAction() {
		setId(ID);
		setText("Deactivate");
		setImageDescriptor(TaskListImages.TASK_INACTIVE);
	}

	public void run(ITask task) {
		MylarPlugin.getContextManager().actionObserved(this, Boolean.FALSE.toString());

		try {
			if (task != null) {
				MylarTaskListPlugin.getTaskListManager().deactivateTask(task);
				TaskListView.getDefault().getViewer().refresh();
				TaskListUiUtil.closeEditorInActivePage(task);
//				TaskListView.getDefault().closeTaskEditors(task, page);
			}
		} catch (Exception e) {
			MylarStatusHandler.log(e, " Closing task editor on task deactivation failed");
		}
	}

	public void run() {
		run(TaskListView.getDefault().getSelectedTask());
	}
}
