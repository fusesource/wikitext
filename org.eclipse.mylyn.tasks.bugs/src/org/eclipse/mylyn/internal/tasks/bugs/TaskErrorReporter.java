/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.bugs;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.mylyn.commons.core.AbstractErrorReporter;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractAttributeFactory;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractLegacyRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractTaskDataHandler;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryTaskData;
import org.eclipse.mylyn.internal.tasks.core.deprecated.TaskSelection;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.deprecated.NewTaskEditorInput;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * @author Steffen Pingel
 */
public class TaskErrorReporter extends AbstractErrorReporter {

	private PluginRepositoryMappingManager manager;

	TaskErrorReporter() {
		manager = new PluginRepositoryMappingManager();
	}

	public boolean isEnabled() {
		return manager.hasMappings();
	}
	
	@Override
	public int getPriority(IStatus status) {
		Assert.isNotNull(status);

		String pluginId = status.getPlugin();
		for (int i = 0; i <= pluginId.length(); i++) {
			if (manager.getMapping(pluginId.substring(0, i)) != null) {
				return PRIORITY_DEFAULT;
			}
		}

		return PRIORITY_NONE;
	}

	@Override
	public void handle(IStatus status) {
		Assert.isNotNull(status);

		String pluginId = status.getPlugin();
		Map<String, String> attributes = manager.getAllAttributes(pluginId);

		openNewTaskEditor(status, attributes);
	}

	private void openNewTaskEditor(IStatus status, Map<String, String> attributes) {
		AttributeTaskMapper mapper = new AttributeTaskMapper(attributes);

		TaskRepository taskRepository = mapper.getTaskRepository();
		try {
			if (taskRepository != null) {
				RepositoryTaskData taskData = createTaskData(taskRepository, mapper);
				if (taskData != null) {
					taskData.setSummary(status.getMessage());

					TaskContributorManager manager = new TaskContributorManager();
					manager.updateAttributes(taskData, status);

					String editorId = manager.getEditorId(status);

					NewTaskEditorInput editorInput = new NewTaskEditorInput(taskRepository, taskData);
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					TasksUiUtil.openEditor(editorInput, editorId, page);
					return;
				}
			}

			TaskSelection taskSelection = mapper.createTaskSelection();
			updateAttributes(taskSelection.getLegacyTaskData(), status);

			// fall back to opening wizard
			TasksUiUtil.openNewTaskEditor(null, taskSelection, taskRepository);
		} catch (OperationCanceledException e) {
			// ignore
		}
	}

	private RepositoryTaskData createTaskData(final TaskRepository taskRepository, AttributeTaskMapper mapper) {
		AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
				taskRepository.getConnectorKind());
		if (!(connector instanceof AbstractLegacyRepositoryConnector)) {
			return null;
		}
		final AbstractTaskDataHandler taskDataHandler = ((AbstractLegacyRepositoryConnector) connector).getLegacyTaskDataHandler();
		if (taskDataHandler == null) {
			return null;
		}

		AbstractAttributeFactory attributeFactory = taskDataHandler.getAttributeFactory(taskRepository.getRepositoryUrl(),
				taskRepository.getConnectorKind(), AbstractTask.DEFAULT_TASK_KIND);

		final RepositoryTaskData taskData = new RepositoryTaskData(attributeFactory, taskRepository.getConnectorKind(),
				taskRepository.getRepositoryUrl(), TasksUiPlugin.getDefault().getNextNewRepositoryTaskId());
		taskData.setNew(true);

		mapper.applyTo(taskData);

		try {
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						if (!taskDataHandler.initializeTaskData(taskRepository, taskData, monitor)) {
							throw new InvocationTargetException(new Exception());
						}
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} catch (OperationCanceledException e) {
						throw new InterruptedException();
					}
				}
			};

			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		} catch (InvocationTargetException e) {
			return null;
		} catch (InterruptedException e) {
			throw new OperationCanceledException();
		}

		taskDataHandler.cloneTaskData(mapper.createTaskSelection().getLegacyTaskData(), taskData);

		return taskData;
	}

	private void updateAttributes(RepositoryTaskData taskData, IStatus status) {

	}

}
