/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core;

import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskCategory;
import org.eclipse.mylyn.tasks.core.AbstractTask.PriorityLevel;

/**
 * Holds orphaned or uncategorized tasks for a given repository
 * 
 * @author Rob Elves
 */
public class OrphanedTasksContainer extends AbstractTaskCategory {

	public static final String LABEL = "Uncategorized";

	public static final String HANDLE = "orphans";

	private String repositoryUrl;

	private String connectorKind;

	public OrphanedTasksContainer(String connectorKind, String repositoryUrl) {
		super(repositoryUrl + "-" + HANDLE);
		this.repositoryUrl = repositoryUrl;
		this.connectorKind = connectorKind;
	}

	@Override
	public String getPriority() {
		return PriorityLevel.P1.toString();
	}

	@Override
	public String getSummary() {
		return "Archive: " + getRepositoryUrl();
	}

	@Override
	public boolean isUserDefined() {
		return false;
	}

	public String getRepositoryUrl() {
		return repositoryUrl;
	}

	public String getConnectorKind() {
		return connectorKind;
	}

	/**
	 * setting will also refactor handle
	 */
	public void setRepositoryUrl(String repositoryUrl) {
		this.repositoryUrl = repositoryUrl;
		this.setHandleIdentifier(repositoryUrl + "-" + HANDLE);
	}

	@Override
	public boolean contains(String handle) {
		for (AbstractTask child : getChildrenInternal()) {
			if (child.getHandleIdentifier().equals(handle)) {
				return true;
			}
		}
		return false;
	}
}
