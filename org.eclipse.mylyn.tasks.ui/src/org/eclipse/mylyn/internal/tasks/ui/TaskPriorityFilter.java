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

import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskListElement;
import org.eclipse.mylar.tasks.core.Task;

/**
 * @author Mik Kersten
 * @author Ken Sueda
 */
public class TaskPriorityFilter extends AbstractTaskListFilter {

	private static final String PRIORITY_PREFIX = "P";

	private String priorityLevel = Task.PriorityLevel.P5.toString();

	public TaskPriorityFilter() {
		displayPrioritiesAbove(TaskListView.getCurrentPriorityLevel());
	}
 
	public void displayPrioritiesAbove(String level) {
		priorityLevel = level;
	}

	public boolean select(Object element) {
		if (element instanceof ITaskListElement) {
			if (element instanceof AbstractQueryHit && ((AbstractQueryHit) element).getCorrespondingTask() != null) {
				element = ((AbstractQueryHit) element).getCorrespondingTask();
			}

			if (element instanceof ITask) {
				ITask task = (ITask) element;
				if (shouldAlwaysShow(task)) {
					return true;
				}
			}
			String priority = ((ITaskListElement) element).getPriority();
			if (priority == null || !(priority.startsWith(PRIORITY_PREFIX))) {
				return true;
			}
			if (priorityLevel.compareTo(((ITaskListElement) element).getPriority()) >= 0) {
				return true;
			}
			return false;
		}
		return true;
	}

}
