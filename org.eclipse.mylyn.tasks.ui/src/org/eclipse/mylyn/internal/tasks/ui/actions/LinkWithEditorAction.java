/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;

/**
 * @author Willian Mitsuda
 */
public class LinkWithEditorAction extends Action {

	private final TaskListView taskListView;

	public LinkWithEditorAction(TaskListView taskListView) {
		super("&Link with Editor", IAction.AS_CHECK_BOX);
		setImageDescriptor(TasksUiImages.LINK_EDITOR);
		this.taskListView = taskListView;
	}

	@Override
	public void run() {
		taskListView.setLinkWithEditor(isChecked());
	}

}