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

package org.eclipse.mylyn.internal.tasks.ui.views;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.graphics.Image;

/**
 * @author Mik Kersten
 */
public class TaskRepositoryLabelProvider implements ILabelProvider {

//extends LabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object obj, int index) {
		if (index == 0) {
			return getImage(obj);
		} else {
			return null;
		}
	}

	public Image getImage(Object object) {
		if (object instanceof AbstractRepositoryConnector) {
			AbstractRepositoryConnector repositoryConnector = (AbstractRepositoryConnector) object;
			Image image = TasksUiPlugin.getDefault().getBrandingIcon(repositoryConnector.getConnectorKind());
			if (image != null) {
				return image;
			} else {
				return TasksUiImages.getImage(TasksUiImages.REPOSITORY);
			}
		} else if (object instanceof TaskRepository) {
			if (((TaskRepository) object).isOffline()) {
				return TasksUiImages.getImageWithOverlay(TasksUiImages.REPOSITORY, TasksUiImages.OVERLAY_OFFLINE, true,
						false);
			} else {
				return TasksUiImages.getImage(TasksUiImages.REPOSITORY);
			}
		}
		return null;
	}

	public String getText(Object object) {
		if (object instanceof TaskRepository) {
			TaskRepository repository = (TaskRepository) object;
			return repository.getRepositoryLabel();
		} else if (object instanceof AbstractRepositoryConnector) {
			return ((AbstractRepositoryConnector) object).getLabel();
		} else {
			return getText(object);
		}
	}

	public void addListener(ILabelProviderListener listener) {
		// ignore

	}

	public void dispose() {
		// ignore

	}

	public boolean isLabelProperty(Object element, String property) {
		// ignore
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		// ignore

	}
}
