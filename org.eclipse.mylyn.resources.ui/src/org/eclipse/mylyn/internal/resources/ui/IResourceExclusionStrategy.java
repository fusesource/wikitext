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

package org.eclipse.mylyn.internal.resources.ui;

import org.eclipse.core.resources.IResource;

/**
 * @author Shawn Minto
 */
public interface IResourceExclusionStrategy {

	void init();

	void dispose();

	/**
	 * Called to indicate that the strategy should refresh its state
	 */
	void update();

	/**
	 * If an implementation returns <code>true</code> for an IFolder, all children (files and folders) will be excluded
	 * from the context as well
	 * 
	 * @return true if the resource should not be added to the active task context
	 */
	boolean isExcluded(IResource resource);

}
