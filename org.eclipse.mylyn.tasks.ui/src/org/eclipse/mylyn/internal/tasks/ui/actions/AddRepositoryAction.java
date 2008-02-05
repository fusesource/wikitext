/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.mylyn.internal.tasks.ui.ITaskCommandIds;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * @author Mik Kersten
 */
public class AddRepositoryAction extends Action {

	public static final String TITLE = "Add Task Repository";

	private static final String ID = "org.eclipse.mylyn.tasklist.repositories.add";

	public AddRepositoryAction() {
		setImageDescriptor(TasksUiImages.REPOSITORY_NEW);
		setText(TITLE);
		setId(ID);
		setEnabled(TasksUiPlugin.getRepositoryManager().hasUserManagedRepositoryConnectors());
	}

	@Override
	public void run() {
		IHandlerService handlerSvc = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
		try {
			handlerSvc.executeCommand(ITaskCommandIds.ADD_TASK_REPOSITORY, null);
		} catch (Exception e) {
			StatusHandler.fail(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, e.getMessage(), e));
		}
	}

}
