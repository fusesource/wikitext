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
package org.eclipse.mylyn.tasks.tests;

import java.io.File;
import java.util.Collection;

import org.eclipse.mylyn.context.core.ContextCorePlugin;
import org.eclipse.mylyn.context.tests.AbstractContextTest;
import org.eclipse.mylyn.internal.context.core.InteractionContext;
import org.eclipse.mylyn.internal.tasks.core.LocalRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.core.LocalTask;
import org.eclipse.mylyn.internal.tasks.ui.wizards.TaskDataImportWizard;
import org.eclipse.mylyn.internal.tasks.ui.wizards.TaskDataImportWizardPage;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.Task;
import org.eclipse.mylyn.tasks.core.TaskList;
import org.eclipse.mylyn.tasks.ui.TaskListManager;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.widgets.Shell;

/**
 * Test case for the Task Import Wizard
 * 
 * @author Rob Elves
 */
public class TaskDataImportTest extends AbstractContextTest {

	private TaskDataImportWizard wizard = null;

	private TaskDataImportWizardPage wizardPage = null;

	// private String sourceDir = "testdata/taskdataimporttest";

	// private File sourceDirFile = null;

	private String sourceZipPath = "testdata/taskdataimporttest/mylardata-2007-01-19.zip";

	private File sourceZipFile = null;

	private TaskListManager manager = TasksUiPlugin.getTaskListManager();

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// Create the import wizard
		wizard = new TaskDataImportWizard();
		wizard.addPages();
		wizard.createPageControls(new Shell());
		wizardPage = (TaskDataImportWizardPage) wizard.getPage(TaskDataImportWizardPage.PAGE_NAME);
		assertNotNull(wizardPage);

		manager.resetTaskList();
		assertEquals(2, manager.getTaskList().getRootElements().size());

		sourceZipFile = TaskTestUtil.getLocalFile(sourceZipPath);
		assertTrue(sourceZipFile.exists());

		// make sure no tasks and categories exist prior to import tests
		assertEquals(2, manager.getTaskList().getTaskContainers().size());
		ContextCorePlugin.getContextManager().getActivityMetaContext().reset();
	}

	@Override
	protected void tearDown() throws Exception {
		ContextCorePlugin.getContextManager().resetActivityHistory();
		TasksUiPlugin.getRepositoryManager().clearRepositories(TasksUiPlugin.getDefault().getRepositoriesFilePath());
		TasksUiPlugin.getTaskListManager().resetTaskList();
		super.tearDown();
	}

	/**
	 * Tests the wizard when it has been asked to import all task data from a zip file
	 */
	public void testImportRepositoriesZip() {
		TaskList taskList = TasksUiPlugin.getTaskListManager().getTaskList();
		InteractionContext historyContext = ContextCorePlugin.getContextManager().getActivityMetaContext();
		assertNotNull(taskList);
		assertNotNull(historyContext);
		assertTrue(taskList.getAllTasks().size() == 0);
		assertTrue(historyContext.getInteractionHistory().size() == 0);
		//assertEquals(1, TasksUiPlugin.getRepositoryManager().getAllRepositories().size());

		wizardPage.setParameters(true, true, true, true, true, "", sourceZipFile.getPath());
		wizard.performFinish();

		Collection<ITask> tasks = taskList.getAllTasks();
		assertEquals(2, tasks.size());
		for (ITask task : tasks) {
			assertTrue(ContextCorePlugin.getContextManager().hasContext(task.getHandleIdentifier()));
		}
		historyContext = ContextCorePlugin.getContextManager().getActivityMetaContext();
		assertNotNull(historyContext);
		assertTrue(historyContext.getInteractionHistory().size() > 0);
		assertEquals(2, TasksUiPlugin.getRepositoryManager().getAllRepositories().size());
	}

	public void testImportOverwritesAllTasks() {
		TaskList taskList = TasksUiPlugin.getTaskListManager().getTaskList();
		InteractionContext historyContext = ContextCorePlugin.getContextManager().getActivityMetaContext();
		assertNotNull(taskList);
		assertNotNull(historyContext);
		assertTrue(taskList.getAllTasks().size() == 0);
		assertTrue(historyContext.getInteractionHistory().size() == 0);
		assertEquals(2, TasksUiPlugin.getRepositoryManager().getAllRepositories().size());

		Task task1 = new LocalTask(LocalRepositoryConnector.REPOSITORY_URL, "999", "label");
		taskList.addTask(task1);
		Collection<ITask> tasks = taskList.getAllTasks();
		assertEquals(1, tasks.size());

		wizardPage.setParameters(true, true, true, true, true, "", sourceZipFile.getPath());
		wizard.performFinish();

		tasks = taskList.getAllTasks();
		assertEquals(2, tasks.size());
		assertTrue(!taskList.getAllTasks().contains(task1));
		for (ITask task : tasks) {
			assertTrue(ContextCorePlugin.getContextManager().hasContext(task.getHandleIdentifier()));
		}
		historyContext = ContextCorePlugin.getContextManager().getActivityMetaContext();
		assertNotNull(historyContext);
		assertTrue(historyContext.getInteractionHistory().size() > 0);
		assertEquals(3, TasksUiPlugin.getRepositoryManager().getAllRepositories().size());
	}

}
