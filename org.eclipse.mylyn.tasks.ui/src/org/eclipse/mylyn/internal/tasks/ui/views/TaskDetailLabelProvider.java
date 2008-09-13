/*******************************************************************************
 * Copyright (c) 2004, 2008 Willian Mitsuda and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Willian Mitsuda - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.swt.graphics.Image;

/**
 * Displays task repository info from a {@link AbstractTask}
 * 
 * @author Willian Mitsuda
 */
public class TaskDetailLabelProvider extends LabelProvider implements ILabelProvider {

	@Override
	public Image getImage(Object element) {
		if (!(element instanceof ITask)) {
			return super.getImage(element);
		}

		AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
				((ITask) element).getConnectorKind());
		ImageDescriptor overlay = TasksUiPlugin.getDefault().getOverlayIcon(connector.getConnectorKind());
		if (overlay != null) {
			return CommonImages.getImageWithOverlay(TasksUiImages.REPOSITORY, overlay, false, false);
		} else {
			return CommonImages.getImage(TasksUiImages.REPOSITORY);
		}
	}

	@Override
	public String getText(Object element) {
		if (!(element instanceof ITask)) {
			return super.getText(element);
		}

		ITask task = (ITask) element;
		TaskRepository repository = TasksUi.getRepositoryManager().getRepository(task.getConnectorKind(),
				task.getRepositoryUrl());
		return repository.getRepositoryLabel();
	}
}