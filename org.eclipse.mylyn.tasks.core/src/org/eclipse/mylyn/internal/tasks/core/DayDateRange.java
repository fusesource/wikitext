/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core;

import java.util.Calendar;

import org.eclipse.mylyn.internal.provisional.commons.core.CommonMessages;

/**
 * @author Rob Elves
 */
public class DayDateRange extends DateRange {

	public DayDateRange(Calendar startDate, Calendar endDate) {
		super(startDate, endDate);
	}

	public DayDateRange next() {
		return create(Calendar.DAY_OF_YEAR, 1);
	}

	public DayDateRange previous() {
		return create(Calendar.DAY_OF_YEAR, -1);
	}

	protected DayDateRange create(int field, int multiplier) {
		Calendar previousStart = (Calendar) getStartDate().clone();
		Calendar previousEnd = (Calendar) getEndDate().clone();
		previousStart.add(field, 1 * multiplier);
		previousEnd.add(field, 1 * multiplier);
		return new DayDateRange(previousStart, previousEnd);
	}

	@Override
	public String toString(boolean useDayOfWeekForNextWeek) {
		boolean isThisWeek = TaskActivityUtil.getCurrentWeek().includes(this);
		Calendar endNextWeek = TaskActivityUtil.getCalendar();
		endNextWeek.add(Calendar.DAY_OF_YEAR, 7);
		boolean isNextWeek = TaskActivityUtil.getNextWeek().includes(this) && this.before(endNextWeek);
		if (isThisWeek || (useDayOfWeekForNextWeek && isNextWeek)) {
			String day = ""; //$NON-NLS-1$
			switch (getStartDate().get(Calendar.DAY_OF_WEEK)) {
			case Calendar.MONDAY:
				day = CommonMessages.Monday;
				break;
			case Calendar.TUESDAY:
				day = CommonMessages.Tuesday;
				break;
			case Calendar.WEDNESDAY:
				day = CommonMessages.Wednesday;
				break;
			case Calendar.THURSDAY:
				day = CommonMessages.Thursday;
				break;
			case Calendar.FRIDAY:
				day = CommonMessages.Friday;
				break;
			case Calendar.SATURDAY:
				day = CommonMessages.Saturday;
				break;
			case Calendar.SUNDAY:
				day = CommonMessages.Sunday;
				break;
			}
			if (isPresent()) {
				return day + Messages.DayDateRange___Today;
			} else {
				return day;
			}
		}
		return super.toString(useDayOfWeekForNextWeek);
	}

	public static boolean isDayRange(Calendar calStart, Calendar calEnd) {
		// bug 248683
		long diff = (calEnd.getTimeInMillis() - calStart.getTimeInMillis()) - (DAY - 1);
		return Math.abs(diff) <= 60 * 60 * 1000;
	}
}
