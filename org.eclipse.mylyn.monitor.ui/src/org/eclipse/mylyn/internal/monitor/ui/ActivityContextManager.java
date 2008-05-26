/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.monitor.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.context.core.InteractionContextManager;
import org.eclipse.mylyn.monitor.core.InteractionEvent;
import org.eclipse.mylyn.monitor.ui.AbstractUserActivityMonitor;
import org.eclipse.mylyn.monitor.ui.IActivityContextManager;
import org.eclipse.mylyn.monitor.ui.IUserAttentionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

/**
 * Manages the meta task-activity context.
 * 
 * @author Mik Kersten
 * @author Rob Elves
 * @since 2.0
 */
@SuppressWarnings("restriction")
public class ActivityContextManager implements IActivityContextManager {

	private static final char WORKINGSET_DELIMETER = '\u200B'; // unicode zero width space

	private final int TICK = 30 * 1000;

	private final int SHORT_TICK = 5 * 1000;

	private AbstractUserActivityMonitor userActivityMonitor;

	private final Set<IUserAttentionListener> attentionListeners = new CopyOnWriteArraySet<IUserAttentionListener>();

	private long startTime = -1;

	private int timeout;

	private final Object startTimeLock = new Object();

	private boolean wasTimedOut = true;

	private int wait = SHORT_TICK;

	private CheckActivityJob checkJob;

	private final ArrayList<AbstractUserActivityMonitor> activityMonitors;

	public static final String ACTIVITY_TIMEOUT = "org.eclipse.mylyn.monitor.ui.activity.timeout";

	public static final String ACTIVITY_TIMEOUT_ENABLED = "org.eclipse.mylyn.monitor.ui.activity.timeout.enabled";

	public ActivityContextManager(int timeout, ArrayList<AbstractUserActivityMonitor> monitors) {
		this.activityMonitors = monitors;
		this.timeout = timeout;
	}

	public void start() {
		for (AbstractUserActivityMonitor monitor : activityMonitors) {
			monitor.start();
		}
		checkJob = new CheckActivityJob();
		checkJob.setSystem(true);
		checkJob.setPriority(Job.DECORATE);
		checkJob.schedule(TICK);
	}

	public void stop() {
		for (AbstractUserActivityMonitor monitor : activityMonitors) {
			monitor.stop();
		}
		if (checkJob != null) {
			checkJob.cancel();
		}
	}

	public void addListener(IUserAttentionListener listener) {
		attentionListeners.add(listener);
	}

	public void removeListener(IUserAttentionListener listener) {
		attentionListeners.remove(listener);
	}

	private void addMonitoredActivityTime(long start, long end) {
		if (end > start) {
			ContextCorePlugin.getContextManager().processActivityMetaContextEvent(
					new InteractionEvent(InteractionEvent.Kind.ATTENTION,
							InteractionContextManager.ACTIVITY_STRUCTUREKIND_TIMING, getStructureHandle(),
							InteractionContextManager.ACTIVITY_ORIGINID_WORKBENCH, null,
							InteractionContextManager.ACTIVITY_DELTA_ADDED, 1f, new Date(start), new Date(end)));
			for (IUserAttentionListener attentionListener : attentionListeners) {
				attentionListener.userAttentionGained();
			}
		}
	}

	public void addActivityTime(String handle, long start, long end) {
		if (handle != null) {
			ContextCorePlugin.getContextManager().processActivityMetaContextEvent(
					new InteractionEvent(InteractionEvent.Kind.ATTENTION,
							InteractionContextManager.ACTIVITY_STRUCTUREKIND_TIMING, handle,
							InteractionContextManager.ACTIVITY_ORIGINID_USER, null,
							InteractionContextManager.ACTIVITY_DELTA_ADDED, 1f, new Date(start), new Date(end)));
		}
	}

	public void removeActivityTime(String handle, long start, long end) {
		if (handle != null) {
			ContextCorePlugin.getContextManager().processActivityMetaContextEvent(
					new InteractionEvent(InteractionEvent.Kind.ATTENTION,
							InteractionContextManager.ACTIVITY_STRUCTUREKIND_TIMING, handle,
							InteractionContextManager.ACTIVITY_ORIGINID_USER, null,
							InteractionContextManager.ACTIVITY_DELTA_REMOVED, 1f, new Date(start), new Date(end)));
		}
	}

	private void fireInactive() {
		for (IUserAttentionListener attentionListener : attentionListeners) {
			attentionListener.userAttentionLost();
		}
	}

	private long getLastEventTime() {
		for (AbstractUserActivityMonitor monitor : activityMonitors) {
			if (monitor.isEnabled()) {
				userActivityMonitor = monitor;
				return userActivityMonitor.getLastInteractionTime();
			}
		}

		return -1;
	}

	private long getStartTime() {
		synchronized (startTimeLock) {
			return startTime;
		}
	}

	private void setStartTime(long startTime) {
		synchronized (startTimeLock) {
			this.startTime = startTime;
		}
	}

	class CheckActivityJob extends Job {

		public CheckActivityJob() {
			super("Activity Monitor Job");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				if (Platform.isRunning()) {
					if (!MonitorUiPlugin.getDefault().getWorkbench().isClosing()) {

						long localLastEventTime = getLastEventTime();
						long localStartTime = getStartTime();
						long currentTime = System.currentTimeMillis();
						if ((currentTime - localLastEventTime) >= timeout && timeout != 0) {
							if (wasTimedOut == false) {
								fireInactive();
								// timed out
								wasTimedOut = true;
							}
							wait = SHORT_TICK;
						} else {
							if (wasTimedOut) {
								wasTimedOut = false;
								// back...
								setStartTime(localLastEventTime);
							} else {
								addMonitoredActivityTime(localStartTime, currentTime);
								setStartTime(currentTime);
							}
							wait = TICK;
						}

					}
				}
				return Status.OK_STATUS;
			} finally {
				if (Platform.isRunning()) {
					checkJob.schedule(wait);
				}
			}
		}
	}

	public void setInactivityTimeout(int inactivityTimeout) {
		timeout = inactivityTimeout;
	}

	public int getInactivityTimeout() {
		return timeout;
	}

	public String getStructureHandle() {
		if (ContextCore.getContextManager().getActiveContext().getHandleIdentifier() != null) {
			return ContextCore.getContextManager().getActiveContext().getHandleIdentifier();
		} else {
			final String[] handle = new String[1];
			handle[0] = "";
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

				public void run() {
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					if (window != null) {
						IWorkbenchPage page = window.getActivePage();
						IWorkingSet[] workingSets;
						if (page != null) {
							workingSets = page.getWorkingSets();
							for (IWorkingSet workingSet : workingSets) {
								String workingSetId = workingSet.getId();
								if (workingSetId == null) {
									workingSetId = "";
								}
								handle[0] += workingSetId + "." + workingSet.getName() + WORKINGSET_DELIMETER;
							}
						}
					}
				}
			});

			if (handle[0].length() > 0) {
				return handle[0];
			}
		}
		return InteractionContextManager.ACTIVITY_HANDLE_NONE;
	}
}
