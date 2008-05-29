/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core.sync;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.TaskList;
import org.eclipse.mylyn.internal.tasks.core.data.TaskDataManager;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractLegacyRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.core.deprecated.LegacyTaskDataCollector;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITasksModel;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.ITask.SynchronizationState;
import org.eclipse.mylyn.tasks.core.data.ITaskDataManager;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationContext;
import org.eclipse.mylyn.tasks.core.sync.SynchronizationJob;

/**
 * @author Mik Kersten
 * @author Rob Elves
 * @author Steffen Pingel
 */
public class SynchronizeQueriesJob extends SynchronizationJob {

	private static class MutexRule implements ISchedulingRule {

		private final Object object;

		public MutexRule(Object object) {
			this.object = object;
		}

		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			if (rule instanceof MutexRule) {
				return object.equals(((MutexRule) rule).object);
			}
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	private class TaskCollector extends LegacyTaskDataCollector {

		private final Set<ITask> removedQueryResults;

		private final RepositoryQuery repositoryQuery;

		private int resultCount;

		public TaskCollector(RepositoryQuery repositoryQuery) {
			this.repositoryQuery = repositoryQuery;
			this.removedQueryResults = new HashSet<ITask>(repositoryQuery.getChildren());
		}

		@Override
		public void accept(TaskData taskData) {
			ITask task = taskList.getTask(taskData.getRepositoryUrl(), taskData.getTaskId());
			if (task == null) {
				task = tasksModel.createTask(repository, taskData.getTaskId());
				task.setStale(true);
			} else {
				removedQueryResults.remove(task);
			}
			try {
				taskDataManager.putUpdatedTaskData(task, taskData, isUser());
			} catch (CoreException e) {
				StatusHandler.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN, "Failed to save task", e));
			}
			taskList.addTask(task, repositoryQuery);
			resultCount++;
		}

		@Override
		@Deprecated
		public void accept(RepositoryTaskData taskData) {
			boolean changed;
			AbstractTask task = (AbstractTask) taskList.getTask(taskData.getRepositoryUrl(), taskData.getTaskId());
			if (task == null) {
				task = ((AbstractLegacyRepositoryConnector) connector).createTask(taskData.getRepositoryUrl(),
						taskData.getTaskId(), "");
				task.setStale(true);
				changed = ((AbstractLegacyRepositoryConnector) connector).updateTaskFromTaskData(repository, task,
						taskData);
			} else {
				changed = ((AbstractLegacyRepositoryConnector) connector).updateTaskFromTaskData(repository, task,
						taskData);
				removedQueryResults.remove(task);
			}
			taskList.addTask(task, repositoryQuery);
			if (!taskData.isPartial()) {
				((TaskDataManager) taskDataManager).saveIncoming(task, taskData, isUser());
			} else if (changed && !task.isStale()
					&& task.getSynchronizationState() == SynchronizationState.SYNCHRONIZED) {
				// TODO move to synchronizationManager
				// set incoming marker for web tasks 
				task.setSynchronizationState(SynchronizationState.INCOMING);
			}
			if (isChangedTasksSynchronization() && task.isStale()) {
				tasksToBeSynchronized.add(task);
				task.setSynchronizing(true);
			}
			resultCount++;
		}

		public Set<ITask> getRemovedChildren() {
			return removedQueryResults;
		}

		public int getResultCount() {
			return resultCount;
		}

	}

	public static final String MAX_HITS_REACHED = "Max allowed number of hits returned exceeded. Some hits may not be displayed. Please narrow query scope.";

	private final AbstractRepositoryConnector connector;

	private final Set<RepositoryQuery> queries;

	private final TaskRepository repository;

	private final ITaskDataManager taskDataManager;

	private final TaskList taskList;

	private final HashSet<ITask> tasksToBeSynchronized = new HashSet<ITask>();

	private final ITasksModel tasksModel;

	public SynchronizeQueriesJob(TaskList taskList, ITaskDataManager taskDataManager, ITasksModel tasksModel,
			AbstractRepositoryConnector connector, TaskRepository repository, Set<RepositoryQuery> queries) {
		super("Synchronizing Queries (" + repository.getRepositoryLabel() + ")");
		this.taskList = taskList;
		this.taskDataManager = taskDataManager;
		this.tasksModel = tasksModel;
		this.connector = connector;
		this.repository = repository;
		this.queries = queries;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		try {
			monitor.beginTask("Processing", 20 + queries.size() * 20 + 40 + 10);

			Set<ITask> allTasks;
			if (!isFullSynchronization()) {
				allTasks = new HashSet<ITask>();
				for (RepositoryQuery query : queries) {
					allTasks.addAll(query.getChildren());
				}
			} else {
				allTasks = Collections.unmodifiableSet(taskList.getTasks(repository.getRepositoryUrl()));
			}

			MutexRule rule = new MutexRule(repository);
			try {
				Job.getJobManager().beginRule(rule, monitor);

				SynchronizationContext event = new SynchronizationContext(taskDataManager);
				event.setTaskRepository(repository);
				event.setFullSynchronization(isFullSynchronization());
				event.setTasks(allTasks);
				event.setNeedsPerformQueries(true);

//				try {
				// hook into the connector for checking for changed tasks and have the connector mark tasks that need synchronization
				if (firePreSynchronization(event, new SubProgressMonitor(monitor, 20))) {
					// synchronize queries, tasks changed within query are added to set of tasks to be synchronized
					synchronizeQueries(monitor, event);

					// for background synchronizations all changed tasks are synchronized including the ones that are not part of a query
					//if (!isUser()) {
					for (ITask task : allTasks) {
						if (task.isStale()) {
							tasksToBeSynchronized.add(task);
							((AbstractTask) task).setSynchronizing(true);
						}
					}
					//}

					// synchronize tasks that were marked by the connector
					if (!tasksToBeSynchronized.isEmpty()) {
						Policy.checkCanceled(monitor);
						monitor.subTask("Synchronizing " + tasksToBeSynchronized.size() + " changed tasks");
						synchronizeTasks(new SubProgressMonitor(monitor, 40));
					} else {
						monitor.worked(40);
					}

					// hook into the connector for synchronization time stamp management
					firePostSynchronization(event, new SubProgressMonitor(monitor, 10));
				}
//				} finally {
//					taskList.notifyElementsChanged(null);
//				}
			} finally {
				Job.getJobManager().endRule(rule);
			}
			return Status.OK_STATUS;
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		} finally {
			monitor.done();
		}
	}

	private void synchronizeQueries(IProgressMonitor monitor, ISynchronizationContext event) {
		for (RepositoryQuery repositoryQuery : queries) {
			Policy.checkCanceled(monitor);
			repositoryQuery.setStatus(null);

			monitor.subTask("Synchronizing query " + repositoryQuery.getSummary());
			synchronizeQuery(repositoryQuery, event, new SubProgressMonitor(monitor, 20));

			repositoryQuery.setSynchronizing(false);
			taskList.notifySynchronizationStateChanged(Collections.singleton(repositoryQuery));
		}
	}

	private boolean firePostSynchronization(SynchronizationContext event, IProgressMonitor monitor) {
		try {
			Policy.checkCanceled(monitor);
			monitor.subTask("Updating repository state");
			event.setChangedTasks(tasksToBeSynchronized);
			if (!isUser()) {
				monitor = Policy.backgroundMonitorFor(monitor);
			}
			connector.postSynchronization(event, monitor);
			return true;
		} catch (CoreException e) {
			updateQueryStatus(e.getStatus());
			return false;
		}
	}

	private boolean firePreSynchronization(ISynchronizationContext event, IProgressMonitor monitor) {
		try {
			Policy.checkCanceled(monitor);
			monitor.subTask("Querying repository");
			if (!isUser()) {
				monitor = Policy.backgroundMonitorFor(monitor);
			}
			connector.preSynchronization(event, monitor);
			if (!event.needsPerformQueries() && !isUser()) {
				updateQueryStatus(null);
				return false;
			}
			return true;
		} catch (CoreException e) {
			// synchronization is unlikely to succeed, inform user and exit
			updateQueryStatus(e.getStatus());
			return false;
		}
	}

	private void synchronizeQuery(RepositoryQuery repositoryQuery, ISynchronizationContext event,
			IProgressMonitor monitor) {
		TaskCollector collector = new TaskCollector(repositoryQuery);

		if (!isUser()) {
			monitor = Policy.backgroundMonitorFor(monitor);
		}
		IStatus result = connector.performQuery(repository, repositoryQuery, collector, event, monitor);
		if (result.getSeverity() == IStatus.CANCEL) {
			// do nothing
		} else if (result.isOK()) {
			if (collector.getResultCount() >= TaskDataCollector.MAX_HITS) {
				StatusHandler.log(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN, MAX_HITS_REACHED + "\n"
						+ repositoryQuery.getSummary()));
			}

			Set<ITask> removedChildren = collector.getRemovedChildren();
			if (!removedChildren.isEmpty()) {
				taskList.removeFromContainer(repositoryQuery, removedChildren);
			}

			repositoryQuery.setLastSynchronizedStamp(new SimpleDateFormat("MMM d, H:mm:ss").format(new Date()));
		} else {
			repositoryQuery.setStatus(result);
		}
	}

	private void synchronizeTasks(IProgressMonitor monitor) {
		SynchronizeTasksJob job = new SynchronizeTasksJob(taskList, taskDataManager, connector, repository,
				tasksToBeSynchronized);
		job.setUser(isUser());
		job.run(monitor);
	}

	private void updateQueryStatus(final IStatus status) {
		for (RepositoryQuery repositoryQuery : queries) {
			repositoryQuery.setStatus(status);
			repositoryQuery.setSynchronizing(false);
			taskList.notifyElementChanged(repositoryQuery);
		}
	}

}
