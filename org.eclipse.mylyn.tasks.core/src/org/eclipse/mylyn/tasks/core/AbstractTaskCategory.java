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

package org.eclipse.mylyn.tasks.core;

/**
 * A container that stores tasks from any repository.
 * 
 * @author Mik Kersten
 * @since 2.0
 */
public abstract class AbstractTaskCategory extends AbstractTaskContainer {

	public AbstractTaskCategory(String handleAndDescription) {
		super(handleAndDescription);
	}

	/**
	 * Override to return true for categories that the user creates, deletes, and renames. Return false for categories
	 * that are managed
	 */
	public abstract boolean isUserDefined();
}
