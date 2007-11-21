/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.AbstractAttributeFactory;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.editors.NewTaskEditorInput;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * @author Steffen Pingel
 * 
 * TODO support connectors that do not have a rich editor
 */
public class NewSubTaskAction extends Action implements IViewActionDelegate, IExecutableExtension {

	private static final String TOOLTIP = "Create a new subtask";

	private static final String LABEL = "Subtask";

	public static final String ID = "org.eclipse.mylyn.tasks.ui.new.subtask";

	private AbstractTask selectedTask;

	public NewSubTaskAction() {
		super(LABEL);
		setToolTipText(TOOLTIP);
		setId(ID);
		setImageDescriptor(TasksUiImages.NEW_SUB_TASK);
	}

	@Override
	public void run() {
		if (selectedTask == null) {
			return;
		}

		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
				selectedTask.getConnectorKind());
		final AbstractTaskDataHandler taskDataHandler = connector.getTaskDataHandler();
		if (taskDataHandler == null || !taskDataHandler.canInitializeSubTaskData()) {
			return;
		}

		String repositoryUrl = selectedTask.getRepositoryUrl();
		final RepositoryTaskData selectedTaskData = TasksUiPlugin.getTaskDataManager().getNewTaskData(repositoryUrl,
				selectedTask.getTaskId());
		if (selectedTaskData == null) {
			StatusHandler.displayStatus("Unable to create subtask", new Status(IStatus.WARNING,
					TasksUiPlugin.ID_PLUGIN, "Could not retrieve task data for task: " + selectedTask.getUrl()));
			return;
		}

		final TaskRepository taskRepository = TasksUiPlugin.getRepositoryManager().getRepository(repositoryUrl);
		AbstractAttributeFactory attributeFactory = taskDataHandler.getAttributeFactory(taskRepository.getUrl(),
				taskRepository.getConnectorKind(), AbstractTask.DEFAULT_TASK_KIND);
		final RepositoryTaskData taskData = new RepositoryTaskData(attributeFactory, selectedTask.getConnectorKind(),
				taskRepository.getUrl(), TasksUiPlugin.getDefault().getNextNewRepositoryTaskId());
		taskData.setNew(true);

		final boolean[] result = new boolean[1];
		IProgressService service = PlatformUI.getWorkbench().getProgressService();
		try {
			service.run(false, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						result[0] = taskDataHandler.initializeSubTaskData(taskRepository, taskData, selectedTaskData,
								new NullProgressMonitor());
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			StatusHandler.displayStatus("Unable to create subtask", ((CoreException) e.getCause()).getStatus());
			return;
		} catch (InterruptedException e) {
			// canceled
			return;
		}

		if (result[0]) {
			// open editor
			NewTaskEditorInput editorInput = new NewTaskEditorInput(taskRepository, taskData);
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			TasksUiUtil.openEditor(editorInput, TaskEditor.ID_EDITOR, page);
		} else {
			StatusHandler.displayStatus("Unable to create subtask", new Status(IStatus.INFO, TasksUiPlugin.ID_PLUGIN,
					"The connector does not support creating subtasks for this task"));
		}
	}

	public void run(IAction action) {
		run();
	}

	public void init(IViewPart view) {
	}

	public void selectionChanged(IAction action, ISelection selection) {
		selectedTask = null;
		if (selection instanceof StructuredSelection) {
			Object selectedObject = ((StructuredSelection) selection).getFirstElement();
			if (selectedObject instanceof AbstractTask) {
				selectedTask = (AbstractTask) selectedObject;

				AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
						selectedTask.getConnectorKind());
				final AbstractTaskDataHandler taskDataHandler = connector.getTaskDataHandler();
				if (taskDataHandler == null || !taskDataHandler.canInitializeSubTaskData()) {
					selectedTask = null;
				}
			}
		}

		action.setEnabled(selectedTask != null);
	}

	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
	}

}
