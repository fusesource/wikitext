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
package org.eclipse.mylar.internal.tasks.ui;

import org.eclipse.mylar.tasks.core.ITask;

/**
 * Custom filters are used so that the "Find:" filter can 'see through'
 * any filters that may have been applied.
 * 
 * @author Mik Kersten
 */
public abstract class AbstractTaskListFilter {

	public abstract boolean select(Object parent, Object element);

	public boolean shouldAlwaysShow(ITask task) {
		return task.isActive();
	}
}
