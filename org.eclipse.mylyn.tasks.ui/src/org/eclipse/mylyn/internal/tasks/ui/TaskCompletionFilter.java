/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.internal.tasks.ui;

import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;

/**
 * @author Mik Kersten
 */
public class TaskCompletionFilter extends AbstractTaskListFilter {

	private TaskListView taskListView;

	public TaskCompletionFilter(TaskListView taskListView) {
		this.taskListView = taskListView;
	}

	@Override
	public boolean select(Object parent, Object element) {
		if (element instanceof AbstractTask) {
			AbstractTask task = (AbstractTask) element;

			if (task.isCompleted()
					&& taskListView.isFocusedMode()
					&& TasksUiPlugin.getDefault().getPreferenceStore().getBoolean(
							TasksUiPreferenceConstants.FILTER_COMPLETE_MODE)) {
				// but has child with incoming don't filter
				return hasDescendantIncoming(task);
			} else if (!taskListView.isFocusedMode() && task.isCompleted()) {
				return hasIncompleteDescendant(task);
			}
			return !task.isCompleted();
		}
		return true;
	}
}
