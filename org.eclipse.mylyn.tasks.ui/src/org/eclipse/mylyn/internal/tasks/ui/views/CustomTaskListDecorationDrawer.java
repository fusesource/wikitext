/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.ui.views;

import java.util.Arrays;

import org.eclipse.mylar.context.core.ContextCorePlugin;
import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.AbstractTaskContainer;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskListElement;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask.RepositoryTaskSyncState;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * @author Mik Kersten
 */
class CustomTaskListDecorationDrawer implements Listener {

	private final TaskListView taskListView;

	private int activationImageOffset;

	private Image taskActive = TasksUiImages.getImage(TasksUiImages.TASK_ACTIVE);

	private Image taskInactive = TasksUiImages.getImage(TasksUiImages.TASK_INACTIVE);

	private Image taskInactiveContext = TasksUiImages.getImage(TasksUiImages.TASK_INACTIVE_CONTEXT);

	CustomTaskListDecorationDrawer(TaskListView taskListView, int activationImageOffset) {
		this.taskListView = taskListView;
		this.activationImageOffset = activationImageOffset;
		this.taskListView.synchronizationOverlaid = TasksUiPlugin.getDefault().getPluginPreferences().getBoolean(
				TaskListPreferenceConstants.INCOMING_OVERLAID);
	}

	/*
	 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly.
	 * Therefore, it is critical for performance that these methods be as
	 * efficient as possible.
	 */
	public void handleEvent(Event event) {
		Object data = event.item.getData();
		ITask task = null;
		Image activationImage = null;
		if (data instanceof ITask) {
			task = (ITask) data;
		} else if (data instanceof AbstractQueryHit) {
			task = ((AbstractQueryHit) data).getCorrespondingTask();
		}
		if (task != null) {
			if (task.isActive()) {
				activationImage = taskActive;
			} else if (ContextCorePlugin.getContextManager().hasContext(task.getHandleIdentifier())) {
				activationImage = taskInactiveContext;
			} else {
				activationImage = taskInactive;
			}
		} else if (data instanceof AbstractQueryHit) {
			activationImage = taskInactive;
		}
		if (data instanceof ITaskListElement) {
			switch (event.type) {
			case SWT.EraseItem: {
				if (activationImage != null) {
					drawActivationImage(activationImageOffset, event, activationImage);
				}
				if (!this.taskListView.synchronizationOverlaid) {
					if (data instanceof ITaskListElement) {
						drawSyncronizationImage((ITaskListElement) data, event);
					}
				}
//				currWidth = event.width;
				break;
			}
			case SWT.PaintItem: {
				if (activationImage != null) {
					drawActivationImage(activationImageOffset, event, activationImage);
				}
				if (data instanceof ITaskListElement) {
					drawSyncronizationImage((ITaskListElement) data, event);
				}
				break;
			}
			}
		}
	}

	private void drawSyncronizationImage(ITaskListElement element, Event event) {
//		if (this.taskListView.synchronizationOverlaid) {
//			Image image = TasksUiImages.getImage(TaskElementLabelProvider.getSynchronizationImageDescriptor(element));
//			if (image != null) {
//				event.gc.drawImage(image, event.x + 3, event.y + 4);
//			}
//		} else {
			Image image = null;
			int offsetX = 7;
			int offsetY = (event.height / 2) - 5;
			if (this.taskListView.synchronizationOverlaid) {
				offsetX = event.x + 3;
			}
			if (element instanceof AbstractTaskContainer) {
				if (element instanceof AbstractTaskContainer) {
					if (!Arrays.asList(this.taskListView.getViewer().getExpandedElements()).contains(element)
							&& hasIncoming((AbstractTaskContainer) element)) {
						image = TasksUiImages.getImage(TasksUiImages.STATUS_NORMAL_INCOMING);
						offsetX = 24;
					}
				}
			} else {
				image = TasksUiImages.getImage(TaskElementLabelProvider.getSynchronizationImageDescriptor(element));
//				image = TasksUiImages.getCompositeSynchImage(TaskElementLabelProvider
//						.getSynchronizationImageDescriptor(element), true);
			}
			if (image != null) {
				event.gc.drawImage(image, offsetX, event.y + offsetY);
//				event.gc.drawImage(image, currWidth - 16, event.y + 1);
			}
//		}
	}

	private boolean hasIncoming(AbstractTaskContainer container) {
		for (ITask task : container.getChildren()) {
			if (task instanceof AbstractRepositoryTask) {
				AbstractRepositoryTask containedRepositoryTask = (AbstractRepositoryTask) task;
				if (containedRepositoryTask.getSyncState() == RepositoryTaskSyncState.INCOMING) {
					return true;
				}
			}
		}
		if (container instanceof AbstractRepositoryQuery) {
			AbstractRepositoryQuery query = (AbstractRepositoryQuery) container;
			for (AbstractQueryHit hit : query.getHits()) {
				if (hit.getCorrespondingTask() == null) {
					return true;
				}
			}
		}
		return false;
	}

// private void drawPriorityImage(ITask task, Event event) {
// ImageDescriptor descriptor =
// TaskElementLabelProvider.getPriorityImageDescriptor(task);
// if (descriptor != null) {
// Image image = TasksUiImages.getImage(descriptor);
// if (image != null) {
// event.gc.drawImage(image, event.x +
// CompositeTaskImageDescriptor.WIDTH_DECORATION-4, event.y);
// }
// }
// }

	private void drawActivationImage(final int activationImageOffset, Event event, Image image) {
		Rectangle rect = image.getBounds();
		int offset = Math.max(0, (event.height - rect.height) / 2);
		event.gc.drawImage(image, activationImageOffset, event.y + offset);
	}
}