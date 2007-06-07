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

package org.eclipse.mylar.tasks.core;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Rob Elves
 * @author Mik Kersten
 */
public class DateRangeContainer extends AbstractTaskContainer {

	private Set<DateRangeActivityDelegate> dateRangeDelegates = new HashSet<DateRangeActivityDelegate>();

	private Map<DateRangeActivityDelegate, Long> taskToDuration = new HashMap<DateRangeActivityDelegate, Long>();

	private Calendar startDate;

	private Calendar endDate;

	private long totalElapsed = 0;

	private long totalEstimated = 0;

	public DateRangeContainer(GregorianCalendar startDate, GregorianCalendar endDate, String description) {
		super(description);
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public DateRangeContainer(Calendar startDate, Calendar endDate, String description) {
		super(description);
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public DateRangeContainer(GregorianCalendar startDate, GregorianCalendar endDate) {
		super(DateFormat.getDateInstance(DateFormat.FULL).format(startDate.getTime()) + " to "
				+ DateFormat.getDateInstance(DateFormat.FULL).format(endDate.getTime()));
		// super(startDate.hashCode() + endDate.hashCode() + "");
		// String start =
		// DateFormat.getDateInstance(DateFormat.FULL).format(startDate.getTime());
		// String end =
		// DateFormat.getDateInstance(DateFormat.FULL).format(endDate.getTime());
		// this.description = start + " to " + end;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public DateRangeContainer(Date time, Date time2, String description) {
		super(description);
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

	public void clear() {
		totalEstimated = 0;
		totalElapsed = 0;
		taskToDuration.clear();
		dateRangeDelegates.clear();
		super.clear();
	}

	public void addTask(DateRangeActivityDelegate taskWrapper) {
		long taskActivity = taskWrapper.getActivity();
		if (taskActivity < 0)
			taskActivity = 0;
		totalElapsed += taskActivity;
		dateRangeDelegates.remove(taskWrapper);
		dateRangeDelegates.add(taskWrapper);
		if (taskToDuration.containsKey(taskWrapper)) {
			long previous = taskToDuration.get(taskWrapper);
			long newDuration = previous + taskActivity;
			taskToDuration.put(taskWrapper, newDuration);
		} else {
			taskToDuration.put(taskWrapper, taskActivity);
		}
		super.add(taskWrapper.getCorrespondingTask());
	}

	public void remove(DateRangeActivityDelegate taskWrapper) {
		dateRangeDelegates.remove(taskWrapper);
		super.remove(taskWrapper.getCorrespondingTask());
	}

	public Calendar getStart() {
		return startDate;
	}

	public Calendar getEnd() {
		return endDate;
	}

	public long getTotalElapsed() {
		return totalElapsed;
	}

	public long getElapsed(DateRangeActivityDelegate taskWrapper) {
		if (taskToDuration.containsKey(taskWrapper)) {
			return taskToDuration.get(taskWrapper);
		} else {
			return 0;
		}
	}

	public long getTotalEstimated() {
		totalEstimated = 0;
		for (ITask task : dateRangeDelegates) {
			totalEstimated += task.getEstimateTimeHours();
		}
		return totalEstimated;
	}

	public boolean isArchive() {
		return false;
	}

	public void setIsArchive(boolean isArchive) {
		// ignore
	}

	public String getPriority() {
		return "";
	}

	@Override
	public void setHandleIdentifier(String id) {
		// ignore
	}

	public Set<DateRangeActivityDelegate> getDateRangeDelegates() {
		return dateRangeDelegates;
	}

	public boolean isFuture() {
		return !isPresent() && getStart().after(Calendar.getInstance());
	}

	public boolean isPresent() {
		return getStart().before(Calendar.getInstance()) && getEnd().after(Calendar.getInstance());
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((startDate == null) ? 0 : startDate.hashCode());
		result = PRIME * result + ((endDate == null) ? 0 : endDate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final DateRangeContainer other = (DateRangeContainer) obj;
		if (startDate == null) {
			if (other.startDate != null)
				return false;
		} else if (!startDate.equals(other.startDate))
			return false;
		if (endDate == null) {
			if (other.endDate != null)
				return false;
		} else if (!endDate.equals(other.endDate))
			return false;
		return true;
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	/**
	 * The handle for most containers is their summary. Override to specify a
	 * different natural ordering.
	 */
	@Override
	public int compareTo(ITaskListElement taskListElement) {
		return startDate.compareTo(((DateRangeContainer) taskListElement).startDate);
	}
}