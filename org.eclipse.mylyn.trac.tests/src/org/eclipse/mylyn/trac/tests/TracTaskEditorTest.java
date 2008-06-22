/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.trac.tests;

import junit.framework.TestCase;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylyn.internal.trac.core.client.ITracClient.Version;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.trac.tests.support.TracTestConstants;
import org.eclipse.mylyn.trac.tests.support.TracTestUtil;

/**
 * @author Steffen Pingel
 */
public class TracTaskEditorTest extends TestCase {

	public void testGetSelectedRepository() throws Exception {
		TaskRepository repository = TracTestUtil.init(TracTestConstants.TEST_TRAC_010_URL,
				Version.XML_RPC);

		ITask task = TracTestUtil.createTask(repository, "1");
		TasksUiPlugin.getTaskList().addTask(task);
		TasksUiUtil.openTask(task);

		TaskListView taskListView = TaskListView.getFromActivePerspective();
		// force refresh since automatic refresh is delayed  
		taskListView.getViewer().refresh();
		taskListView.getViewer().expandAll();
		taskListView.getViewer().setSelection(new StructuredSelection(task));

		assertFalse(taskListView.getViewer().getSelection().isEmpty());
		assertEquals(repository, TasksUiUtil.getSelectedRepository(taskListView.getViewer()));
	}

}
