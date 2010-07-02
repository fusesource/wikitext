/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.builds.ui;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.emf.common.notify.impl.NotificationImpl;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.mylyn.internal.builds.core.operations.RefreshOperation;

/**
 * @author Steffen Pingel
 */
public class BuildRefresher implements IPropertyChangeListener {

	private RefreshOperation refreshOperation;

	private long getInterval() {
		return BuildsUiPlugin.getDefault().getPreferenceStore().getLong(BuildsUiConstants.PREF_AUTO_REFRESH_INTERVAL);
	}

	public boolean isEnabled() {
		return BuildsUiPlugin.getDefault().getPreferenceStore().getBoolean(BuildsUiConstants.PREF_AUTO_REFRESH_ENABLED);
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(BuildsUiConstants.PREF_AUTO_REFRESH_ENABLED)
				|| event.getProperty().equals(BuildsUiConstants.PREF_AUTO_REFRESH_INTERVAL)) {
			reschedule();
		}
	}

	private synchronized void reschedule() {
		if (isEnabled()) {
			if (refreshOperation == null) {
				refreshOperation = new RefreshOperation(BuildsUiInternal.getModel());
				refreshOperation.setSystem(true);
				refreshOperation.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						// FIXME use model events
						BuildsUiInternal.getModel().eNotify(new NotificationImpl(0, false, true));

						reschedule();
					}
				});
			}
			refreshOperation.schedule(getInterval());
		} else {
			if (refreshOperation != null) {
				refreshOperation.cancel();
			}
		}
	}

	public synchronized void stop() {
		if (refreshOperation != null) {
			refreshOperation.cancel();
		}
	}

}