/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.team.ui;

import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;

/**
 * Integrates an Eclipse Team repository with Mylyn.
 * 
 * @author Gunnar Wagenknecht
 * @author Mik Kersten
 * @since 1.0
 */
public abstract class AbstractActiveChangeSetProvider {

	/**
	 * Return the change set collector that manages the active change set for the participant associated with this
	 * capability. A <code>null</code> is returned if active change sets are not supported. The default is to return
	 * <code>null</code>. This method must be overridden by subclasses that support active change sets.
	 * 
	 * @return the change set collector that manages the active change set for the participant associated with this
	 *         capability or <code>null</code> if active change sets are not supported.
	 */
	public ActiveChangeSetManager getActiveChangeSetManager() {
		return null;
	}

//	/**
//	 * @since 3.0
//	 */
//	public ActiveChangeSet createChangeSet(AbstractTask task, ActiveChangeSetManager manager) {
//		return new ContextChangeSet(task, manager);
//	}

}
