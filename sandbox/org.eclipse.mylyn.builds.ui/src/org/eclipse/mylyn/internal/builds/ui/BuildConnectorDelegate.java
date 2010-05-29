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

package org.eclipse.mylyn.internal.builds.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.builds.core.IBuildServer;
import org.eclipse.mylyn.builds.core.spi.BuildConnector;
import org.eclipse.mylyn.builds.core.spi.BuildServerBehaviour;

/**
 * @author Steffen Pingel
 */
public class BuildConnectorDelegate extends BuildConnector {

	private IStatus status;

	private BuildConnector core;

	private final BuildConnectorDescriptor descriptor;

	public BuildConnectorDelegate(BuildConnectorDescriptor descriptor) {
		this.descriptor = descriptor;
		init(descriptor.connectorKind, descriptor.label);
	}

	public synchronized BuildConnector getCore() throws CoreException {
		if (core != null) {
			return core;
		}
		if (status != null) {
			throw new CoreException(status);
		}
		status = descriptor.createConnector();
		if (status != null) {
			throw new CoreException(status);
		}
		return core;
	}

	@Override
	public BuildServerBehaviour getBehaviour(IBuildServer server) throws CoreException {
		return getCore().getBehaviour(server);
	}

}
