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

package org.eclipse.mylyn.internal.context.ui;

import java.util.Calendar;

import org.eclipse.mylyn.core.MylarStatusHandler;
import org.eclipse.mylyn.internal.tasks.core.LocalRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.ui.AbstractTaskListFilter;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPreferenceConstants;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.DateRangeContainer;
import org.eclipse.mylyn.tasks.core.AbstractTask;
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
			if (object instanceof DateRangeContainer) {
				DateRangeContainer dateRangeTaskContainer = (DateRangeContainer) object;
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
			MylarStatusHandler.fail(t, "interest filter failed", false);
		}
		return false;
	}

	private boolean isDateRangeInteresting(DateRangeContainer container) {
		return (TasksUiPlugin.getTaskListManager().isWeekDay(container));// ||dateRangeTaskContainer.isFuture();
	}

	protected boolean isUninteresting(Object parent, AbstractTask task) {
		return !task.isActive()
				&& !hasInterestingSubTasks(parent, task, true)
				&& ((task.isCompleted() && !TasksUiPlugin.getTaskListManager().isCompletedToday(task) && !hasChanges(
						parent, task)) || (TasksUiPlugin.getTaskListManager().isScheduledAfterThisWeek(task))
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
				|| (TasksUiPlugin.getTaskListManager().isCompletedToday(task))
				|| shouldShowInFocusedWorkweekDateContainer(parent, task)
				|| (isInterestingForThisWeek(parent, task) && !task.isCompleted())
				|| (TasksUiPlugin.getTaskListManager().isOverdue(task))
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
		if (parent instanceof DateRangeContainer) {

			if (TasksUiPlugin.getTaskListManager().isOverdue(task) || task.isPastReminder())
				return true;

			DateRangeContainer container = (DateRangeContainer) parent;
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
		if (parent instanceof DateRangeContainer) {
			return shouldShowInFocusedWorkweekDateContainer(parent, task);
		} else {
			return TasksUiPlugin.getTaskListManager().isScheduledForThisWeek(task)
					|| TasksUiPlugin.getTaskListManager().isScheduledForToday(task) || task.isPastReminder();
		}
	}

	public static boolean hasChanges(Object parent, AbstractTask task) {
		if (parent instanceof DateRangeContainer) {
			if (!shouldShowInFocusedWorkweekDateContainer(parent, task)) {
				return false;
			}
		}
		if (task instanceof AbstractTask) {
			AbstractTask repositoryTask = (AbstractTask) task;
			if (repositoryTask.getSyncState() == RepositoryTaskSyncState.OUTGOING) {
				return true;
			} else if (repositoryTask.getSyncState() == RepositoryTaskSyncState.INCOMING
					&& !(parent instanceof DateRangeContainer)) {
				return true;
			} else if (repositoryTask.getSyncState() == RepositoryTaskSyncState.CONFLICT) {
				return true;
			}
		}
		return false;
	}
}
