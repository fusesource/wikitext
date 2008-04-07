/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylyn.internal.tasks.ui.TaskRepositoryUtil;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;

/**
 * @author Mik Kersten
 */
public class DeleteTaskRepositoryAction extends AbstractTaskRepositoryAction {

	private static final String ID = "org.eclipse.mylyn.tasklist.repositories.delete";

	public DeleteTaskRepositoryAction() {
		super("Delete Repository");
		setImageDescriptor(WorkbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		setId(ID);
		setEnabled(false);
		setActionDefinitionId(IWorkbenchActionDefinitionIds.DELETE);
	}

	public void init(IViewPart view) {
		// ignore
	}

	@Override
	public void run() {
		try {

			boolean deleteConfirmed = MessageDialog.openQuestion(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow()
					.getShell(), "Confirm Delete", "Delete the selected task repositories?");
			if (deleteConfirmed) {
				IStructuredSelection selection = getStructuredSelection();
				Set<AbstractRepositoryQuery> queries = TasksUiPlugin.getTaskListManager().getTaskList().getQueries();
				List<TaskRepository> repositoriesInUse = new ArrayList<TaskRepository>();
				List<TaskRepository> repositoriesToDelete = new ArrayList<TaskRepository>();
				for (Object selectedObject : selection.toList()) {
					if (selectedObject instanceof TaskRepository) {
						TaskRepository taskRepository = (TaskRepository) selectedObject;
						if (queries != null && queries.size() > 0) {
							for (AbstractRepositoryQuery query : queries) {
								if (query.getRepositoryUrl().equals(taskRepository.getRepositoryUrl())) {
									repositoriesInUse.add(taskRepository);
									break;
								}
							}
						}
						if (!repositoriesInUse.contains(taskRepository)) {
							repositoriesToDelete.add(taskRepository);
						}
					}
				}

				for (TaskRepository taskRepository : repositoriesToDelete) {
					TasksUiPlugin.getRepositoryManager().removeRepository(taskRepository,
							TasksUiPlugin.getDefault().getRepositoriesFilePath());
					// if repository is contributed via template, ensure it isn't added again
					TaskRepositoryUtil.disableAddAutomatically(taskRepository.getRepositoryUrl());
				}

				if (repositoriesInUse.size() > 0) {
					MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
							"Repository In Use",
							"One or more of the selected repositories is being used by a query and can not be deleted.");
				}
			}
		} catch (Exception e) {
			StatusHandler.fail(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, e.getMessage(), e));
		}
	}
}
