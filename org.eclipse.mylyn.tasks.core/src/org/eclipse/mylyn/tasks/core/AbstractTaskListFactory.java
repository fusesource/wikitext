/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core;

import java.util.Collections;
import java.util.Set;

import org.w3c.dom.Element;

/**
 * Responsible for storing and creating task list elements.
 * 
 * When overriding these methods be sure not to perform network access since the Task List is read and written
 * frequently.
 * 
 * @author Mik Kersten
 * @author Ken Sueda
 * @since 2.0
 */
public abstract class AbstractTaskListFactory {

	public static final String KEY_QUERY = "Query";

	public static final String KEY_QUERY_STRING = "QueryString";

	public static final String KEY_TASK = "Task";

	public abstract boolean canCreate(AbstractTask task);

	public boolean canCreate(AbstractRepositoryQuery query) {
		return false;
	}

	public AbstractRepositoryQuery createQuery(String repositoryUrl, String queryString, String label, Element element) {
		return null;
	}

	public abstract AbstractTask createTask(String repositoryUrl, String taskId, String label, Element element);

	public String getQueryElementName(AbstractRepositoryQuery query) {
		return "";
	}

	public Set<String> getQueryElementNames() {
		return Collections.emptySet();
	}

	public abstract String getTaskElementName();

	public void setAdditionalAttributes(AbstractRepositoryQuery query, Element node) {
		// ignore
	}

	public void setAdditionalAttributes(AbstractTask task, Element element) {
		// ignore
	}
}