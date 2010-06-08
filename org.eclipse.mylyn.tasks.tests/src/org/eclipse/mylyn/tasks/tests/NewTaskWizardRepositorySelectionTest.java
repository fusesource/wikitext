/*******************************************************************************
 * Copyright (c) 2004, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.tasks.tests;

import junit.framework.TestCase;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.mylyn.internal.tasks.core.LocalRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylyn.internal.tasks.ui.wizards.MultiRepositoryAwareWizard;
import org.eclipse.mylyn.internal.tasks.ui.wizards.SelectRepositoryPage;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.tests.connector.MockRepositoryConnector;
import org.eclipse.mylyn.tasks.tests.connector.MockTask;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.wizards.NewTaskWizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @author Mik Kersten
 */
public class NewTaskWizardRepositorySelectionTest extends TestCase {

	// see bug 203801
	public void testRepositorySettingWithTaskListSelection() {
		TaskRepository mockRepository = new TaskRepository(MockRepositoryConnector.CONNECTOR_KIND,
				MockRepositoryConnector.REPOSITORY_URL);
		TasksUiPlugin.getRepositoryManager().addRepository(mockRepository);

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		TaskListView view = (TaskListView) TasksUiUtil.openTasksViewInActivePerspective();
		MockTask mockTask = new MockTask("mock.task");
		TasksUiPlugin.getTaskActivityManager().scheduleNewTask(mockTask);
		TasksUiPlugin.getTaskList().addTask(mockTask);

		view.setFocusedMode(true);
		view.getViewer().refresh();
		view.getViewer().expandAll();
		view.getViewer().setSelection(new StructuredSelection(mockTask), true);
		assertEquals(mockTask, ((StructuredSelection) view.getViewer().getSelection()).getFirstElement());

		MultiRepositoryAwareWizard wizard = TasksUiInternal.createNewTaskWizard(null);
		WizardDialog dialog = null;
		dialog = new WizardDialog(shell, wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		SelectRepositoryPage page = (SelectRepositoryPage) wizard.getPages()[0];
		assertTrue(page.getRepositories().contains(mockRepository));
		assertEquals(mockRepository, ((IStructuredSelection) page.getViewer().getSelection()).getFirstElement());

		TasksUiPlugin.getRepositoryManager().removeRepository(mockRepository);
		TasksUiPlugin.getTaskList().deleteTask(mockTask);
		wizard.dispose();
		dialog.close();
	}

	// see bug bug 202184
	public void testDefaultWithNoTaskListSelection() {
		TaskListView view = (TaskListView) TasksUiUtil.openTasksViewInActivePerspective();
		view.getViewer().setSelection(new StructuredSelection());

		NewTaskWizard wizard = new NewTaskWizard(null, null);
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		WizardDialog dialog = null;
		dialog = new WizardDialog(shell, wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		SelectRepositoryPage page = (SelectRepositoryPage) wizard.getPages()[0];
		TaskRepository localRepository = TasksUiPlugin.getRepositoryManager().getRepositories(
				LocalRepositoryConnector.CONNECTOR_KIND).iterator().next();
		assertEquals(localRepository, ((IStructuredSelection) page.getViewer().getSelection()).getFirstElement());

		wizard.dispose();
		dialog.close();
	}
}
