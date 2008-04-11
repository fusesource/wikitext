/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * @author Steffen Pingel
 */
public class RefreshRepositoryTasksAction extends AbstractTaskRepositoryAction implements IViewActionDelegate {

	private static final String ID = "org.eclipse.mylyn.tasklist.repositories.refreshAllTasks";

	public RefreshRepositoryTasksAction() {
		super("Refresh All Tasks");
		setId(ID);
		setEnabled(false);
	}

	@Override
	public void run() {
		IStructuredSelection selection = getStructuredSelection();
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			Object selectedObject = iter.next();
			if (selectedObject instanceof TaskRepository) {
				TaskRepository repository = (TaskRepository) selectedObject;
				synchronizeAllTasks(repository);
			}
		}
	}

	private void synchronizeAllTasks(TaskRepository repository) {
		final AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
				repository.getConnectorKind());
		if (connector != null) {
			Set<AbstractTask> repositoryTasks = TasksUi.getTaskListManager().getTaskList().getRepositoryTasks(
					repository.getRepositoryUrl());
			TasksUi.synchronizeTasks(connector, repositoryTasks, true, null);
		}
	}

	public void init(IViewPart view) {
	}

	public void run(IAction action) {
		run();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		selectionChanged((IStructuredSelection) selection);
	}

}
