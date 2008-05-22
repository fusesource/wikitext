/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.monitor.ui;


/**
 * Extend to monitor periods of user activity and inactivity.
 * 
 * @author Mik Kersten
 * @author Rob Elves
 * @since 2.0
 */
public abstract class AbstractUserActivityMonitor {

	private long lastEventTimeStamp = -1;

	public long getLastInteractionTime() {
		synchronized (this) {
			return lastEventTimeStamp;
		}
	}

	public void setLastEventTime(long lastEventTime) {
		synchronized (this) {
			lastEventTimeStamp = lastEventTime;
		}
	}

	public abstract void start();

	public abstract void stop();

	/**
	 * @return false if monitor unable to run (i.e. startup failures of any kind)
	 */
	public boolean isEnabled() {
		return true;
	}
}
