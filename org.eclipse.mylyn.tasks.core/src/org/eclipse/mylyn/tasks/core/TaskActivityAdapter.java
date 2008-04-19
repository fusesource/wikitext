/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core;

/**
 * @author Steffen Pingel
 * @since 3.0
 */
public class TaskActivityAdapter implements ITaskActivityListener {

	public void activityReset() {
	}

	/**
	 * Warning: This is called frequently (i.e. every 15s) Implementers are responsible for launching jobs for long
	 * running activity.
	 */
	public void elapsedTimeUpdated(AbstractTask task, long newElapsedTime) {
	}

}
