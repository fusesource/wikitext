/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.tasks.tests.connector;

import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.TaskList;

/**
 * @author Mik Kersten
 */
public class MockRepositoryQuery extends AbstractRepositoryQuery {

	public String MOCK_URL = "http://mock.repository";

	public MockRepositoryQuery(String description, TaskList taskList) {
		super(description, taskList);
		super.setUrl(MOCK_URL);
	}
	
	public MockRepositoryQuery(String description, TaskList taskList, String url) {
		super(description, taskList);
		super.setUrl(url);
	}

	@Override
	public String getRepositoryKind() {
		return "mock";
	}

}
