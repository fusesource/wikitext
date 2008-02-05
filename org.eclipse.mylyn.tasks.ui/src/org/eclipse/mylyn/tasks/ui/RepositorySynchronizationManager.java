/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.ui;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.internal.tasks.core.TaskDataManager;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskList;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.AbstractTask.RepositoryTaskSyncState;
import org.eclipse.ui.PlatformUI;

/**
 * Encapsulates synchronization policy.
 * 
 * NOTE: likely to change for 3.0
 * 
 * @author Mik Kersten
 * @author Rob Elves
 */
public final class RepositorySynchronizationManager {

	private final MutexRule taskRule = new MutexRule();

	private final MutexRule queryRule = new MutexRule();

	protected boolean forceSyncExecForTesting = false;

	/**
	 * Synchronize a single task. Note that if you have a collection of tasks to synchronize with this connector then
	 * you should call synchronize(Set<Set<AbstractTask> repositoryTasks, ...)
	 * 
	 * @param listener
	 *            can be null
	 */
	public final Job synchronize(AbstractRepositoryConnector connector, AbstractTask repositoryTask,
			boolean forceSynch, IJobChangeListener listener) {
		Set<AbstractTask> toSync = new HashSet<AbstractTask>();
		toSync.add(repositoryTask);
		return synchronize(connector, toSync, forceSynch, listener);
	}

	/**
	 * @param listener
	 *            can be null
	 */
	public final Job synchronize(AbstractRepositoryConnector connector, Set<AbstractTask> repositoryTasks,
			boolean forceSynch, final IJobChangeListener listener) {

		final SynchronizeTaskJob synchronizeJob = new SynchronizeTaskJob(connector, repositoryTasks);
		synchronizeJob.setForced(forceSynch);
		synchronizeJob.setPriority(Job.DECORATE);
		synchronizeJob.setRule(taskRule);
		if (listener != null) {
			synchronizeJob.addJobChangeListener(listener);
		}
		for (AbstractTask repositoryTask : repositoryTasks) {
			repositoryTask.setSynchronizing(true);
		}
		if (!forceSyncExecForTesting) {
			synchronizeJob.schedule();
		} else {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					synchronizeJob.run(new NullProgressMonitor());
					if (listener != null) {
						listener.done(null);
					}
				}
			});
		}
		return synchronizeJob;

	}

	/**
	 * For synchronizing a single query. Use synchronize(Set, IJobChangeListener) if synchronizing multiple queries at a
	 * time.
	 */
	public final Job synchronize(AbstractRepositoryConnector connector, final AbstractRepositoryQuery repositoryQuery,
			IJobChangeListener listener, boolean forceSync) {
		TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(
				repositoryQuery.getRepositoryKind(), repositoryQuery.getRepositoryUrl());
		return synchronize(connector, repository, Collections.singleton(repositoryQuery), listener, Job.LONG, 0,
				forceSync);
	}

	public final Job synchronize(AbstractRepositoryConnector connector, TaskRepository repository,
			final Set<AbstractRepositoryQuery> repositoryQueries, final IJobChangeListener listener, int priority,
			long delay, boolean userForcedSync) {
		return synchronize(connector, repository, repositoryQueries, listener, priority, delay, userForcedSync, true);
	}

	/**
	 * @param fullSynchronization
	 *            synchronize all changed tasks for <code>repository</code>
	 * @since 2.2
	 */
	public final Job synchronize(AbstractRepositoryConnector connector, TaskRepository repository,
			final Set<AbstractRepositoryQuery> repositoryQueries, final IJobChangeListener listener, int priority,
			long delay, boolean userForcedSync, boolean fullSynchronization) {
		TaskList taskList = TasksUiPlugin.getTaskListManager().getTaskList();
		for (AbstractRepositoryQuery repositoryQuery : repositoryQueries) {
			repositoryQuery.setSynchronizing(true);
			// TasksUiPlugin.getTaskListManager().getTaskList().notifyContainerUpdated(repositoryQuery);
		}
		taskList.notifyContainersUpdated(repositoryQueries);

		final SynchronizeQueryJob job = new SynchronizeQueryJob(connector, repository, repositoryQueries, taskList);
		job.setSynchronizeChangedTasks(true);
		job.setFullSynchronization(fullSynchronization);
		job.setForced(userForcedSync);
		if (listener != null) {
			job.addJobChangeListener(listener);
		}
		job.setRule(queryRule);
		job.setPriority(priority);
		if (!forceSyncExecForTesting) {
			job.schedule(delay);
		} else {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					job.run(new NullProgressMonitor());
					if (listener != null) {
						listener.done(null);
					}
				}
			});
		}
		return job;
	}

	/**
	 * Synchronizes only those tasks that have changed since the last time the given repository was synchronized. Calls
	 * to this method update TaskRepository.syncTime.
	 * 
	 * API-3.0 remove since this method is not used
	 */
	public final void synchronizeChanged(final AbstractRepositoryConnector connector, final TaskRepository repository) {
		// Method left here for completeness. Currently unused since ScheduledTaskListSynchJob calls SynchronizeQueriesJob
		// which synchronizes all changed if unforced (background). 
		Set<AbstractRepositoryQuery> emptySet = Collections.emptySet();
		synchronize(connector, repository, emptySet, null, Job.LONG, 0, false);
	}

	/**
	 * @param repositoryTask
	 *            task that changed
	 * @param modifiedAttributes
	 *            attributes that have changed during edit session
	 */
	public synchronized void saveOutgoing(AbstractTask repositoryTask, Set<RepositoryTaskAttribute> modifiedAttributes) {
		repositoryTask.setSynchronizationState(RepositoryTaskSyncState.OUTGOING);
		TasksUiPlugin.getTaskDataManager().saveEdits(repositoryTask.getRepositoryUrl(), repositoryTask.getTaskId(),
				Collections.unmodifiableSet(modifiedAttributes));
		TasksUiPlugin.getTaskListManager().getTaskList().notifyTaskChanged(repositoryTask, false);
	}

	/**
	 * Saves incoming data and updates task sync state appropriately
	 * 
	 * @return true if call results in change of sync state
	 */
	public synchronized boolean saveIncoming(final AbstractTask repositoryTask, final RepositoryTaskData newTaskData,
			boolean forceSync) {
		final RepositoryTaskSyncState startState = repositoryTask.getSynchronizationState();
		RepositoryTaskSyncState status = repositoryTask.getSynchronizationState();

		if (newTaskData == null) {
			StatusHandler.log(new Status(IStatus.WARNING, TasksUiPlugin.ID_PLUGIN, 
					"Download of " + repositoryTask.getSummary() + " from "
					+ repositoryTask.getRepositoryUrl() + " failed."));
			return false;
		}

		RepositoryTaskData previousTaskData = TasksUiPlugin.getTaskDataManager().getNewTaskData(
				repositoryTask.getRepositoryUrl(), repositoryTask.getTaskId());

		if (repositoryTask.isSubmitting()) {
			status = RepositoryTaskSyncState.SYNCHRONIZED;
			repositoryTask.setSubmitting(false);
			TaskDataManager dataManager = TasksUiPlugin.getTaskDataManager();
			dataManager.discardEdits(repositoryTask.getRepositoryUrl(), repositoryTask.getTaskId());

			TasksUiPlugin.getTaskDataManager().setNewTaskData(newTaskData);
			/**
			 * If we set both so we don't see our own changes
			 * 
			 * @see RepositorySynchronizationManager.setTaskRead(AbstractTask, boolean)
			 */
			// TasksUiPlugin.getTaskDataManager().setOldTaskData(repositoryTask.getHandleIdentifier(),
			// newTaskData);
		} else {

			switch (status) {
			case OUTGOING:
				if (checkHasIncoming(repositoryTask, newTaskData)) {
					status = RepositoryTaskSyncState.CONFLICT;
				}
				TasksUiPlugin.getTaskDataManager().setNewTaskData(newTaskData);
				break;

			case CONFLICT:
				// fall through to INCOMING (conflict implies incoming)
			case INCOMING:
				// only most recent incoming will be displayed if two
				// sequential incoming's /conflicts happen

				TasksUiPlugin.getTaskDataManager().setNewTaskData(newTaskData);
				break;
			case SYNCHRONIZED:
				boolean hasIncoming = checkHasIncoming(repositoryTask, newTaskData);
				if (hasIncoming) {
					status = RepositoryTaskSyncState.INCOMING;
					repositoryTask.setNotified(false);
				}
				if (hasIncoming || previousTaskData == null || forceSync) {
					TasksUiPlugin.getTaskDataManager().setNewTaskData(newTaskData);
				}
				break;
			}
		}
		repositoryTask.setSynchronizationState(status);
		return startState != repositoryTask.getSynchronizationState();
	}

	public void saveOffline(AbstractTask task, RepositoryTaskData taskData) {
		TasksUiPlugin.getTaskDataManager().setNewTaskData(taskData);
	}

	/** public for testing purposes */
	public boolean checkHasIncoming(AbstractTask repositoryTask, RepositoryTaskData newData) {
		if (repositoryTask.getSynchronizationState() == RepositoryTaskSyncState.INCOMING) {
			return true;
		}

		String lastModified = repositoryTask.getLastReadTimeStamp();
		RepositoryTaskAttribute modifiedDateAttribute = newData.getAttribute(RepositoryTaskAttribute.DATE_MODIFIED);
		if (lastModified != null && modifiedDateAttribute != null && modifiedDateAttribute.getValue() != null) {
			if (lastModified.trim().compareTo(modifiedDateAttribute.getValue().trim()) == 0) {
				// Only set to synchronized state if not in incoming state.
				// Case of incoming->sync handled by markRead upon opening
				// or a forced synchronization on the task only.
				return false;
			}

			Date modifiedDate = newData.getAttributeFactory().getDateForAttributeType(
					RepositoryTaskAttribute.DATE_MODIFIED, modifiedDateAttribute.getValue());
			Date lastModifiedDate = newData.getAttributeFactory().getDateForAttributeType(
					RepositoryTaskAttribute.DATE_MODIFIED, lastModified);
			if (modifiedDate != null && lastModifiedDate != null && modifiedDate.equals(lastModifiedDate)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param repositoryTask -
	 *            repository task to mark as read or unread
	 * @param read -
	 *            true to mark as read, false to mark as unread
	 */
	public void setTaskRead(AbstractTask repositoryTask, boolean read) {
		TaskDataManager dataManager = TasksUiPlugin.getTaskDataManager();
		RepositoryTaskData taskData = TasksUiPlugin.getTaskDataManager().getNewTaskData(
				repositoryTask.getRepositoryUrl(), repositoryTask.getTaskId());

		if (read && repositoryTask.getSynchronizationState().equals(RepositoryTaskSyncState.INCOMING)) {
			if (taskData != null && taskData.getLastModified() != null) {
				repositoryTask.setLastReadTimeStamp(taskData.getLastModified());
				dataManager.setOldTaskData(taskData);
			}
			repositoryTask.setSynchronizationState(RepositoryTaskSyncState.SYNCHRONIZED);
			TasksUiPlugin.getTaskListManager().getTaskList().notifyTaskChanged(repositoryTask, false);
		} else if (read && repositoryTask.getSynchronizationState().equals(RepositoryTaskSyncState.CONFLICT)) {
			if (taskData != null && taskData.getLastModified() != null) {
				repositoryTask.setLastReadTimeStamp(taskData.getLastModified());
			}
			repositoryTask.setSynchronizationState(RepositoryTaskSyncState.OUTGOING);
			TasksUiPlugin.getTaskListManager().getTaskList().notifyTaskChanged(repositoryTask, false);
		} else if (read && repositoryTask.getSynchronizationState().equals(RepositoryTaskSyncState.SYNCHRONIZED)) {
			if (taskData != null && taskData.getLastModified() != null) {
				repositoryTask.setLastReadTimeStamp(taskData.getLastModified());
				// By setting old every time (and not setting upon submission)
				// we see our changes
				// If condition is enabled and we save old in OUTGOING handler
				// our own changes
				// will not be displayed after submission.
				// if
				// (dataManager.getOldTaskData(repositoryTask.getHandleIdentifier())
				// == null) {
				dataManager.setOldTaskData(taskData);
				// }
			}
//			else if (repositoryTask.getLastReadTimeStamp() == null && repositoryTask.isLocal()) {
//				// fall back for cases where the stamp is missing, set bogus date
//				repositoryTask.setLastReadTimeStamp(LocalTask.SYNC_DATE_NOW);
//			}

		} else if (!read && repositoryTask.getSynchronizationState().equals(RepositoryTaskSyncState.SYNCHRONIZED)) {
			repositoryTask.setSynchronizationState(RepositoryTaskSyncState.INCOMING);
			TasksUiPlugin.getTaskListManager().getTaskList().notifyTaskChanged(repositoryTask, false);
		}

		// for connectors that don't support task data set read date to now (bug#204741)
		if (read && taskData == null && repositoryTask.isLocal()) {
			repositoryTask.setLastReadTimeStamp((new Date()).toString());
		}
	}

	public void discardOutgoing(AbstractTask repositoryTask) {
		TaskDataManager dataManager = TasksUiPlugin.getTaskDataManager();
		dataManager.discardEdits(repositoryTask.getRepositoryUrl(), repositoryTask.getTaskId());
		repositoryTask.setSynchronizationState(RepositoryTaskSyncState.SYNCHRONIZED);

		TasksUiPlugin.getTaskListManager().getTaskList().notifyTaskChanged(repositoryTask, true);
	}

	/**
	 * For testing
	 */
	public final void setForceSyncExec(boolean forceSyncExec) {
		this.forceSyncExecForTesting = forceSyncExec;
	}

	/**
	 * For testing
	 */
	public final boolean isForcedSyncExec() {
		return this.forceSyncExecForTesting;
	}

	private static class MutexRule implements ISchedulingRule {
		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}

		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}
	}

	/*
	private static class RepositoryMutexRule implements ISchedulingRule {

		private TaskRepository repository = null;

		public RepositoryMutexRule(TaskRepository repository) {
			this.repository = repository;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			if (rule instanceof RepositoryMutexRule) {
				return repository.equals(((RepositoryMutexRule) rule).getRepository());
			} else {
				return false;
			}
		}

		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		public TaskRepository getRepository() {
			return repository;
		}
	}
	*/

}
