/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core;

import org.eclipse.mylyn.internal.tasks.core.ScheduledTaskContainer;

/**
 * Notified of task activity changes.
 * 
 * @author Mik Kersten
 * @author Rob Elves
 * @since 2.0
 */
public interface ITaskActivityListener {

	public abstract void taskActivated(AbstractTask task);

	public abstract void taskDeactivated(AbstractTask task);

	public abstract void taskListRead();

	/**
	 * @param week
	 *            can be null
	 */
	public abstract void activityChanged(ScheduledTaskContainer week);

}
