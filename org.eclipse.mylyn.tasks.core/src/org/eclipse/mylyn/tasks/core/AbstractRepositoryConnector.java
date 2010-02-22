/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core;

import java.util.Collection;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.data.TaskRelation;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;

/**
 * Encapsulates common operations that can be performed on a task repository. Extend to connect with a Java API or WS
 * API for accessing the repository. Only methods that take a progress monitor can do network I/O.
 * 
 * @author Mik Kersten
 * @author Rob Elves
 * @author Shawn Minto
 * @since 2.0
 */
public abstract class AbstractRepositoryConnector {

	private static final long REPOSITORY_CONFIGURATION_UPDATE_INTERVAL = 24 * 60 * 60 * 1000;

	/**
	 * Returns true, if the connector provides a wizard for creating new tasks.
	 * 
	 * @since 2.0
	 */
	// TODO move this to ConnectorUi.hasNewTaskWizard()
	public abstract boolean canCreateNewTask(TaskRepository repository);

	/**
	 * Returns true, if the connector supports retrieval of tasks based on String keys.
	 * 
	 * @since 2.0
	 */
	public abstract boolean canCreateTaskFromKey(TaskRepository repository);

	/**
	 * @since 3.0
	 */
	public boolean canQuery(TaskRepository repository) {
		return true;
	}

	/**
	 * @since 3.0
	 */
	public boolean canSynchronizeTask(TaskRepository taskRepository, ITask task) {
		return true;
	}

	/**
	 * Whether the connector can delete the task on the Task Repository
	 * 
	 * @since 3.3
	 */
	public boolean canDeleteTask(TaskRepository repository, ITask task) {
		return false;
	}

	/**
	 * @return the unique kind of the repository, e.g. "bugzilla"
	 * @since 2.0
	 */
	public abstract String getConnectorKind();

	/**
	 * The connector's summary i.e. "JIRA (supports 3.3.1 and later)"
	 * 
	 * @since 2.0
	 */
	public abstract String getLabel();

	/**
	 * Can return null if URLs are not used to identify tasks.
	 */
	public abstract String getRepositoryUrlFromTaskUrl(String taskFullUrl);

	/**
	 * Returns a short label for the connector, e.g. Bugzilla.
	 * 
	 * @since 2.3
	 */
	public String getShortLabel() {
		String label = getLabel();
		if (label == null) {
			return null;
		}

		int i = label.indexOf("("); //$NON-NLS-1$
		if (i != -1) {
			return label.substring(0, i).trim();
		}

		i = label.indexOf(" "); //$NON-NLS-1$
		if (i != -1) {
			return label.substring(0, i).trim();
		}

		return label;
	}

	/**
	 * @since 3.0
	 */
	public AbstractTaskAttachmentHandler getTaskAttachmentHandler() {
		return null;
	}

	/**
	 * @since 3.0
	 */
	public abstract TaskData getTaskData(TaskRepository taskRepository, String taskId, IProgressMonitor monitor)
			throws CoreException;

	/**
	 * @since 3.0
	 */
	public AbstractTaskDataHandler getTaskDataHandler() {
		return null;
	}

	/**
	 * @since 2.0
	 */
	public abstract String getTaskIdFromTaskUrl(String taskFullUrl);

	/**
	 * Used for referring to the task in the UI.
	 */
	public String getTaskIdPrefix() {
		return "task"; //$NON-NLS-1$
	}

	/**
	 * @since 2.0
	 */
	public String[] getTaskIdsFromComment(TaskRepository repository, String comment) {
		return null;
	}

	/**
	 * @since 3.0
	 */
	public ITaskMapping getTaskMapping(TaskData taskData) {
		return new TaskMapper(taskData);
	}

	/**
	 * Connectors can override to return other tasks associated with this task.
	 * 
	 * @since 3.0
	 */
	public Collection<TaskRelation> getTaskRelations(TaskData taskData) {
		return null;
	}

	/**
	 * @since 2.0
	 */
	public abstract String getTaskUrl(String repositoryUrl, String taskId);

	/**
	 * @since 3.0
	 */
	public abstract boolean hasTaskChanged(TaskRepository taskRepository, ITask task, TaskData taskData);

	/**
	 * @since 3.0
	 */
	public boolean hasLocalCompletionState(TaskRepository taskRepository, ITask task) {
		return false;
	}

	/**
	 * @since 3.0
	 */
	public boolean hasRepositoryDueDate(TaskRepository taskRepository, ITask task, TaskData taskData) {
		return false;
	}

	/**
	 * Default implementation returns true every 24hrs.
	 * 
	 * @return true to indicate that the repository configuration is stale and requires update
	 * @since 3.0
	 */
	public boolean isRepositoryConfigurationStale(TaskRepository repository, IProgressMonitor monitor)
			throws CoreException {
		Date configDate = repository.getConfigurationDate();
		if (configDate != null) {
			return (new Date().getTime() - configDate.getTime()) > REPOSITORY_CONFIGURATION_UPDATE_INTERVAL;
		}
		return true;
	}

	/**
	 * @since 2.0
	 */
	public boolean isUserManaged() {
		return true;
	}

	/**
	 * Runs <code>query</code> on <code>repository</code>, results are passed to <code>collector</code>. If a repository
	 * does not return the full task data for a result, {@link TaskData#isPartial()} will return true.
	 * <p>
	 * Implementors must complete executing <code>query</code> before returning from this method.
	 * 
	 * @param repository
	 *            task repository to run query against
	 * @param query
	 *            query to run
	 * @param collector
	 *            callback for returning results
	 * @param session
	 *            provides additional information for running the query, may be <code>null</code>
	 * @param monitor
	 *            for reporting progress
	 * @return {@link Status#OK_STATUS} in case of success, an error status otherwise
	 * @throws OperationCanceledException
	 *             if the query was canceled
	 * @since 3.0
	 */
	public abstract IStatus performQuery(TaskRepository repository, IRepositoryQuery query,
			TaskDataCollector collector, ISynchronizationSession session, IProgressMonitor monitor);

	/**
	 * Delete the task from the server
	 * 
	 * @throws UnsupportedOperationException
	 *             if this is not implemented by the connector
	 * @since 3.3
	 */
	public IStatus deleteTask(TaskRepository repository, ITask task, IProgressMonitor monitor) throws CoreException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Hook into the synchronization process.
	 * 
	 * @since 3.0
	 */
	public void postSynchronization(ISynchronizationSession event, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
	}

	/**
	 * Hook into the synchronization process.
	 * 
	 * @since 3.0
	 */
	public void preSynchronization(ISynchronizationSession event, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
	}

	/**
	 * Updates the local repository configuration cache (e.g. products and components). Connectors are encouraged to
	 * implement {@link #updateRepositoryConfiguration(TaskRepository, ITask, IProgressMonitor)} in addition this
	 * method.
	 * 
	 * @param repository
	 *            the repository to update configuration for
	 * @since 3.0
	 * @see #isRepositoryConfigurationStale(TaskRepository, IProgressMonitor)
	 */
	public abstract void updateRepositoryConfiguration(TaskRepository taskRepository, IProgressMonitor monitor)
			throws CoreException;

	/**
	 * Updates the local repository configuration cache (e.g. products and components). The default implementation
	 * invokes {@link #updateRepositoryConfiguration(TaskRepository, IProgressMonitor)}.
	 * 
	 * @param repository
	 *            the repository to update configuration for
	 * @param task
	 *            if not null, limit the update to the details relevant to task
	 * @see #updateRepositoryConfiguration(TaskRepository, IProgressMonitor)
	 * @since 3.3
	 */
	public void updateRepositoryConfiguration(TaskRepository taskRepository, ITask task, IProgressMonitor monitor)
			throws CoreException {
		updateRepositoryConfiguration(taskRepository, monitor);
	}

	/**
	 * @since 3.0
	 */
	public abstract void updateTaskFromTaskData(TaskRepository taskRepository, ITask task, TaskData taskData);

}
