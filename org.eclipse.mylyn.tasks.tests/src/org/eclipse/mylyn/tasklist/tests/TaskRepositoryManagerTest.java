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

package org.eclipse.mylar.tasklist.tests;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.mylar.internal.bugzilla.core.BugzillaPlugin;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.provisional.tasklist.AbstractRepositoryConnector;
import org.eclipse.mylar.provisional.tasklist.MylarTaskListPlugin;
import org.eclipse.mylar.provisional.tasklist.TaskRepositoryManager;
import org.eclipse.mylar.tasklist.tests.mockconnector.MockRepositoryConnector;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.TaskRepository;

/**
 * @author Mik Kersten
 * @author Rob Elves
 */
public class TaskRepositoryManagerTest extends TestCase {

	private static final String DEFAULT_KIND = BugzillaPlugin.REPOSITORY_KIND;

	private static final String DEFAULT_URL = "http://eclipse.org";

	private static final String ANOTHER_URL = "http://codehaus.org";

	private TaskRepositoryManager manager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		manager = MylarTaskListPlugin.getRepositoryManager();
		assertNotNull(manager);
		manager.clearRepositories();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (manager != null) {
			manager.clearRepositories();
		}
	}

	public void testHandles() {
		String url = IBugzillaConstants.ECLIPSE_BUGZILLA_URL;
		String id = "123";
		String handle = AbstractRepositoryTask.getHandle(url, id);
		assertEquals(url, AbstractRepositoryTask.getRepositoryUrl(handle));
		assertEquals(id, AbstractRepositoryTask.getTaskId(handle));
//		assertEquals(123, AbstractRepositoryTask.getTaskIdAsInt(handle));
	}

	public void testMultipleNotAdded() throws MalformedURLException {
		TaskRepository repository = new TaskRepository(DEFAULT_KIND, DEFAULT_URL);
		manager.addRepository(repository);
		TaskRepository repository2 = new TaskRepository(DEFAULT_KIND, DEFAULT_URL);
		manager.addRepository(repository2);
		assertEquals(1, manager.getAllRepositories().size());
	}

	public void testGet() throws MalformedURLException {
		assertEquals("", MylarTaskListPlugin.getDefault().getPreferenceStore().getString(TaskRepositoryManager.PREF_REPOSITORIES));

		TaskRepository repository = new TaskRepository(DEFAULT_KIND, DEFAULT_URL);
		manager.addRepository(repository);
		assertEquals(repository, manager.getRepository(DEFAULT_KIND, DEFAULT_URL));
		assertNull(manager.getRepository(DEFAULT_KIND, "foo"));
		assertNull(manager.getRepository("foo", DEFAULT_URL));
	}

	public void testConnectorAddition() {
		AbstractRepositoryConnector connector = new MockRepositoryConnector();
		manager.addRepositoryConnector(connector);
		assertNotNull(manager.getRepositoryConnector(connector.getRepositoryType()));
	}

	public void testRepositoryPersistance() throws MalformedURLException {
		// assertEquals("",
		// MylarTaskListPlugin.getMylarCorePrefs().getString(TaskRepositoryManager.PREF_REPOSITORIES));

		TaskRepository repository1 = new TaskRepository("bugzilla", "http://bugzilla");
		TaskRepository repository2 = new TaskRepository("jira", "http://jira");
		manager.addRepository(repository1);
		manager.addRepository(repository2);

		// assertNotNull(MylarTaskListPlugin.getMylarCorePrefs().getString(TaskRepositoryManager.PREF_REPOSITORIES));

		List<TaskRepository> repositoryList = new ArrayList<TaskRepository>();
		repositoryList.add(repository2);
		repositoryList.add(repository1);
		manager.readRepositories();
		// NOTE: different conditions for running with and without the JIRA
		// Connector
		if (manager.getRepositoryConnectors().size() > 1) {
			assertTrue(manager.getAllRepositories().contains(repository1));
			assertTrue(manager.getAllRepositories().contains(repository2));
			// assertEquals(repositoryList, manager.getAllRepositories());
		} else {
			// TODO there is something wrong with this
			assertEquals("all: " + manager.getAllRepositories(), 1, manager.getAllRepositories().size());
		}
	}

	public void testRepositoryAttributePersistance() throws MalformedURLException {
		assertEquals("", MylarTaskListPlugin.getDefault().getPreferenceStore().getString(TaskRepositoryManager.PREF_REPOSITORIES));

		String version = "123";
		String encoding = "UTF-16";
		String fakeTimeZone = "nowhere";
		Date now = new Date();
		String dateString = now.toString();

		TaskRepository repository1 = new TaskRepository("bugzilla", "http://bugzilla");
		repository1.setVersion(version);
		repository1.setCharacterEncoding(encoding);
		repository1.setTimeZoneId(fakeTimeZone);
		repository1.setSyncTimeStamp(dateString);
		manager.addRepository(repository1);

		manager.readRepositories();
		TaskRepository temp = manager.getRepository(repository1.getKind(), repository1.getUrl());
		assertNotNull(temp);
		assertEquals(version, temp.getVersion());
		assertEquals(encoding, temp.getCharacterEncoding());
		assertEquals(fakeTimeZone, temp.getTimeZoneId());
		assertEquals(dateString, temp.getSyncTimeStamp());

	}

	public void testRepositoryPersistanceAfterDelete() throws MalformedURLException {

		TaskRepository repository = new TaskRepository(DEFAULT_KIND, DEFAULT_URL);
		manager.addRepository(repository);
		assertNotNull(manager.getRepository(repository.getKind(), repository.getUrl()));
	
		TaskRepository repository2 = new TaskRepository(DEFAULT_KIND, ANOTHER_URL);
		manager.addRepository(repository2);
		assertNotNull(manager.getRepository(repository2.getKind(), repository2.getUrl()));

		manager.removeRepository(repository2);

		assertNull(manager.getRepository(repository2.getKind(), repository2.getUrl()));
	}

	public void testRepositoryWithUnnownUrlHandler() {
		TaskRepository repository = new TaskRepository("eclipse.technology.mylar",
				"nntp://news.eclipse.org/eclipse.technology.mylar");

		repository.setAuthenticationCredentials("testUser", "testPassword");

		assertEquals("testUser", repository.getUserName());
		assertEquals("testPassword", repository.getPassword());
	}

	public void testRepositoryWithCustomAttributes() throws Exception {
		TaskRepository repository = new TaskRepository("web", "http://jroller.com/page/eu");
		repository.setProperty("owner", "euxx");
		manager.addRepository(repository);

		manager.readRepositories();

		TaskRepository temp = manager.getRepository(repository.getKind(), repository.getUrl());
		assertNotNull(temp);
		assertEquals("euxx", temp.getProperty("owner"));
	}
}
