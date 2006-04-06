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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.mylar.provisional.tasklist.ITask;

public class TaskActivityContentProvider implements IStructuredContentProvider, ITaskPlannerContentProvider {

	TaskActivityEditorInput editorInput;
	Viewer viewer;
	
	public TaskActivityContentProvider(TaskActivityEditorInput editorInput) {
		this.editorInput = editorInput;
	}
	
	
	public Object[] getElements(Object inputElement) {		
		List<ITask> allTasks = new ArrayList<ITask>();
		allTasks.addAll(editorInput.getCompletedTasks());
		allTasks.addAll(editorInput.getInProgressTasks());		
		return allTasks.toArray();
	}

	public void dispose() {
		// ignore
		
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
	}


	public void removeTask(ITask task) {
		editorInput.removeCompletedTask(task);
		editorInput.removeInProgressTask(task);		
	}


	public void addTask(ITask task) {
		// ignore		
	}
	
}
