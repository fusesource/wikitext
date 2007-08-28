/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core;

import java.util.Calendar;


/**
 * @author Rob Elves
 */
public class TaskActivityUtil {

	public static Calendar snapStartOfDay(Calendar cal) {
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.getTime();
		return cal;
	}

	public static Calendar snapStartOfHour(Calendar cal) {
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.getTime();
		return cal;
	}

	public static Calendar snapEndOfHour(Calendar cal) {
		cal.set(Calendar.MINUTE, cal.getMaximum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getMaximum(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, cal.getMaximum(Calendar.MILLISECOND));
		cal.getTime();
		return cal;
	}

	public static Calendar snapEndOfDay(Calendar cal) {
		cal.set(Calendar.HOUR_OF_DAY, cal.getMaximum(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, cal.getMaximum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getMaximum(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, cal.getMaximum(Calendar.MILLISECOND));
		cal.getTime();
		return cal;
	}

	public static Calendar snapNextDay(Calendar cal) {
		cal.add(Calendar.DAY_OF_MONTH, 1);
		snapStartOfDay(cal);
		return cal;
	}

	public static Calendar snapStartOfWorkWeek(Calendar cal) {
		cal.setFirstDayOfWeek(TaskActivityManager.getInstance().getStartDay());
		cal.set(Calendar.DAY_OF_WEEK, TaskActivityManager.getInstance().getStartDay());
		snapStartOfDay(cal);
		return cal;
	}

	public static Calendar snapEndOfWeek(Calendar cal) {
		cal.setFirstDayOfWeek(TaskActivityManager.getInstance().getStartDay());
		cal.set(Calendar.DAY_OF_WEEK, cal.getActualMaximum(Calendar.DAY_OF_WEEK));
		snapEndOfDay(cal);
		return cal;
	}

	public static Calendar snapForwardNumDays(Calendar calendar, int days) {
		calendar.add(Calendar.DAY_OF_MONTH, days);
		calendar.set(Calendar.HOUR_OF_DAY, TaskActivityManager.getInstance().getEndHour());
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}

	public static Calendar snapEndOfWorkDay(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, TaskActivityManager.getInstance().getEndHour());
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}

	public static Calendar snapNextWorkWeek(Calendar calendar) {
		calendar.add(Calendar.WEEK_OF_MONTH, 1);
		snapStartOfWorkWeek(calendar);
		snapEndOfWorkDay(calendar);
		return calendar;
	}

	public static boolean isAfterCurrentWeek(Calendar time) {
		if (time != null) {
			Calendar cal = getCalendar();
			return time.compareTo(snapNextWorkWeek(cal)) > -1;
		}
		return false;
	}

	public static boolean isFuture(Calendar time) {
		if (time != null) {
			Calendar cal = getCalendar();
			cal.add(Calendar.WEEK_OF_MONTH, 2);
			return time.compareTo(cal) > -1;
		}
		return false;
	}

	public static boolean isThisWeek(Calendar time) {
		if (time != null) {
			Calendar weekStart = getCalendar();
			snapStartOfWorkWeek(weekStart);
			Calendar weekEnd = getCalendar();
			snapEndOfWeek(weekEnd);
			return (time.compareTo(weekStart) >= 0 && time.compareTo(weekEnd) <= 0);
		}
		return false;
	}

	public static boolean isToday(Calendar time) {
		if (time != null) {
			Calendar dayStart = getCalendar();
			snapStartOfDay(dayStart);
			Calendar midnight = getCalendar();
			snapEndOfDay(midnight);
			return (time.compareTo(dayStart) >= 0 && time.compareTo(midnight) == -1);
		}
		return false;
	}

	public static Calendar getCalendar() {
		Calendar cal = Calendar.getInstance();
		cal.setFirstDayOfWeek(TaskActivityManager.getInstance().getStartDay());
		cal.getTime();
		return cal;
	}

	public static Calendar getStartOfCurrentWeek() {
		Calendar cal = getCalendar();
		return snapStartOfWorkWeek(cal);
	}

	public static Calendar getEndOfCurrentWeek() {
		Calendar cal = getCalendar();
		return snapEndOfWeek(cal);
	}

}
