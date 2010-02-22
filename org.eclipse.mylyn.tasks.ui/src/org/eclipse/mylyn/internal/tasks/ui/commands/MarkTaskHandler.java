/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.commands;

import java.util.Collections;
import java.util.Date;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylyn.internal.provisional.commons.ui.WorkbenchUtil;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.actions.ClearOutgoingAction;
import org.eclipse.mylyn.internal.tasks.ui.editors.Messages;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylyn.monitor.ui.MonitorUi;
import org.eclipse.mylyn.tasks.core.IRepositoryElement;
import org.eclipse.mylyn.tasks.core.ITask;

/**
 * @author Steffen Pingel
 */
public abstract class MarkTaskHandler extends AbstractTaskHandler {

	public static class ClearOutgoingHandler extends AbstractTaskHandler {
		@Override
		protected void execute(ExecutionEvent event, ITask task) throws ExecutionException {
			ClearOutgoingAction action = new ClearOutgoingAction(Collections.singletonList((IRepositoryElement) task));
			if (action.isEnabled()) {
				action.run();
			}
		}
	}

	public static class ClearActiveTimeHandler extends AbstractTaskHandler {
		@Override
		protected void execute(ExecutionEvent event, ITask task) throws ExecutionException {
			if (MessageDialog.openConfirm(WorkbenchUtil.getShell(),
					Messages.TaskEditorPlanningPart_Confirm_Activity_Time_Deletion,
					Messages.TaskEditorPlanningPart_Do_you_wish_to_reset_your_activity_time_on_this_task_)) {
				MonitorUi.getActivityContextManager().removeActivityTime(task.getHandleIdentifier(), 0l,
						System.currentTimeMillis());
			}
		}
	}

	public static class MarkTaskCompleteHandler extends AbstractTaskHandler {

		public static final String ID_COMMAND = "org.eclipse.mylyn.tasks.ui.command.markTaskComplete"; //$NON-NLS-1$

		@Override
		protected void execute(ExecutionEvent event, ITask task) throws ExecutionException {
			if (TasksUiInternal.hasLocalCompletionState(task)) {
				task.setCompletionDate(new Date());
				TasksUiPlugin.getTaskList().notifyElementChanged(task);
			}
		}
	}

	public static class MarkTaskIncompleteHandler extends AbstractTaskHandler {
		@Override
		protected void execute(ExecutionEvent event, ITask task) throws ExecutionException {
			if (TasksUiInternal.hasLocalCompletionState(task)) {
				task.setCompletionDate(null);
				TasksUiPlugin.getTaskList().notifyElementChanged(task);
			}
		}
	}

	public static class MarkTaskReadHandler extends AbstractTaskHandler {
		@Override
		protected void execute(ExecutionEvent event, ITask task) throws ExecutionException {
			TasksUiPlugin.getTaskDataManager().setTaskRead(task, true);
		}
	}

	public static class MarkTaskUnreadHandler extends AbstractTaskHandler {
		@Override
		protected void execute(ExecutionEvent event, ITask task) throws ExecutionException {
			TasksUiPlugin.getTaskDataManager().setTaskRead(task, false);
		}
	}

	public static class MarkTaskReadGoToNextUnreadTaskHandler extends AbstractTaskListViewHandler {
		@Override
		protected void execute(ExecutionEvent event, TaskListView taskListView, IRepositoryElement item)
				throws ExecutionException {
			if (item instanceof ITask) {
				ITask task = (ITask) item;
				TasksUiPlugin.getTaskDataManager().setTaskRead(task, true);
				GoToUnreadTaskHandler.execute(event, org.eclipse.mylyn.internal.tasks.ui.util.TreeWalker.Direction.DOWN);
			}
		}
	}

	public static class MarkTaskReadGoToPreviousUnreadTaskHandler extends AbstractTaskListViewHandler {
		@Override
		protected void execute(ExecutionEvent event, TaskListView taskListView, IRepositoryElement item)
				throws ExecutionException {
			if (item instanceof ITask) {
				ITask task = (ITask) item;
				TasksUiPlugin.getTaskDataManager().setTaskRead(task, true);
				GoToUnreadTaskHandler.execute(event, org.eclipse.mylyn.internal.tasks.ui.util.TreeWalker.Direction.UP);
			}
		}
	}

}
