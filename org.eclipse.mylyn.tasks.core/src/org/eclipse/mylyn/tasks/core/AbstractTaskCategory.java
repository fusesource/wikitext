/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	@Override
	public boolean equals(Object object) {
		return super.equals(object);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
