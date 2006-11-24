/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.ui.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.IParameterValues;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * Compute repository connectors type/label map
 * 
 * @author Willian Mitsuda
 */
public class TaskRepositoryParameterValues implements IParameterValues {

	public Map<String, String> getParameterValues() {
		Collection<AbstractRepositoryConnector> connectors = TasksUiPlugin.getRepositoryManager()
				.getRepositoryConnectors();
		Map<String, String> values = new HashMap<String, String>();
		for (AbstractRepositoryConnector connector : connectors) {
			values.put(connector.getRepositoryType(), connector.getLabel());
		}
		return values;
	}

}
