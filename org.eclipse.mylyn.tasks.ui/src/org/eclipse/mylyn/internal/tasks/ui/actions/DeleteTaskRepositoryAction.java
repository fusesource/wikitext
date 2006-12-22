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

package org.eclipse.mylar.internal.tasks.ui.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylar.context.core.MylarStatusHandler;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.internal.WorkbenchImages;

/**
 * @author Mik Kersten
 */
public class DeleteTaskRepositoryAction extends BaseSelectionListenerAction {

	private static final String ID = "org.eclipse.mylar.tasklist.repositories.delete";

	public DeleteTaskRepositoryAction() {
		super("Delete Repository");
		setImageDescriptor(WorkbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		setId(ID);
		setEnabled(false);
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		return selection != null && !selection.isEmpty();
	}

	public void init(IViewPart view) {
		// ignore
	}
	
	@Override
	public void run() {
		try {

			boolean deleteConfirmed = MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getShell(), "Confirm Delete", "Delete the selected task repositories?");
			if (deleteConfirmed) {
				IStructuredSelection selection = getStructuredSelection();
				Set<AbstractRepositoryQuery> queries = TasksUiPlugin.getTaskListManager().getTaskList()
						.getQueries();
				List<TaskRepository> repositoriesInUse = new ArrayList<TaskRepository>();
				List<TaskRepository> repositoriesToDelete = new ArrayList<TaskRepository>();
				for (Object selectedObject : selection.toList()) {
					if (selectedObject instanceof TaskRepository) {
						TaskRepository taskRepository = (TaskRepository) selectedObject;
						if (queries != null && queries.size() > 0) {
							for (AbstractRepositoryQuery query : queries) {
								if (query.getRepositoryUrl().equals(taskRepository.getUrl())) {
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
					TasksUiPlugin.getRepositoryManager().removeRepository(taskRepository, TasksUiPlugin.getDefault().getRepositoriesFilePath());
				}

				if (repositoriesInUse.size() > 0) {
					MessageDialog
							.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
									"Repository In Use",
									"One or more of the selected repositories is being used by a query and can not be deleted.");
				}
			}
		} catch (Exception e) {
			MylarStatusHandler.fail(e, e.getMessage(), true);
		}
	}
}
