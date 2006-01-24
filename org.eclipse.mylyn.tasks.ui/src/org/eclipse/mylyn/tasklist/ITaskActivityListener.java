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
/*
 * Created on Jan 13, 2005
 */
package org.eclipse.mylar.tasklist;

import java.util.List;

/**
 * @author Mik Kersten
 */
public interface ITaskActivityListener {

	public abstract void taskActivated(ITask task);

	public abstract void tasksActivated(List<ITask> tasks);

	public abstract void taskDeactivated(ITask task);

	public abstract void taskChanged(ITask task);

	public abstract void tasklistRead();

	public abstract void taskListModified();
}
