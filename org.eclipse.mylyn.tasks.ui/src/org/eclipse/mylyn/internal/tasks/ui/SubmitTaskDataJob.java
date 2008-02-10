/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.internal.tasks.ui;

import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;

/**
 * @author Steffen Pingel
 */
public class SubmitTaskDataJob extends Job {

	private static final String LABEL_JOB_SUBMIT = "Submitting to repository";

	private final TaskRepository taskRepository;

	private final RepositoryTaskData taskData;

	private final AbstractRepositoryConnector connector;

	private boolean attachContext;

	private IStatus errorStatus;

	private AbstractTask task;

	public SubmitTaskDataJob(AbstractRepositoryConnector connector, TaskRepository taskRepository,
			RepositoryTaskData taskData) {
		super(LABEL_JOB_SUBMIT);
		this.connector = connector;
		this.taskRepository = taskRepository;
		this.taskData = taskData;
	}

	public boolean getAttachContext() {
		return attachContext;
	}

	public void setAttachContext(boolean attachContext) {
		this.attachContext = attachContext;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			monitor.beginTask("Submitting task", 3);
			String taskId = connector.getTaskDataHandler().postTaskData(taskRepository, taskData,
					new SubProgressMonitor(monitor, 1));
			if (taskData.isNew()) {
				if (taskId == null) {
					throw new CoreException(new RepositoryStatus(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN,
							RepositoryStatus.ERROR_INTERNAL,
							"Task could not be created. No additional information was provided by the connector."));
				}
				
				task = connector.createTaskFromExistingId(taskRepository, taskId,
						new SubProgressMonitor(monitor, 1));
				if (task == null) {
					throw new CoreException(new RepositoryStatus(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN,
							RepositoryStatus.ERROR_INTERNAL,
							"Task could not be created. No additional information was provided by the connector."));
				}
			} else {
				task = TasksUiPlugin.getTaskListManager().getTaskList().getTask(taskRepository.getUrl(),
						taskData.getId());
			}

			if (task == null) {
				// repository task only
				return Status.OK_STATUS;
			}

			// attach context if required
			if (attachContext && connector.getAttachmentHandler() != null) {
				connector.getAttachmentHandler().attachContext(taskRepository, task, "",
						new SubProgressMonitor(monitor, 1));
			}

			// initiate task list update
			TasksUiPlugin.getSynchronizationScheduler().synchNow(0, Collections.singletonList(taskRepository), false);

			// synchronize task
			task.setSubmitting(true);
			try {
				Job synchronizeJob = TasksUiPlugin.getSynchronizationManager().synchronize(connector, task, true, null);
				synchronizeJob.join();
			} finally {
				task.setSubmitting(false);
			}
			
			TasksUiPlugin.getSynchronizationManager().setTaskRead(task, true);
		} catch (CoreException e) {
			errorStatus = e.getStatus();
		} catch (Exception e) {
			errorStatus = new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, e.getMessage(), e);
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	public IStatus getError() {
		return errorStatus;
	}

	public AbstractTask getTask() {
		return task;
	}

}
