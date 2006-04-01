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

package org.eclipse.mylar.internal.tasklist.ui.views;

import java.text.DateFormat;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.mylar.internal.core.util.DateUtil;
import org.eclipse.mylar.internal.core.util.MylarStatusHandler;
import org.eclipse.mylar.internal.tasklist.ui.TaskListColorsAndFonts;
import org.eclipse.mylar.internal.tasklist.ui.TaskListImages;
import org.eclipse.mylar.internal.tasklist.ui.TaskUiUtil;
import org.eclipse.mylar.provisional.tasklist.DateRangeActivityDelegate;
import org.eclipse.mylar.provisional.tasklist.DateRangeContainer;
import org.eclipse.mylar.provisional.tasklist.ITask;
import org.eclipse.mylar.provisional.tasklist.AbstractTaskContainer;
import org.eclipse.mylar.provisional.tasklist.Task.PriorityLevel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/**
 * @author Rob Elves
 */
public class TaskActivityLabelProvider extends DecoratingLabelProvider implements ITableLabelProvider, IColorProvider,
		IFontProvider {

	private static final String UNITS_HOURS = " hours";

	private static final String NO_MINUTES = "0 minutes";

	private Color categoryBackgroundColor;

	public TaskActivityLabelProvider(ILabelProvider provider, ILabelDecorator decorator, Color parentBacground) {
		super(provider, decorator);
		this.categoryBackgroundColor = parentBacground;
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			if (element instanceof DateRangeContainer) {
				return TaskListImages.getImage(TaskListImages.CALENDAR);
			} else if (element instanceof DateRangeActivityDelegate) {
				return super.getImage(((DateRangeActivityDelegate) element).getCorrespondingTask());
			} else {
				return super.getImage(element);
			}
		} else if (columnIndex == 1) {
			if (element instanceof DateRangeActivityDelegate) {
				DateRangeActivityDelegate taskElement = (DateRangeActivityDelegate) element;
				return TaskUiUtil.getImageForPriority(PriorityLevel.fromString(taskElement.getPriority()));
			}
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof DateRangeActivityDelegate) {
			DateRangeActivityDelegate activityDelegate = (DateRangeActivityDelegate) element;
			ITask task = activityDelegate.getCorrespondingTask();
			switch (columnIndex) {			
			case 2:
				return task.getDescription();
			case 3:
				return DateUtil.getFormattedDurationShort(activityDelegate.getDateRangeContainer().getElapsed(activityDelegate));
			case 4:
				return task.getEstimateTimeHours() + UNITS_HOURS;
			case 5:
				if (task.getReminderDate() != null) {
					return DateFormat.getDateInstance(DateFormat.MEDIUM).format(task.getReminderDate());
				} else {
					return "";
				}
			case 6:
				if (activityDelegate.getStart() > 0 && activityDelegate.getDateRangeContainer().getElapsed(activityDelegate) > 0) {
					return DateFormat.getDateInstance(DateFormat.MEDIUM).format(activityDelegate.getStart());
				} else {
					return "";
				}
			}
		} else if (element instanceof DateRangeContainer) {
			DateRangeContainer taskCategory = (DateRangeContainer) element;
			switch (columnIndex) {
			case 2:
				return taskCategory.getDescription();
			case 3:
				String elapsedTimeString = NO_MINUTES;
				try {
					elapsedTimeString = DateUtil.getFormattedDurationShort(taskCategory.getTotalElapsed());
					if (elapsedTimeString.equals(""))
						elapsedTimeString = NO_MINUTES;
				} catch (RuntimeException e) {
					MylarStatusHandler.fail(e, "Could not format elapsed time", true);
				}
				return elapsedTimeString;
			case 4:
				return taskCategory.getTotalEstimated() + UNITS_HOURS;
			}
		}
		return null;
	}

	@Override
	public Color getBackground(Object element) {
		if (element instanceof AbstractTaskContainer) {
			return categoryBackgroundColor;
		} else {
			return super.getBackground(element);
		}
	}

	public Font getFont(Object element) {
		if (element instanceof DateRangeContainer) {
			DateRangeContainer container = (DateRangeContainer) element;
			if (container.isPresent()) {
				return TaskListColorsAndFonts.BOLD;
			}
		} else if (element instanceof DateRangeActivityDelegate) {
			DateRangeActivityDelegate durationDelegate = (DateRangeActivityDelegate) element;
			return super.getFont(durationDelegate.getCorrespondingTask());
		}
		return super.getFont(element);
	}

	public void setCategoryBackgroundColor(Color categoryBackgroundColor) {
		this.categoryBackgroundColor = categoryBackgroundColor;
	}
}
