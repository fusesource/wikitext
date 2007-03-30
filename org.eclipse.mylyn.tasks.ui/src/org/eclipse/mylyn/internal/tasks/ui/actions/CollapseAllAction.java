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

package org.eclipse.mylar.internal.tasks.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.mylar.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;

/**
 * @author Mik Kersten
 */
public class CollapseAllAction extends Action {

	private static final String LABEL = "Collapse All";

	public static final String ID = "org.eclipse.mylar.tasklist.actions.collapse.all";

	private TaskListView taskListView;

	public CollapseAllAction(TaskListView taskListView) {
		super(LABEL);
		this.taskListView = taskListView;
		setId(ID);
		setText(LABEL);
		setToolTipText(LABEL);
		setImageDescriptor(TasksUiImages.COLLAPSE_ALL);
	}

	@Override
	public void run() {
		if (taskListView.getViewer() != null)
			taskListView.getViewer().collapseAll();
	}
}
