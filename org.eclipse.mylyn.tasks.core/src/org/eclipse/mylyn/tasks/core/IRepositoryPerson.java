/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core;

/**
 * @author Steffen Pingel
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRepositoryPerson {

	/**
	 * @since 3.0
	 */
	public abstract String getConnectorKind();

	/**
	 * @since 3.0
	 */
	public abstract String getName();

	/**
	 * @since 3.0
	 */
	public abstract String getPersonId();

	/**
	 * @since 3.0
	 */
	public abstract String getRepositoryUrl();

	/**
	 * @since 3.0
	 */
	public abstract TaskRepository getTaskRepository();

	/**
	 * @since 3.0
	 */
	public abstract void setName(String name);

}