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

package org.eclipse.mylyn.internal.tasks.ui.views;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.actions.DisconnectRepositoryAction;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
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
				return CommonImages.getImage(TasksUiImages.REPOSITORY);
			}
		} else if (object instanceof TaskRepository) {
			if (((TaskRepository) object).isOffline()) {
				return CommonImages.getImage(TasksUiImages.REPOSITORY_OFFLINE);
			} else {
				return CommonImages.getImage(TasksUiImages.REPOSITORY);
			}
		}
		return null;
	}

	public String getText(Object object) {

		if (object instanceof TaskRepository) {
			TaskRepository repository = (TaskRepository) object;
			StringBuilder label = new StringBuilder();
			label.append(repository.getRepositoryLabel());
			if (repository.isOffline()) {
				label.append(" [" + DisconnectRepositoryAction.LABEL + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return label.toString();
		} else if (object instanceof AbstractRepositoryConnector) {
			return ((AbstractRepositoryConnector) object).getLabel();
		} else {
			return null;
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
