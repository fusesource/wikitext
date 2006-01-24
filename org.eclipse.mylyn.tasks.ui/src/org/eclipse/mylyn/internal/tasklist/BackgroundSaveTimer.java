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
package org.eclipse.mylar.internal.tasklist;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylar.internal.core.util.ITimerThreadListener;
import org.eclipse.mylar.internal.core.util.MylarStatusHandler;
import org.eclipse.mylar.internal.core.util.TimerThread;

/**
 * Timer that periodically runs saveRequested() on its client as a job
 * 
 * @author Wesley Coelho
 */
public class BackgroundSaveTimer implements ITimerThreadListener {

	private final static int DEFAULT_SAVE_INTERVAL = 1 * 60 * 1000;

	private int saveInterval = DEFAULT_SAVE_INTERVAL;

	private IBackgroundSaveListener listener = null;

	private TimerThread timer = null;

	private boolean forceSyncExec = false;

	public BackgroundSaveTimer(IBackgroundSaveListener listener) {
		this.listener = listener;
		timer = new TimerThread(saveInterval / 1000); // This constructor
		// wants seconds
		timer.addListener(this);
	}

	public void start() {
		timer.start();
	}

	public void stop() {
		timer.kill();
	}

	public void setSaveIntervalMillis(int saveIntervalMillis) {
		this.saveInterval = saveIntervalMillis;
		timer.setTimeoutMillis(saveIntervalMillis);
	}

	public int getSaveIntervalMillis() {
		return saveInterval;
	}

	/**
	 * For testing
	 */
	public void setForceSyncExec(boolean forceSyncExec) {
		this.forceSyncExec = forceSyncExec;
	}

	/**
	 * Called by the ActivityTimerThread Calls save in a new job
	 */
	public void fireTimedOut() {
		try {
			if (!forceSyncExec) {
				final SaveJob job = new SaveJob("Saving Task Data", listener);
				job.schedule();
			} else {
				listener.saveRequested();
			}
		} catch (RuntimeException e) {
			MylarStatusHandler.log("Could not schedule save job", this);
		}
	}

	/** Job that makes the save call */
	private class SaveJob extends Job {
		private IBackgroundSaveListener listener = null;

		public SaveJob(String name, IBackgroundSaveListener listener) {
			super(name);
			this.listener = listener;
		}

		protected IStatus run(IProgressMonitor monitor) {
			listener.saveRequested();
			return Status.OK_STATUS;
		}
	}

	public void intervalElapsed() {
		// ignore
	}

}
