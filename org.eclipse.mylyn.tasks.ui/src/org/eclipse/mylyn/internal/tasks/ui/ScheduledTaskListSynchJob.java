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

package org.eclipse.mylar.internal.tasks.ui;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.TaskList;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TaskListManager;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * @author Rob Elves
 */
public class ScheduledTaskListSynchJob extends Job {

	private static final int UPDATE_ATTRIBUTES_FREQUENCY = 10;

	private static final String LABEL_TASK = "Task Repository Synchronization";

	private static final String JOB_NAME = "Synchronizing repository tasks";

	private long scheduleDelay = 1000 * 60 * 20;// 20 minutes default

	private TaskList taskList = null;

	private static long count = 0;

	private TaskListManager taskListManager;

	private List<TaskRepository> repositories = null;

	public ScheduledTaskListSynchJob(long schedule, TaskListManager taskListManager) {
		super(JOB_NAME);
		this.scheduleDelay = schedule;
		this.taskListManager = taskListManager;
		this.setSystem(true);
		this.setPriority(Job.BUILD);
	}

	public ScheduledTaskListSynchJob(TaskListManager taskListManager) {
		super(JOB_NAME);
		this.taskListManager = taskListManager;
		this.setPriority(Job.BUILD);
		this.scheduleDelay = -1;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		try {
			if (monitor == null) {
				monitor = new NullProgressMonitor();
			}

			taskList = taskListManager.getTaskList();
			if (repositories == null) {
				repositories = TasksUiPlugin.getRepositoryManager().getAllRepositories();
			}
			monitor.beginTask(LABEL_TASK, repositories.size());

			for (final TaskRepository repository : repositories) {
				if (monitor.isCanceled()) {
					scheduleDelay = -1;
					throw new OperationCanceledException();
				}
				final AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager()
						.getRepositoryConnector(repository.getKind());
				if (connector == null) {
					monitor.worked(1);
					continue;
				}

				// Occasionally update repository attributes
				if (count >= UPDATE_ATTRIBUTES_FREQUENCY) {
					Job updateJob = new Job("Updating attributes for "+repository.getUrl()) {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								connector.updateAttributes(repository, new SubProgressMonitor(monitor, 1));
							} catch (Throwable t) {
								// ignore, since we might not be connector
//								MylarStatusHandler.log(t, "Unable to update attributes for " + repository.getUrl()
//										+ "  " + t.getMessage());
							}
							return Status.OK_STATUS;
						}
					};
					//updateJob.setSystem(true);
					updateJob.setPriority(Job.LONG);
					updateJob.schedule();
				}

				Set<AbstractRepositoryQuery> queries = Collections.unmodifiableSet(taskList
						.getRepositoryQueries(repository.getUrl()));
				if (queries.size() > 0) {
					if (connector != null) {
						JobChangeAdapter jobAdapter = new JobChangeAdapter() {
							@Override
							public void done(IJobChangeEvent event) {
								TasksUiPlugin.getSynchronizationManager().synchronizeChanged(connector, repository);
							}
						};
						TasksUiPlugin.getSynchronizationManager().synchronize(connector, queries, jobAdapter,
								Job.DECORATE, 0, false);
					}
				} else {
					TasksUiPlugin.getSynchronizationManager().synchronizeChanged(connector, repository);
				}
				monitor.worked(1);
			}
		} finally {
			count = count >= UPDATE_ATTRIBUTES_FREQUENCY ? 0 : count + 1;
			if (monitor != null) {
				monitor.done();
			}
		}
		return Status.OK_STATUS;
	}

	public void setSchedule(long schedule) {
		this.scheduleDelay = schedule;
	}

	public void setRepositories(List<TaskRepository> repositories) {
		this.repositories = repositories;
	}

	/**
	 * for testing purposes
	 */
	public static long getCount() {
		return count;
	}

	/** for testing */
	public static void resetCount() {
		count = 0;
	}

	public long getScheduleDelay() {
		return scheduleDelay;
	}

}