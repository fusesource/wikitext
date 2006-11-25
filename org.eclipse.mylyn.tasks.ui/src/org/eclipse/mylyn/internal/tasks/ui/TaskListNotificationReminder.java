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

package org.eclipse.mylar.internal.tasks.ui;

import java.util.Date;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.mylar.internal.tasks.ui.views.TaskElementLabelProvider;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

/**
 * @author Rob Elves
 */
public class TaskListNotificationReminder implements ITaskListNotification {

	private final ITask task;

	private DecoratingLabelProvider labelProvider = new DecoratingLabelProvider(new TaskElementLabelProvider(),
			PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());

	private Date date;

	public TaskListNotificationReminder(ITask task) {
		this.task = task;
	}

	public String getDescription() {
		return null;
	}

	public String getLabel() {
		if (labelProvider.getText(task).length() > 40) {
			String truncated = labelProvider.getText(task).substring(0, 35);
			return truncated + "...";
		}
		return labelProvider.getText(task);
	}

	public void openTask() {

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				TaskUiUtil.refreshAndOpenTaskListElement(task);
			}
		});

	}

	public Image getNotificationIcon() {
		return labelProvider.getImage(task);
	}

	public Image getOverlayIcon() {
		return TaskListImages.getImage(TaskListImages.CALENDAR);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TaskListNotificationReminder)) {
			return false;
		}
		TaskListNotificationReminder notification = (TaskListNotificationReminder) o;
		return notification.getTask().equals(task);
	}

	private ITask getTask() {
		return task;
	}

	@Override
	public int hashCode() {
		return task.hashCode();
	}

	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {	
		this.date = date;
	}
	
	public int compareTo(ITaskListNotification anotherNotification) throws ClassCastException {
	    if (!(anotherNotification instanceof ITaskListNotification))
	      throw new ClassCastException("A ITaskListNotification object expected.");
	    Date anotherDate = (anotherNotification).getDate();
	    if(date != null && anotherDate != null) {
	    	return date.compareTo(anotherDate);
	    } else if(date == null) {
	    	return -1;
	    } else {
	    	return 1;
	    }    
	  }
}
