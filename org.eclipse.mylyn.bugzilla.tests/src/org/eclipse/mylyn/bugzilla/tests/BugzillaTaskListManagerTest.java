/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.bugzilla.tests;

import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryQuery;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaTask;
import org.eclipse.mylyn.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylyn.internal.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.TaskList;
import org.eclipse.mylyn.internal.tasks.ui.TaskListManager;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;

/**
 * @author Mik Kersten
 */
public class BugzillaTaskListManagerTest extends TestCase {

	private TaskListManager manager;

	private TaskRepository repository;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		manager = TasksUiPlugin.getTaskListManager();
		manager.readExistingOrCreateNewList();
		manager.resetTaskList();
		manager.saveTaskList();
		repository = new TaskRepository(BugzillaCorePlugin.REPOSITORY_KIND, IBugzillaConstants.ECLIPSE_BUGZILLA_URL);
		TasksUiPlugin.getRepositoryManager().addRepository(repository,
				TasksUiPlugin.getDefault().getRepositoriesFilePath());
		assertEquals(0, manager.getTaskList().getAllTasks().size());
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		manager.resetTaskList();
		TasksUiPlugin.getTaskListManager().saveTaskList();
// TasksUiPlugin.getDefault().getTaskListSaveManager().saveTaskList(true);
		TasksUiPlugin.getRepositoryManager().removeRepository(repository,
				TasksUiPlugin.getDefault().getRepositoriesFilePath());
	}

	// TODO: move
	public void testRemindedPersistance() {

		String repositoryUrl = "https://bugs.eclipse.org/bugs";

		String bugNumber = "106939";

		BugzillaTask task1 = new BugzillaTask(repositoryUrl, bugNumber, "label");
		manager.getTaskList().addTask(task1);

		task1.setReminded(true);

		TasksUiPlugin.getTaskListManager().saveTaskList();
		TasksUiPlugin.getTaskListManager().resetTaskList();
		TasksUiPlugin.getTaskListManager().readExistingOrCreateNewList();

		TaskList taskList = manager.getTaskList();
		assertEquals(1, taskList.getAllTasks().size());
		Set<ITask> tasksReturned = taskList.getTasks(repositoryUrl);
		assertNotNull(tasksReturned);
		assertEquals(1, tasksReturned.size());
		for (ITask task : tasksReturned) {
			assertTrue(task.isReminded());
		}
	}

	public void testRepositoryTaskExternalization() {
		BugzillaTask repositoryTask = new BugzillaTask(IBugzillaConstants.ECLIPSE_BUGZILLA_URL, "1", "label");
		repositoryTask.setTaskKind("kind");
		manager.getTaskList().addTask(repositoryTask);
		manager.saveTaskList();

		manager.resetTaskList();
		// manager.getTaskList().clear();
		// TaskList list = new TaskList();
		// manager.setTaskList(list);
		manager.readExistingOrCreateNewList();
		assertEquals(1, manager.getTaskList()
				.getUnmatchedContainer(IBugzillaConstants.ECLIPSE_BUGZILLA_URL)
				.getChildren()
				.size());
		ITask readTask = manager.getTaskList()
				.getUnmatchedContainer(IBugzillaConstants.ECLIPSE_BUGZILLA_URL)
				.getChildren()
				.iterator()
				.next();

		assertEquals(repositoryTask.getHandleIdentifier(), readTask.getHandleIdentifier());
		assertEquals(repositoryTask.getSummary(), readTask.getSummary());
		assertEquals(repositoryTask.getTaskKind(), readTask.getTaskKind());
	}

	public void testQueryExternalization() {
		AbstractRepositoryQuery query = new BugzillaRepositoryQuery("repositoryUrl", "queryUrl", "label");
		// long time = 1234;
		// Date oldDate = new Date(time);
		// query.setLastRefresh(oldDate);
		assertEquals("repositoryUrl", query.getRepositoryUrl());
		assertEquals("queryUrl", query.getUrl());
		assertEquals("<never>", query.getLastSynchronizedTimeStamp());
		query.setLastSynchronizedStamp("today");
		// assertEquals(time, query.getLastSynchronized().getTime());
		manager.getTaskList().addQuery(query);
		manager.saveTaskList();
		assertNotNull(manager.getTaskList());

		manager.resetTaskList();
		manager.readExistingOrCreateNewList();
		assertEquals(1, manager.getTaskList().getQueries().size());
		AbstractRepositoryQuery readQuery = manager.getTaskList().getQueries().iterator().next();
		assertEquals(query.getUrl(), readQuery.getUrl());
		assertEquals(query.getRepositoryUrl(), readQuery.getRepositoryUrl());
		assertEquals("today", query.getLastSynchronizedTimeStamp());
		assertEquals("repositoryUrl", readQuery.getRepositoryUrl());
		// assertEquals(time, readQuery.getLastSynchronized().getTime());
	}

	public void testBugzillaCustomQueryExternalization() {
		BugzillaRepositoryQuery query = new BugzillaRepositoryQuery("repositoryUrl", "queryUrl", "label");
		query.setCustomQuery(true);
		manager.getTaskList().addQuery(query);
		manager.saveTaskList();

		manager.resetTaskList();
		// manager.getTaskList().clear();
		// TaskList list = new TaskList();
		// manager.setTaskList(list);
		manager.readExistingOrCreateNewList();
		assertEquals(1, manager.getTaskList().getQueries().size());
		BugzillaRepositoryQuery readQuery = (BugzillaRepositoryQuery) manager.getTaskList()
				.getQueries()
				.iterator()
				.next();
		assertTrue(readQuery.isCustomQuery());
	}

	public void testDeleteQuery() {
		AbstractRepositoryQuery query = new BugzillaRepositoryQuery("repositoryUrl", "queryUrl", "label");
		manager.getTaskList().addQuery(query);

		AbstractRepositoryQuery readQuery = manager.getTaskList().getQueries().iterator().next();
		assertEquals(query, readQuery);

		manager.getTaskList().deleteQuery(query);
		assertEquals(0, manager.getTaskList().getQueries().size());
	}

	public void testDeleteQueryAfterRename() {
		AbstractRepositoryQuery query = new BugzillaRepositoryQuery("repositoryUrl", "queryUrl", "label");
		manager.getTaskList().addQuery(query);

		AbstractRepositoryQuery readQuery = manager.getTaskList().getQueries().iterator().next();
		assertEquals(query, readQuery);
		manager.getTaskList().renameContainer(query, "newName");
		manager.getTaskList().deleteQuery(query);
		assertEquals(0, manager.getTaskList().getQueries().size());
	}

	public void testCreateQueryWithSameName() {
		AbstractRepositoryQuery query = new BugzillaRepositoryQuery("repositoryUrl", "queryUrl", "label");
		manager.getTaskList().addQuery(query);
		assertEquals(1, manager.getTaskList().getQueries().size());
		AbstractRepositoryQuery readQuery = manager.getTaskList().getQueries().iterator().next();
		assertEquals(query, readQuery);

		manager.getTaskList().addQuery(new BugzillaRepositoryQuery("repositoryUrl", "queryUrl", "label"));
		assertEquals(1, manager.getTaskList().getQueries().size());
	}
}
