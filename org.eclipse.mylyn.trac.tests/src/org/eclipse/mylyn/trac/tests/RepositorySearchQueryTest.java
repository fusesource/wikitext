/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.trac.tests;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylar.context.tests.support.MylarTestUtils;
import org.eclipse.mylar.context.tests.support.MylarTestUtils.Credentials;
import org.eclipse.mylar.context.tests.support.MylarTestUtils.PrivilegeLevel;
import org.eclipse.mylar.internal.trac.core.ITracClient;
import org.eclipse.mylar.internal.trac.core.TracCorePlugin;
import org.eclipse.mylar.internal.trac.core.ITracClient.Version;
import org.eclipse.mylar.internal.trac.core.model.TracSearch;
import org.eclipse.mylar.internal.trac.ui.TracRepositoryQuery;
import org.eclipse.mylar.internal.trac.ui.search.RepositorySearchQuery;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractQueryHitCollector;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.core.TaskRepositoryManager;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.trac.tests.support.TestFixture;
import org.eclipse.mylar.trac.tests.support.XmlRpcServer.TestData;

/**
 * @author Steffen Pingel
 */
public class RepositorySearchQueryTest extends TestCase {

	private TestData data;

	private TaskRepositoryManager manager;

//	private TracRepositoryConnector connector;

	private TaskRepository repository;

	public RepositorySearchQueryTest() {
	}

	protected void setUp() throws Exception {
		super.setUp();

		data = TestFixture.init010();
		manager = TasksUiPlugin.getRepositoryManager();
		manager.clearRepositories(TasksUiPlugin.getDefault().getRepositoriesFilePath());

//		connector = (TracRepositoryConnector) manager.getRepositoryConnector(TracUiPlugin.REPOSITORY_KIND);
		TasksUiPlugin.getSynchronizationManager().setForceSyncExec(true);
	}

	protected void init(String url, Version version) {
		Credentials credentials = MylarTestUtils.readCredentials(PrivilegeLevel.USER);

		repository = new TaskRepository(TracCorePlugin.REPOSITORY_KIND, url);
		repository.setAuthenticationCredentials(credentials.username, credentials.password);
		repository.setTimeZoneId(ITracClient.TIME_ZONE);
		repository.setCharacterEncoding(ITracClient.CHARSET);
		repository.setVersion(version.name());

		manager.addRepository(repository, TasksUiPlugin.getDefault().getRepositoriesFilePath());
	}

	public void testSearch() {
		init(Constants.TEST_TRAC_096_URL, Version.TRAC_0_9);

		TracSearch search = new TracSearch();
		String queryUrl = repository.getUrl() + ITracClient.QUERY_URL + search.toUrl();
		TracRepositoryQuery query = new TracRepositoryQuery(repository.getUrl(), queryUrl, "description", null);

		final int count[] = new int[1];
		RepositorySearchQuery searchQuery = new RepositorySearchQuery(repository, query);
		searchQuery.setCollector(new AbstractQueryHitCollector(TasksUiPlugin.getTaskListManager().getTaskList()) {
			@Override
			public void addMatch(AbstractQueryHit hit) {
				assertEquals(Constants.TEST_TRAC_096_URL, hit.getRepositoryUrl());
				count[0]++;
			}
		});
		IStatus status = searchQuery.run(new NullProgressMonitor());
		assertTrue(status.isOK());
		assertEquals(data.tickets.size(), count[0]);
	}

}
