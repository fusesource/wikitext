/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.ui.views;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * @author Mik Kersten
 */
public class AdaptiveRefreshPolicy {

	private int refreshThreshold = 2000;

	private Set<IFilteredTreeListener> listeners = new HashSet<IFilteredTreeListener>();
	
	private Text filterText = null;
	
	private Job refreshJob;
	
	/**
	 * @param refreshJob
	 * @param filteredTree	can be null
	 */
	public AdaptiveRefreshPolicy(Job refreshJob, Text filterText) {
		this.refreshJob = refreshJob;
		this.filterText = filterText;
		refreshJob.addJobChangeListener(REFRESH_JOB_LISTENER);
	}
	
	public void dispose() {
		if (refreshJob != null) {
			refreshJob.removeJobChangeListener(REFRESH_JOB_LISTENER);
		}	
	}
	
	private final IJobChangeListener REFRESH_JOB_LISTENER = new IJobChangeListener() {

		public void aboutToRun(IJobChangeEvent event) {
			// ignore
		}

		public void awake(IJobChangeEvent event) {
			// ignore
		}

		public void done(IJobChangeEvent event) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					for (IFilteredTreeListener listener : listeners) {
						listener.filterTextChanged(filterText.getText());
					}
				}
			});
		}

		public void running(IJobChangeEvent event) {
			// ignore
		}

		public void scheduled(IJobChangeEvent event) {
			// ignore
		}

		public void sleeping(IJobChangeEvent event) {
			// ignore
		}
	};
	
	public void textChanged(String text) {
		if (refreshJob == null)
			return;
		refreshJob.cancel();
		int refreshDelay = 0;
		int textLength = text.length();
		if (textLength > 0) {
			refreshDelay = (int)(refreshThreshold /(textLength*0.6));
			System.err.println(">>> " + refreshDelay);
		}
		refreshJob.addJobChangeListener(REFRESH_JOB_LISTENER);
		refreshJob.schedule(refreshDelay);
	}
	
	public void addListener(IFilteredTreeListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IFilteredTreeListener listener) {
		listeners.remove(listener);
	}

	public void setRefreshDelay(int refreshDelay) {
		this.refreshThreshold = refreshDelay;
	}
	
}
