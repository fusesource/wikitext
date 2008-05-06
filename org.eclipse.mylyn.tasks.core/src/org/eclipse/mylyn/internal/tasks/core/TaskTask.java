/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core;

/**
 * @author Steffen Pingel
 */
public class TaskTask extends AbstractTask {

	private final String connectorKind;

	public TaskTask(String connectorKind, String repositoryUrl, String taskId) {
		super(repositoryUrl, taskId, "");
		this.connectorKind = connectorKind;
	}

	@Override
	public String getConnectorKind() {
		return connectorKind;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

}
