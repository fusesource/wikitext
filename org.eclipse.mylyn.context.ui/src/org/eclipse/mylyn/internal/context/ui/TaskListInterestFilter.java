/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.context.ui;

import java.util.Calendar;

import org.eclipse.mylyn.internal.tasks.core.ScheduledTaskContainer;
import org.eclipse.mylyn.internal.tasks.core.LocalRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.core.TaskActivityManager;
import org.eclipse.mylyn.internal.tasks.ui.AbstractTaskListFilter;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPreferenceConstants;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.tasks.core.AbstractTask.RepositoryTaskSyncState;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;

/**
 * Goal is to have this reuse as much of the super as possible.
 * 
 * @author Mik Kersten
 */
public class TaskListInterestFilter extends AbstractTaskListFilter {

	@Override
	public boolean select(Object parent, Object object) {
		try {
			if (object instanceof ScheduledTaskContainer) {
				ScheduledTaskContainer dateRangeTaskContainer = (ScheduledTaskContainer) object;
				return isDateRangeInteresting(dateRangeTaskContainer);
			}
			if (object instanceof AbstractTask) {
				AbstractTask task = null;
				if (object instanceof AbstractTask) {
					task = (AbstractTask) object;
				}
				if (task != null) {
					if (isUninteresting(parent, task)) {
						return false;
					} else if (isInteresting(parent, task)) {
						return true;
					}
				}
			}
		} catch (Throwable t) {
			StatusHandler.fail(t, "interest filter failed", false);
		}
		return false;
	}

	private boolean isDateRangeInteresting(ScheduledTaskContainer container) {
		return TasksUiPlugin.getTaskListManager().isWeekDay(container);
//		return (TasksUiPlugin.getTaskListManager().isWeekDay(container) && (container.isPresent() || container.isFuture()));
	}

	protected boolean isUninteresting(Object parent, AbstractTask task) {
		return !task.isActive()
				&& !hasInterestingSubTasks(parent, task, true)
				&& ((task.isCompleted() && !TaskActivityManager.getInstance().isCompletedToday(task) && !hasChanges(
						parent, task)) || (TaskActivityManager.getInstance().isScheduledAfterThisWeek(task))
						&& !hasChanges(parent, task));
	}

	// TODO: make meta-context more explicit
	protected boolean isInteresting(Object parent, AbstractTask task) {
		return shouldAlwaysShow(parent, task, !TasksUiPlugin.getDefault().getPreferenceStore().getBoolean(
				TasksUiPreferenceConstants.FILTER_SUBTASKS));
	}

	@Override
	public boolean shouldAlwaysShow(Object parent, AbstractTask task, boolean checkSubTasks) {
		return super.shouldAlwaysShow(parent, task, checkSubTasks) || hasChanges(parent, task)
				|| (TaskActivityManager.getInstance().isCompletedToday(task))
				|| shouldShowInFocusedWorkweekDateContainer(parent, task)
				|| (isInterestingForThisWeek(parent, task) && !task.isCompleted())
				|| (TaskActivityManager.getInstance().isOverdue(task))
				|| hasInterestingSubTasks(parent, task, checkSubTasks)
				|| LocalRepositoryConnector.DEFAULT_SUMMARY.equals(task.getSummary());
		// || isCurrentlySelectedInEditor(task);
	}

	private boolean hasInterestingSubTasks(Object parent, AbstractTask task, boolean checkSubTasks) {
		if (!checkSubTasks) {
			return false;
		}
		if (TasksUiPlugin.getDefault().getPreferenceStore().getBoolean(TasksUiPreferenceConstants.FILTER_SUBTASKS)) {
			return false;
		}
		if (task.getChildren() != null && task.getChildren().size() > 0) {
			for (AbstractTask subTask : task.getChildren()) {
				if (shouldAlwaysShow(parent, subTask, false)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean shouldShowInFocusedWorkweekDateContainer(Object parent, AbstractTask task) {
		if (parent instanceof ScheduledTaskContainer) {

			if (TaskActivityManager.getInstance().isOverdue(task) || task.isPastReminder())
				return true;

			ScheduledTaskContainer container = (ScheduledTaskContainer) parent;
			Calendar previousCal = TasksUiPlugin.getTaskListManager().getActivityPrevious().getEnd();
			Calendar nextCal = TasksUiPlugin.getTaskListManager().getActivityNextWeek().getStart();
			if (container.getEnd().compareTo(previousCal) > 0 && container.getStart().compareTo(nextCal) < 0) {
				// within workweek
				return true;
			}
		}

		return false;
	}

	public static boolean isInterestingForThisWeek(Object parent, AbstractTask task) {
		if (parent instanceof ScheduledTaskContainer) {
			return shouldShowInFocusedWorkweekDateContainer(parent, task);
		} else {
			return TaskActivityManager.getInstance().isScheduledForThisWeek(task)
					|| TaskActivityManager.getInstance().isScheduledForToday(task) || task.isPastReminder();
		}
	}

	public static boolean hasChanges(Object parent, AbstractTask task) {
		if (parent instanceof ScheduledTaskContainer) {
			if (!shouldShowInFocusedWorkweekDateContainer(parent, task)) {
				return false;
			}
		}

		boolean result = false;
		if (task != null) {
			if (task.getLastReadTimeStamp() == null) {
				return true;
			} else if (task.getSynchronizationState() == RepositoryTaskSyncState.OUTGOING) {
				return true;
			} else if (task.getSynchronizationState() == RepositoryTaskSyncState.INCOMING
					&& !(parent instanceof ScheduledTaskContainer)) {
				return true;
			} else if (task.getSynchronizationState() == RepositoryTaskSyncState.CONFLICT) {
				return true;
			}
			return hasChanges(parent, (AbstractTaskContainer) task);
		}
		return result;
	}

	private static boolean hasChanges(Object parent, AbstractTaskContainer container) {
		boolean result = false;
		for (AbstractTask task : container.getChildren()) {
			if (task != null) {
				if (task.getLastReadTimeStamp() == null) {
					result = true;
				} else if (task.getSynchronizationState() == RepositoryTaskSyncState.OUTGOING) {
					result = true;
				} else if (task.getSynchronizationState() == RepositoryTaskSyncState.INCOMING
						&& !(parent instanceof ScheduledTaskContainer)) {
					result = true;
				} else if (task.getSynchronizationState() == RepositoryTaskSyncState.CONFLICT) {
					result = true;
				} else if (task.getChildren() != null && task.getChildren().size() > 0) {
					result = hasChanges(parent, task);
				}
			}
		}
		return result;
	}
}
