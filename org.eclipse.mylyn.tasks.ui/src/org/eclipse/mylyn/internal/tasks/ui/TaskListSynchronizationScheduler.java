/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.mylyn.tasks.core.SynchronizeJob;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.ITasksJobFactory;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;

/**
 * @author Steffen Pingel
 */
public class TaskListSynchronizationScheduler {

	private long interval;

	private final ITasksJobFactory jobFactory;

	private SynchronizeJob refreshJob;

	public TaskListSynchronizationScheduler(ITasksJobFactory jobFactory) {
		this.jobFactory = jobFactory;
	}

	private SynchronizeJob createRefreshJob() {
		Set<TaskRepository> repositories = new HashSet<TaskRepository>(TasksUiPlugin.getRepositoryManager()
				.getAllRepositories());
		SynchronizeJob job = jobFactory.createSynchronizeRepositoriesJob(repositories);
		job.setUser(false);
		job.setFullSynchronization(true);
		return job;
	}

	public synchronized SynchronizeJob getRefreshJob() {
		return refreshJob;
	}

	private synchronized void reschedule() {
		if (this.interval != 0) {
			refreshJob.schedule(interval);
		}
	}

	public synchronized void setInterval(long interval) {
		setInterval(interval, interval);
	}

	public synchronized void setInterval(long delay, long interval) {
		if (this.interval != interval) {
			this.interval = interval;
			if (refreshJob != null) {
				refreshJob.cancel();
				refreshJob = null;
			}

			if (interval > 0) {
				refreshJob = createRefreshJob();
				refreshJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						reschedule();
					}

				});
				refreshJob.schedule(delay);
			}
		}
	}

	public SynchronizeJob synchronize(TaskRepository repository) {
		// TODO check if a synchronization for repository is already running
		SynchronizeJob job = jobFactory.createSynchronizeRepositoriesJob(Collections.singleton(repository));
		job.setUser(false);
		job.setFullSynchronization(false);
		job.schedule();
		return job;
	}

}
