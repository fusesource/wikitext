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

package org.eclipse.mylar.internal.tasks.ui.editors;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

/**
 * @author Rob Elves
 */
public class TaskEditorInputFactory implements IElementFactory {

	private static final String TAG_TASK_HANDLE = "taskHandle";

	public static final String ID_FACTORY = "org.eclipse.mylar.internal.tasks.ui.editors.TaskEditorInputFactory";

	public IAdaptable createElement(IMemento memento) {
		String handle = memento.getString(TAG_TASK_HANDLE);
		ITask task = TasksUiPlugin.getTaskListManager().getTaskList().getTask(handle);
		if (task != null) {
			return new TaskEditorInput(task, false);
		}
		return null;
	}
	

	public static void saveState(IMemento memento, TaskEditorInput input) {
		if(memento != null && input != null && input.getTask() != null) {
			memento.putString(TAG_TASK_HANDLE, input.getTask().getHandleIdentifier());
		}
	}
}