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

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.mylar.provisional.tasklist.ITask;

/**
 * @author Rob Elves
 * @author Ken Sueda
 */
public class PlannedTasksContentProvider implements IStructuredContentProvider, ITaskPlannerContentProvider {

	TaskPlannerEditorInput editorInput;

	public PlannedTasksContentProvider(TaskPlannerEditorInput editorInput) {
		this.editorInput = editorInput;
	}

	public Object[] getElements(Object inputElement) {
		return editorInput.getPlannedTasks().toArray();
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public void addTask(ITask task) {
		editorInput.addPlannedTask(task);
	}

	public void removeTask(ITask task) {
		editorInput.removePlannedTask(task);
	}

}
