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

package org.eclipse.mylyn.internal.tasks.ui;

import java.util.Collections;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.mylyn.internal.tasks.core.ITaskJobFactory;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.sync.SynchronizationJob;

/**
 * @author Steffen Pingel
 */
public class TaskListSynchronizationScheduler {

	private long interval;

	private final ITaskJobFactory jobFactory;

	private SynchronizationJob refreshJob;

	public TaskListSynchronizationScheduler(ITaskJobFactory jobFactory) {
		this.jobFactory = jobFactory;
	}

	private SynchronizationJob createRefreshJob() {
		SynchronizationJob job = jobFactory.createSynchronizeRepositoriesJob(null);
		// do not show in progress view by default
		job.setSystem(true);
		job.setUser(false);
		job.setFullSynchronization(true);
		return job;
	}

	public synchronized SynchronizationJob getRefreshJob() {
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

	public SynchronizationJob synchronize(TaskRepository repository) {
		// TODO check if a synchronization for repository is already running
		SynchronizationJob job = jobFactory.createSynchronizeRepositoriesJob(Collections.singleton(repository));
		// do not show in progress view by default
		job.setSystem(true);
		job.setUser(false);
		job.setFullSynchronization(false);
		job.schedule();
		return job;
	}

}
