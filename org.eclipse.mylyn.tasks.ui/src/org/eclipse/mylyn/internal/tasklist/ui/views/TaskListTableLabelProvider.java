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
/*
 * Created on Feb 18, 2005
 */
package org.eclipse.mylar.internal.tasklist.ui.views;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.mylar.internal.tasklist.ui.TaskListColorsAndFonts;
import org.eclipse.mylar.internal.tasklist.ui.TaskListImages;
import org.eclipse.mylar.provisional.core.MylarPlugin;
import org.eclipse.mylar.provisional.tasklist.AbstractQueryHit;
import org.eclipse.mylar.provisional.tasklist.AbstractRepositoryQuery;
import org.eclipse.mylar.provisional.tasklist.AbstractRepositoryTask;
import org.eclipse.mylar.provisional.tasklist.ITask;
import org.eclipse.mylar.provisional.tasklist.ITaskContainer;
import org.eclipse.mylar.provisional.tasklist.ITaskListElement;
import org.eclipse.mylar.provisional.tasklist.Task;
import org.eclipse.mylar.provisional.tasklist.AbstractRepositoryTask.RepositoryTaskSyncState;
import org.eclipse.mylar.provisional.tasklist.Task.PriorityLevel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/**
 * @author Mik Kersten
 */
public class TaskListTableLabelProvider extends DecoratingLabelProvider implements ITableLabelProvider,
		ITableColorProvider, ITableFontProvider {
	
	private Color parentBackgroundColor;
		
	public TaskListTableLabelProvider(ILabelProvider provider, ILabelDecorator decorator, Color parentBacground) {
		super(provider, decorator);
		this.parentBackgroundColor = parentBacground;
	}
	
	public Image getImageForPriority(Task.PriorityLevel priorityLevel) {
		switch (priorityLevel) {
		case P1: 
			return TaskListImages.getImage(TaskListImages.PRIORITY_1);
		case P2:
			return TaskListImages.getImage(TaskListImages.PRIORITY_2);
		case P3:
			return TaskListImages.getImage(TaskListImages.PRIORITY_3);
		case P4:
			return TaskListImages.getImage(TaskListImages.PRIORITY_4);
		case P5:
			return TaskListImages.getImage(TaskListImages.PRIORITY_5);
		default:
			return null;
		}
	}
	
	public String getColumnText(Object obj, int columnIndex) {
		if (obj instanceof ITaskListElement) {
//			ITaskListElement element = (ITaskListElement) obj;
			switch (columnIndex) {
			case 0:
				return null;
			case 1:
				return null;
			case 2:
				return null;
			case 3:
				return null;
//				if (element instanceof ITaskContainer || element instanceof AbstractRepositoryQuery) {
//					return null;
//				} else {
//					return element.getPriority();
//				}
			case 4:
				return super.getText(obj);
			}
		}
		return null;
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (!(element instanceof ITaskListElement)) {
			return null;
		}
		if (columnIndex == 0) {
			if (element instanceof ITaskContainer) {
				return super.getImage(element);
			} else {
				ITask task = TaskElementLabelProvider.getCorrespondingTask((ITaskListElement)element);
				if (task != null) {
					if (task.isActive()) {
						return TaskListImages.getImage(TaskListImages.TASK_ACTIVE);
					} else {
						if (MylarPlugin.getContextManager().hasContext(task.getHandleIdentifier())) {
							return TaskListImages.getImage(TaskListImages.TASK_INACTIVE_CONTEXT);
						} else {
							return TaskListImages.getImage(TaskListImages.TASK_INACTIVE);
						}
					}
				} else {
					return TaskListImages.getImage(TaskListImages.TASK_INACTIVE);
				}
			}
		} else if (columnIndex == 1) {
			if (element instanceof ITaskContainer || element instanceof AbstractRepositoryQuery) {
				return null;
			}
			return super.getImage(element); 
		} else if (columnIndex == 2) {
			if (element instanceof ITaskListElement && !(element instanceof ITaskContainer)) {
				ITaskListElement taskElement = (ITaskListElement) element;
				return getImageForPriority(PriorityLevel.fromString(taskElement.getPriority()));
			}
		} else if (columnIndex == 3) {
			AbstractRepositoryTask repositoryTask = null;
			if (element instanceof AbstractQueryHit) {
				repositoryTask = ((AbstractQueryHit)element).getCorrespondingTask();
			} else if (element instanceof AbstractRepositoryTask) {
				repositoryTask = (AbstractRepositoryTask)element;
			}
			if (repositoryTask != null) {
				if (!repositoryTask.hasServerContext()) {
					if (repositoryTask.getSyncState() == RepositoryTaskSyncState.OUTGOING) {
						return TaskListImages.getImage(TaskListImages.STATUS_NORMAL_OUTGOING);
					} else if (repositoryTask.getSyncState() == RepositoryTaskSyncState.INCOMING) {
						return TaskListImages.getImage(TaskListImages.STATUS_NORMAL_INCOMING);
					} else if (repositoryTask.getSyncState() == RepositoryTaskSyncState.CONFLICT) {
						return TaskListImages.getImage(TaskListImages.STATUS_NORMAL_CONFLICT);
					}
				} else {
					if (repositoryTask.getSyncState() == RepositoryTaskSyncState.OUTGOING) {
						return TaskListImages.getImage(TaskListImages.STATUS_CONTEXT_OUTGOING);
					} else if (repositoryTask.getSyncState() == RepositoryTaskSyncState.INCOMING) {
						return TaskListImages.getImage(TaskListImages.STATUS_CONTEXT_INCOMING);
					} else if (repositoryTask.getSyncState() == RepositoryTaskSyncState.CONFLICT) {
						return TaskListImages.getImage(TaskListImages.STATUS_CONTEXT_CONFLICT);
					}
				}
			} else if (element instanceof AbstractQueryHit){
				return TaskListImages.getImage(TaskListImages.STATUS_NORMAL_INCOMING);
			} 
		}
		return null;
	}
	
	public Font getFont(Object element, int columnIndex) {
		return super.getFont(element);
	}

	public Color getForeground(Object element, int columnIndex) {
		return super.getForeground(element);
	}

	public Color getBackground(Object element, int columnIndex) {
		if (element instanceof ITaskContainer) {
			ITaskContainer category = (ITaskContainer) element;
			if (category.isArchive()) {
				return TaskListColorsAndFonts.BACKGROUND_ARCHIVE;
			} else {
				return parentBackgroundColor;
			}
		} else if (element instanceof AbstractRepositoryQuery) {
			return parentBackgroundColor;
		}

		return super.getBackground(element);
	}
}
