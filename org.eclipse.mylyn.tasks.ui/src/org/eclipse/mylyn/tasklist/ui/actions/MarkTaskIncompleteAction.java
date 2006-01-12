/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.tasklist.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylar.tasklist.ITask;
import org.eclipse.mylar.tasklist.MylarTaskListPlugin;
import org.eclipse.mylar.tasklist.ui.TaskListImages;
import org.eclipse.mylar.tasklist.ui.views.TaskListView;

/**
 * @author Mik Kersten and Ken Sueda
 */
public class MarkTaskIncompleteAction extends Action {

	public static final String ID = "org.eclipse.mylar.tasklist.actions.mark.incomplete";

	private final TaskListView view;

	public MarkTaskIncompleteAction(TaskListView view) {
		this.view = view;
		setText("Mark Incomplete");
		setToolTipText("Mark Incomplete");
		setId(ID);
		setImageDescriptor(TaskListImages.TASK_INCOMPLETE);
	}

	@Override
	public void run() {
		for (Object selectedObject : ((IStructuredSelection)this.view.getViewer().getSelection()).toList()) {
			if (selectedObject instanceof ITask) {
				MylarTaskListPlugin.getTaskListManager().markComplete(((ITask)selectedObject), false);
			}
		}
	}
}