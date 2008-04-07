/*******************************************************************************
 * Copyright (c) 2007 - 2007 CodeGear and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.xplanner.tests;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.xplanner.core.service.XPlannerClient;
import org.eclipse.mylyn.xplanner.ui.XPlannerAttributeFactory;
import org.eclipse.mylyn.xplanner.ui.XPlannerRepositoryConnector;
import org.eclipse.mylyn.xplanner.ui.XPlannerTask;
import org.xplanner.soap.TaskData;
import org.xplanner.soap.UserStoryData;

public class XPlannerRepositoryConnectorTest extends TestCase {
	private static XPlannerClient client;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		if (client == null) { // only create data once per run
			client = XPlannerTestUtils.getXPlannerClient();
			XPlannerTestUtils.clearTestData(client);
			XPlannerTestUtils.setUpTestData(client);
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@SuppressWarnings("null")
	public void testCreateTaskFromExistingKeyForUserStory() throws Exception {
		TaskRepository repository = XPlannerTestUtils.getRepository();
		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
				repository.getConnectorKind());
		UserStoryData testUserStory = XPlannerTestUtils.findTestUserStory(client);

		assertTrue(testUserStory != null);

		AbstractTask repositoryTask = connector.createTaskFromExistingId(repository, "" + testUserStory.getId(),
				new NullProgressMonitor());

		assertTrue(repositoryTask instanceof XPlannerTask);
		assertTrue(((XPlannerTask) repositoryTask).getSummary().equals(testUserStory.getName()));
	}

	@SuppressWarnings("null")
	public void testCreateTaskFromExistingKeyForTask() throws Exception {
		TaskRepository repository = XPlannerTestUtils.getRepository();
		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
				repository.getConnectorKind());
		TaskData testTask = XPlannerTestUtils.findTestTask(client);

		assertTrue(testTask != null);

		AbstractTask repositoryTask = connector.createTaskFromExistingId(repository, "" + testTask.getId(),
				new NullProgressMonitor());

		assertTrue(repositoryTask instanceof XPlannerTask);
		assertTrue(((XPlannerTask) repositoryTask).getSummary().equals(testTask.getName()));
	}

	@SuppressWarnings("null")
	public void testUpdateTaskDetailsCompleted() throws Exception {
		TaskRepository repository = XPlannerTestUtils.getRepository();
		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
				repository.getConnectorKind());

		assertTrue(connector instanceof XPlannerRepositoryConnector);
		XPlannerRepositoryConnector xplannerConnector = (XPlannerRepositoryConnector) connector;
		TaskData testTask = XPlannerTestUtils.findTestTask(client);
		XPlannerTask repositoryTask = XPlannerTestUtils.getTestXPlannerTask(client);

		assertTrue(testTask != null);
		assertTrue(repositoryTask != null);

		// update repository task with details
		// save previous completion state
		boolean originalCompleted = testTask.isCompleted();

		// mark testTask as completed
		testTask.setCompleted(true);
		client.update(testTask);

		xplannerConnector.updateTaskDetails(repository.getRepositoryUrl(), repositoryTask, testTask, false);
		assertTrue(repositoryTask.isCompleted());

		//restore testTask's completion state
		testTask.setCompleted(originalCompleted);
		client.update(testTask);
	}

	public void testMarkStaleTasksNoStaleTasks() throws Exception {
		TaskRepository repository = XPlannerTestUtils.getRepository();
		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
				repository.getConnectorKind());

		assertTrue(connector instanceof XPlannerRepositoryConnector);
		XPlannerRepositoryConnector xplannerConnector = (XPlannerRepositoryConnector) connector;

		Set<AbstractTask> tasks = TasksUiPlugin.getTaskListManager().getTaskList().getRepositoryTasks(
				repository.getRepositoryUrl());
		setSyncTimeStamp(repository, tasks);

		String goodUrl = repository.getRepositoryUrl();
		boolean stale = false;
		try {
			repository.setRepositoryUrl("http://localhost");
			stale = xplannerConnector.markStaleTasks(repository, tasks, new NullProgressMonitor());
		} catch (CoreException e) {
			assertTrue(e.getMessage() != null && e.getMessage().contains("Connection error"));
		} finally {
			repository.setRepositoryUrl(goodUrl);
		}

		assertTrue(!stale);
	}

	private void setSyncTimeStamp(TaskRepository repository, Set<AbstractTask> tasks) throws Exception {
		Date date = tasks.iterator().next().getCreationDate();
		String timeStamp = XPlannerAttributeFactory.TIME_DATE_FORMAT.format(date);
		for (AbstractTask task : tasks) {
			if (task instanceof XPlannerTask) {
				TaskData taskData = client.getTask(Integer.valueOf(task.getTaskKey()).intValue());
				if (taskData != null) {
					Calendar lastUpdateTime = new GregorianCalendar();
					lastUpdateTime.setTime(date);
					taskData.setLastUpdateTime(lastUpdateTime);
				}

				task.setLastReadTimeStamp(timeStamp);
			}
		}

		TasksUiPlugin.getRepositoryManager().setSynchronizationTime(repository, timeStamp,
				TasksUiPlugin.getDefault().getRepositoriesFilePath());
	}

	@SuppressWarnings("null")
	public void testChangedSinceLastSyncWithBadConnection() throws Exception {
		TaskRepository repository = XPlannerTestUtils.getRepository();
		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
				repository.getConnectorKind());

		assertTrue(connector instanceof XPlannerRepositoryConnector);
		XPlannerRepositoryConnector xplannerConnector = (XPlannerRepositoryConnector) connector;

		// make bad url
		Set<AbstractTask> tasks = TasksUiPlugin.getTaskListManager().getTaskList().getRepositoryTasks(
				repository.getRepositoryUrl());
		setSyncTimeStamp(repository, tasks);

		String goodUrl = repository.getRepositoryUrl();
		repository.setRepositoryUrl("http://localhost");
		try {
			Set<AbstractTask> changedTasks = xplannerConnector.getChangedSinceLastSync(repository, tasks);
			assertTrue(changedTasks != null);
			assertTrue(changedTasks.size() == 0);
		} catch (CoreException e) {
			assertTrue(e.getMessage() != null && e.getMessage().contains("Connection error"));
		} finally {
			repository.setRepositoryUrl(goodUrl);
		}
	}
}
