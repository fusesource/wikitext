/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core.data;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Steffen Pingel
 * @since 3.0
 */
public interface ITaskDataWorkingCopy {

	public abstract TaskData getEditsData();

	public abstract TaskData getLastReadData();

	public abstract TaskData getLocalData();

	public abstract TaskData getRepositoryData();

	public abstract boolean isSaved();

	public abstract void revert();

	public abstract void refresh(IProgressMonitor monitor) throws CoreException;

	public abstract void save(Set<TaskAttribute> edits, IProgressMonitor monitor) throws CoreException;

	public abstract String getConnectorKind();

	public abstract String getRepositoryUrl();

	public abstract String getTaskId();

}