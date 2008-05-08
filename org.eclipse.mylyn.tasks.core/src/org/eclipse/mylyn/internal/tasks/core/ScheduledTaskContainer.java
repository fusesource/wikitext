/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskElement;

/**
 * @author Rob Elves
 * @author Mik Kersten
 */
public class ScheduledTaskContainer extends AbstractTaskContainer {

	private final Set<ScheduledTaskDelegate> dateRangeDelegates = new HashSet<ScheduledTaskDelegate>();

	private final Calendar startDate;

	private final Calendar endDate;

	private boolean captureFloating = false;

	private final TaskActivityManager activityManager;

	public ScheduledTaskContainer(TaskActivityManager activityManager, GregorianCalendar startDate,
			GregorianCalendar endDate, String description) {
		super(description);
		this.activityManager = activityManager;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public ScheduledTaskContainer(TaskActivityManager activityManager, Calendar startDate, Calendar endDate,
			String description) {
		super(description);
		this.activityManager = activityManager;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public ScheduledTaskContainer(TaskActivityManager activityManager, Date time, Date time2, String description) {
		super(description);
		this.activityManager = activityManager;
		startDate = new GregorianCalendar();
		startDate.setTime(time);
		endDate = new GregorianCalendar();
		endDate.setTime(time2);
		// this.description = summary;
	}

	public boolean includes(Calendar cal) {
		return (startDate.getTimeInMillis() <= cal.getTimeInMillis())
				&& (endDate.getTimeInMillis() >= cal.getTimeInMillis());
	}

	public Calendar getStart() {
		return startDate;
	}

	public Calendar getEnd() {
		return endDate;
	}

	public long getTotalElapsed() {
		long elapsed = 0;
		for (ITask task : getChildren()) {
			elapsed += activityManager.getElapsedTime(task, getStart(), getEnd());
		}
		return elapsed;
	}

	public long getElapsed(ITask task) {
		return activityManager.getElapsedTime(task, getStart(), getEnd());
	}

	public long getTotalEstimated() {
		long totalEstimated = 0;
		for (ITask task : dateRangeDelegates) {
			totalEstimated += task.getEstimatedTimeHours();
		}
		return totalEstimated;
	}

	public boolean isArchive() {
		return false;
	}

	public void setIsArchive(boolean isArchive) {
		// ignore
	}

	@Override
	public String getPriority() {
		return "";
	}

	@Override
	public void setHandleIdentifier(String id) {
		// ignore
	}

	public boolean isFuture() {
		return !isPresent() && getStart().after(TaskActivityUtil.getCalendar());
	}

	public boolean isPresent() {
		return getStart().before(TaskActivityUtil.getCalendar()) && getEnd().after(TaskActivityUtil.getCalendar());
	}

	public boolean isToday() {
		return !isCaptureFloating()
				&& (TaskActivityUtil.getCalendar().get(Calendar.DAY_OF_MONTH) == getStart().get(Calendar.DAY_OF_MONTH));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
		result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ScheduledTaskContainer other = (ScheduledTaskContainer) obj;
		if (endDate == null) {
			if (other.endDate != null) {
				return false;
			}
		} else if (!endDate.equals(other.endDate)) {
			return false;
		}
		if (startDate == null) {
			if (other.startDate != null) {
				return false;
			}
		} else if (!startDate.equals(other.startDate)) {
			return false;
		}
		return true;
	}

	/**
	 * The handle for most containers is their summary. Override to specify a different natural ordering.
	 */
	@Override
	public int compareTo(ITaskElement taskListElement) {
		return startDate.compareTo(((ScheduledTaskContainer) taskListElement).startDate);
	}

	@Override
	public boolean isUserManaged() {
		return false;
	}

	@Override
	public Collection<ITask> getChildren() {
		Set<ITask> children = new HashSet<ITask>();
		Calendar beginning = TaskActivityUtil.getCalendar();
		beginning.setTimeInMillis(0);
		if (isCaptureFloating() && !isFuture()) {
			for (ITask task : activityManager.getScheduledTasks(beginning, getEnd())) {
				if (((AbstractTask) task).internalIsFloatingScheduledDate()) {
					children.add(task);
				}
			}
		} else if (isPresent()) {
			// add all due/overdue
			Calendar end = TaskActivityUtil.getCalendar();
			end.set(5000, 12, 1);
			for (ITask task : activityManager.getDueTasks(beginning, getEnd())) {
				if (activityManager.isOwnedByUser(task)) {
					children.add(task);
				}
			}

			// add all scheduled/overscheduled
			for (ITask task : activityManager.getScheduledTasks(beginning, getEnd())) {
				if (!((AbstractTask) task).internalIsFloatingScheduledDate() && !task.isCompleted()) {
					children.add(task);
				}
			}

			// if not scheduled or due in future, and is active, place in today bin
			ITask activeTask = activityManager.getActiveTask();
			if (activeTask != null && !children.contains(activeTask)) {
				Set<ITask> futureScheduled = activityManager.getScheduledTasks(getStart(), end);
				for (ITask task : activityManager.getDueTasks(getStart(), end)) {
					if (activityManager.isOwnedByUser(task)) {
						futureScheduled.add(task);
					}
				}
				if (!futureScheduled.contains(activeTask)) {
					children.add(activeTask);
				}
			}
		} else if (isFuture()) {
			children.addAll(activityManager.getScheduledTasks(getStart(), getEnd()));
			for (ITask task : activityManager.getDueTasks(getStart(), getEnd())) {
				if (activityManager.isOwnedByUser(task)) {
					children.add(task);
				}
			}
		} else {
			children.addAll(activityManager.getActiveTasks(getStart(), getEnd()));
		}
		return children;
	}

	public boolean isCaptureFloating() {
		return captureFloating;
	}

	public void setCaptureFloating(boolean captureFloating) {
		this.captureFloating = captureFloating;
	}

	@Override
	public String getSummary() {
		if (isToday()) {
			return "Today";
		}
		return super.getSummary();
	}
}
