/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.tasks.tests;

import junit.framework.TestCase;

import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListContentProvider;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylar.tasks.core.Task;
import org.eclipse.mylar.tasks.core.TaskList;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * @author Felix Schwarz
 */
public class TaskListContentProviderTest extends TestCase {

	private TaskListContentProvider provider;

	private TaskListView view;

	private TaskList taskList;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TaskListView.openInActivePerspective();
		view = TaskListView.getFromActivePerspective();
		provider = (TaskListContentProvider) view.getViewer().getContentProvider();
		TasksUiPlugin.getDefault().getPreferenceStore().setValue(TaskListPreferenceConstants.FILTER_SUBTASKS, false);
		view.clearFilters(true);
		view.addFilter(view.getCompleteFilter());
		taskList = TasksUiPlugin.getTaskListManager().getTaskList();
	}

	@Override
	protected void tearDown() throws Exception {
		view.clearFilters(true);
		super.tearDown();
	}

	public void testHasChildren() {

		Task parent = new Task("parent", "parent label");
		Task completedChild = new Task("completed child", "completed child label");
		completedChild.setCompleted(true);
		taskList.addTask(completedChild, parent);
		assertFalse(provider.hasChildren(parent));

		Task incompleteChild = new Task("incomplete child", "incomplete child label");
		incompleteChild.setCompleted(false);
		taskList.addTask(incompleteChild, parent);
		assertTrue(provider.hasChildren(parent));
	}
}
