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

package org.eclipse.mylar.internal.tasklist.planner.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.mylar.internal.tasklist.ui.TaskListImages;
import org.eclipse.mylar.tasks.core.ITask;

/**
 * @author Rob Elves
 */
public class RemoveTaskAction extends Action {
	public static final String ID = "org.eclipse.mylar.taskplannereditor.actions.remove";

	private final TableViewer viewer;

	public RemoveTaskAction(TableViewer view) {
		this.viewer = view;
		setText("Remove Selected");
		setId(ID);
		setImageDescriptor(TaskListImages.REMOVE);
	}

	@Override
	public void run() {
		for (Object object : ((IStructuredSelection) viewer.getSelection()).toList()) {
			if (object instanceof ITask) {
				ITask task = (ITask) object;
				if (task != null) {									
					((ITaskPlannerContentProvider) (viewer.getContentProvider())).removeTask(task);
				}
			}
		}
		viewer.refresh();
	}
}
