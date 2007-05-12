/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.ui.views;

import java.util.Arrays;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.internal.tasks.ui.actions.TaskActivateAction;
import org.eclipse.mylar.internal.tasks.ui.actions.TaskDeactivateAction;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.AbstractTaskContainer;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskListElement;
import org.eclipse.mylar.tasks.core.Task;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author Mik Kersten
 */
class TaskListCellModifier implements ICellModifier {

	private final TaskListView taskListView;

	TaskListCellModifier(TaskListView taskListView) {
		this.taskListView = taskListView;
	}

	public boolean canModify(Object element, String property) {
		return taskListView.isInRenameAction;
	}

	public Object getValue(Object element, String property) {
		try {
			int columnIndex = Arrays.asList(this.taskListView.columnNames).indexOf(property);
			if (element instanceof ITaskListElement) {
				final ITaskListElement taskListElement = (ITaskListElement) element;
				switch (columnIndex) {
				case 0:
					return taskListElement.getSummary();
				case 1:
					return "";
				case 2:
					return "";
				}
			} 
		} catch (Exception e) {
			MylarStatusHandler.log(e, e.getMessage());
		}
		return "";
	}

	public void modify(Object element, String property, Object value) {
		int columnIndex = -1;
		try {
			columnIndex = Arrays.asList(this.taskListView.columnNames).indexOf(property);
			if (((TreeItem) element).getData() instanceof AbstractTaskContainer) {
				AbstractTaskContainer container = (AbstractTaskContainer) ((TreeItem) element).getData();
				switch (columnIndex) {
				case 0:
					TasksUiPlugin.getTaskListManager().getTaskList()
							.renameContainer(container, ((String) value).trim());
				case 1:
					break;
				case 2:
					break;
				}
			} else if (((TreeItem) element).getData() instanceof ITaskListElement) {
				final ITaskListElement taskListElement = (ITaskListElement) ((TreeItem) element).getData();
				ITask task = null;
				if (taskListElement instanceof ITask) {
					task = (ITask) taskListElement;
				} else if (taskListElement instanceof AbstractQueryHit) {
					if (((AbstractQueryHit) taskListElement).getCorrespondingTask() != null) {
						task = ((AbstractQueryHit) taskListElement).getCorrespondingTask();
					}
				}
				switch (columnIndex) {
				case 0:
					if (!(task instanceof AbstractRepositoryTask)) {
						TasksUiPlugin.getTaskListManager().getTaskList().renameTask((Task) task,
								((String) value).trim());
					}
					break;
				case 1:
					break;
				case 2:
					toggleTaskActivation(taskListElement);
					break;
				}
			}
		} catch (Exception e) {
			MylarStatusHandler.fail(e, e.getMessage(), true);
		}
		this.taskListView.getViewer().refresh();
	}

	public void toggleTaskActivation(ITaskListElement taskListElement) {
		ITask task = null;
		if (taskListElement instanceof ITask) {
			task = (ITask) taskListElement;
		} else if (taskListElement instanceof AbstractQueryHit) {
			if (((AbstractQueryHit) taskListElement).getCorrespondingTask() != null) {
				task = ((AbstractQueryHit) taskListElement).getCorrespondingTask();
			}
		}
		
		if (taskListElement instanceof AbstractQueryHit) {
			task = ((AbstractQueryHit) taskListElement).getOrCreateCorrespondingTask();
		}
		if (task != null) {
			if (task.isActive()) {
				new TaskDeactivateAction().run(task);
				this.taskListView.previousTaskAction.setButtonStatus();
			} else {
				new TaskActivateAction().run(task);
				this.taskListView.addTaskToHistory(task);
				this.taskListView.previousTaskAction.setButtonStatus();
			}
		}
	}
}