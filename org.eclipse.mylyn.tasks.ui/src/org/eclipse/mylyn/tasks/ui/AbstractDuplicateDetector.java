/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.ui;

import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.search.SearchHitCollector;

/**
 * Extend to provide task duplicate detection facilities to the task editor (e.g. Java stack trace matching).
 * 
 * @author Gail Murphy
 * @since 2.0
 */
public abstract class AbstractDuplicateDetector {

	protected String name;

	protected String kind;

	public abstract SearchHitCollector getSearchHitCollector(TaskRepository repository, RepositoryTaskData taskData);

	public void setName(String name) {
		this.name = name;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getName() {
		return this.name;
	}

	public String getKind() {
		return this.kind;
	}

}
