/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core.sync;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.TaskList;
import org.eclipse.mylyn.internal.tasks.core.data.TaskDataManager;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryManager;
import org.eclipse.mylyn.tasks.core.IRepositoryModel;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.sync.SynchronizationJob;

/**
 * Updates the task list.
 * 
 * @author Steffen Pingel
 */
public class SynchronizeRepositoriesJob extends SynchronizationJob {

	private final TaskList taskList;

	private final TaskDataManager taskDataManager;

	private final IRepositoryManager repositoryManager;

	private Set<TaskRepository> repositories;

	private final Object family = new Object();

	private final IRepositoryModel tasksModel;

	public SynchronizeRepositoriesJob(TaskList taskList, TaskDataManager taskDataManager, IRepositoryModel tasksModel,
			IRepositoryManager repositoryManager) {
		super(Messages.SynchronizeRepositoriesJob_Synchronizing_Task_List);
		this.taskList = taskList;
		this.taskDataManager = taskDataManager;
		this.tasksModel = tasksModel;
		this.repositoryManager = repositoryManager;
	}

	public Collection<TaskRepository> getRepositories() {
		return Collections.unmodifiableCollection(repositories);
	}

	public void setRepositories(Collection<TaskRepository> repositories) {
		if (repositories != null) {
			this.repositories = new HashSet<TaskRepository>(repositories);
		} else {
			this.repositories = null;
		}
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		// get the current list of repositories
		Set<TaskRepository> repositories = this.repositories;
		if (repositories == null) {
			repositories = new HashSet<TaskRepository>(repositoryManager.getAllRepositories());
		}
		try {
			monitor.beginTask(Messages.SynchronizeRepositoriesJob_Processing, repositories.size() * 100);

			for (TaskRepository repository : repositories) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				if (repository.isOffline()) {
					monitor.worked(100);
					continue;
				}

				monitor.setTaskName(MessageFormat.format(Messages.SynchronizeRepositoriesJob_Processing_,
						repository.getRepositoryLabel()));

				final AbstractRepositoryConnector connector = repositoryManager.getRepositoryConnector(repository.getConnectorKind());
				Set<RepositoryQuery> queries = taskList.getRepositoryQueries(repository.getRepositoryUrl());

				if (isUser() || queries.isEmpty()) {
					monitor.worked(20);
				} else {
					// occasionally request update of repository configuration attributes
					updateRepositoryConfiguration(repository, connector, new SubProgressMonitor(monitor, 20));
				}

				updateQueries(repository, connector, queries, monitor);
			}

			// it's better to remove the job from the progress view instead of having it blocked until all child jobs finish
//			if (isUser()) {
//				Job.getJobManager().join(family, monitor);
//			}
		} catch (InterruptedException e) {
			return Status.CANCEL_STATUS;
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private void updateQueries(TaskRepository repository, final AbstractRepositoryConnector connector,
			Set<RepositoryQuery> queries, IProgressMonitor monitor) {
		if (isUser()) {
			for (RepositoryQuery query : queries) {
				query.setSynchronizing(true);
			}
			taskList.notifySynchronizationStateChanged(queries);
		}

		SynchronizeQueriesJob job = new SynchronizeQueriesJob(taskList, taskDataManager, tasksModel, connector,
				repository, queries) {
			@Override
			public boolean belongsTo(Object family) {
				return SynchronizeRepositoriesJob.this.family == family;
			}
		};
		job.setUser(isUser());
		job.setFullSynchronization(true);
		job.setPriority(Job.DECORATE);
		if (isUser()) {
			job.schedule();
		} else {
			job.run(new SubProgressMonitor(monitor, 80));
		}
	}

	public Object getFamily() {
		return family;
	}

	private void updateRepositoryConfiguration(TaskRepository repository, AbstractRepositoryConnector connector,
			IProgressMonitor monitor) throws InterruptedException {
		try {
			if (!isUser()) {
				monitor = Policy.backgroundMonitorFor(monitor);
			}
			monitor.beginTask(MessageFormat.format(
					Messages.SynchronizeRepositoriesJob_Updating_repository_configuration_for_X,
					repository.getRepositoryUrl()), 100);
			if (connector.isRepositoryConfigurationStale(repository, monitor)) {
				connector.updateRepositoryConfiguration(repository, monitor);
				repository.setConfigurationDate(new Date());
			}
		} catch (CoreException e) {
			repository.setStatus(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN,
					"Updating of repository configuration failed", e)); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
	}
}
