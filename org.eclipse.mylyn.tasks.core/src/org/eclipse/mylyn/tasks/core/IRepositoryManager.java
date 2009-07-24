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

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Steffen Pingel
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRepositoryManager {

	public abstract void addListener(IRepositoryListener listener);

	public abstract void addRepository(TaskRepository repository);

	public abstract List<TaskRepository> getAllRepositories();

	public abstract Set<TaskRepository> getRepositories(String connectorKind);

	public abstract TaskRepository getRepository(String connectorKind, String repositoryUrl);

	public abstract AbstractRepositoryConnector getRepositoryConnector(String connectorKind);

	public abstract Collection<AbstractRepositoryConnector> getRepositoryConnectors();

	public abstract void removeListener(IRepositoryListener listener);

}