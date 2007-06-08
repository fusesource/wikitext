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

package org.eclipse.mylyn.core;

import org.eclipse.core.runtime.IStatus;

/**
 * @author Mik Kersten
 */
public interface IStatusHandler {
 
	/**
	 * Display/log internal failure
	 * 
	 * @param status IStatus representing failure
	 * @param inform inform user via dialog, if no only status is logged
	 */
	public abstract void fail(IStatus status, boolean informUser);

	/**
	 * Display funtional error to user
	 * 
	 * @param title Title of dialog to display
	 * @param status IStatus to display
	 */
	public abstract void displayStatus(final String title, final IStatus status);
}
