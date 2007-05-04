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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.mylar.internal.tasks.ui.views.TaskElementLabelProvider;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskListElement;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.core.Task.PriorityLevel;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * @author Mik Kersten
 */
public class TaskLabelDecorator implements ILightweightLabelDecorator {

	public void decorate(Object element, IDecoration decoration) {
		
		AbstractRepositoryConnectorUi connectorUi = null;
		ImageDescriptor priorityOverlay = null;
		if (element instanceof AbstractRepositoryTask) {
			AbstractRepositoryTask repositoryTask = (AbstractRepositoryTask) element;
			connectorUi = TasksUiPlugin.getRepositoryUi(((AbstractRepositoryTask) element).getRepositoryKind());
			if (connectorUi != null) {
				priorityOverlay = connectorUi.getTaskPriorityOverlay(repositoryTask);
			}
		}
		if (priorityOverlay == null && (element instanceof ITask || element instanceof AbstractQueryHit)) {
			ITask task = TaskElementLabelProvider.getCorrespondingTask((ITaskListElement) element);
			if (task != null) {
				priorityOverlay = TasksUiImages.getImageDescriptorForPriority(PriorityLevel.fromString(task
						.getPriority()));
			} else if (element instanceof AbstractQueryHit){
				priorityOverlay = TasksUiImages.getImageDescriptorForPriority(PriorityLevel.fromString(((AbstractQueryHit)element)
						.getPriority()));
			}
		}
		if (priorityOverlay != null) {
			decoration.addOverlay(priorityOverlay, IDecoration.BOTTOM_LEFT);
		}
		
		if (element instanceof AbstractRepositoryQuery) {
			AbstractRepositoryQuery query = (AbstractRepositoryQuery) element;
			String repositoryUrl = query.getRepositoryUrl();
			TaskRepository taskRepository = TasksUiPlugin.getRepositoryManager().getRepository(repositoryUrl);
			if (repositoryUrl != null && taskRepository != null) {
				if (taskRepository.getUrl().equals(taskRepository.getRepositoryLabel())) {
					try {
						URL url = new URL(repositoryUrl);
						decoration.addSuffix("   [" + url.getHost() + "]");
					} catch (MalformedURLException e) {
						decoration.addSuffix("   [ <unknown host> ]");
					}
				} else {
					decoration.addSuffix("   [" + taskRepository.getRepositoryLabel() + "]");
				}
			}
		} else if (element instanceof AbstractRepositoryTask) {
			AbstractRepositoryTask task = (AbstractRepositoryTask) element;
			if (!task.isCompleted() && TasksUiPlugin.getTaskListManager().isOverdue(task)) {
				decoration.addOverlay(TasksUiImages.OVERLAY_OVER_DUE, IDecoration.TOP_LEFT);
			} else if (!task.isCompleted() && task.getDueDate() != null) {
				decoration.addOverlay(TasksUiImages.OVERLAY_HAS_DUE, IDecoration.TOP_LEFT);
			}
		} else if (element instanceof AbstractQueryHit) {
			ITask correspondingTask = ((AbstractQueryHit) element).getCorrespondingTask();
			decorate(correspondingTask, decoration);
		} else if (element instanceof ITask) {
			ITask task = (ITask) element;
			if (!task.isCompleted() && TasksUiPlugin.getTaskListManager().isOverdue(task)) {
				decoration.addOverlay(TasksUiImages.OVERLAY_OVER_DUE, IDecoration.TOP_LEFT);
			}
		} else if (element instanceof TaskRepository) {
			ImageDescriptor overlay = TasksUiPlugin.getDefault().getOverlayIcon(((TaskRepository) element).getKind());
			if (overlay != null) {
				decoration.addOverlay(overlay, IDecoration.BOTTOM_RIGHT);
			}
		}
	}
	
	public void addListener(ILabelProviderListener listener) {
		// ignore
	}

	public void dispose() {
		// ignore
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		// ignore
	}

}
