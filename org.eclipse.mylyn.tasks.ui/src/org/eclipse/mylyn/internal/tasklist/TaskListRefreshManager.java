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

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.mylar.provisional.tasklist.MylarTaskListPlugin;

/**
 * @author Rob Elves
 */
public class TaskListRefreshManager implements IPropertyChangeListener {

	private ScheduledTaskListRefreshJob refreshJob;

	public void startRefreshJob() {
		if (refreshJob != null) {
			refreshJob.cancel();
		}

		boolean enabled = MylarTaskListPlugin.getMylarPrefs().getBoolean(
				TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_ENABLED);

		if (enabled) {
			long miliseconds = MylarTaskListPlugin.getMylarPrefs().getLong(
					TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_MILISECONDS);

			refreshJob = new ScheduledTaskListRefreshJob(miliseconds, MylarTaskListPlugin.getTaskListManager());
			refreshJob.schedule(miliseconds);
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_ENABLED)				
				|| event.getProperty().equals(TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_MILISECONDS)) {
			startRefreshJob();
		}
	}

}
