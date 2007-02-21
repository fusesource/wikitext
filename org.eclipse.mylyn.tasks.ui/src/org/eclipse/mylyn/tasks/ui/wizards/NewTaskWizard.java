/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.tasks.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.tasks.core.AbstractAttributeFactory;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.ITaskDataHandler;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;
import org.eclipse.mylar.tasks.core.Task;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.TasksUiUtil;
import org.eclipse.mylar.tasks.ui.editors.NewTaskEditorInput;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * @author Steffen Pingel
 */
public class NewTaskWizard extends Wizard implements INewWizard {

	private TaskRepository taskRepository;

	public NewTaskWizard(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
	}

	@Override
	public boolean canFinish() {
		return true;
	}

	@Override
	public boolean performFinish() {
		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
				taskRepository.getKind());

		final ITaskDataHandler taskDataHandler = (ITaskDataHandler) connector.getTaskDataHandler();
		if (taskDataHandler == null) {
			MylarStatusHandler.displayStatus("Error creating new task", new Status(IStatus.ERROR,
					TasksUiPlugin.PLUGIN_ID, "The selected repository does not support creating new tasks."));
			return false;
		}

		AbstractAttributeFactory attributeFactory = taskDataHandler.getAttributeFactory(taskRepository.getUrl(), taskRepository.getKind(), Task.DEFAULT_TASK_KIND);
		
		final RepositoryTaskData taskData = new RepositoryTaskData(attributeFactory, taskRepository.getKind(),
				taskRepository.getUrl(), TasksUiPlugin.getDefault().getNextNewRepositoryTaskId(), Task.DEFAULT_TASK_KIND);
		taskData.setNew(true);

		try {
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						if (!taskDataHandler.initializeTaskData(taskRepository, taskData, monitor)) {
							throw new CoreException(new Status(IStatus.ERROR,
									TasksUiPlugin.PLUGIN_ID, "The selected repository does not support creating new tasks."));
						}
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};

			getContainer().run(true, true, runnable);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				MylarStatusHandler.displayStatus("Error creating new task", ((CoreException) e.getCause()).getStatus());
			} else {
				MylarStatusHandler.fail(e.getCause(), "Error creating new task", true);
			}
			return false;
		} catch (InterruptedException e) {
			return false;
		}

		NewTaskEditorInput editorInput = new NewTaskEditorInput(taskRepository, taskData);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		TasksUiUtil.openEditor(editorInput, TaskListPreferenceConstants.TASK_EDITOR_ID, page);
		return true;
	}
}
