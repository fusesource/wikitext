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

import java.util.List;

import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.ITaskListElement;
import org.eclipse.mylar.tasks.core.Task;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * @author Rob Elves
 */
public class MarkTaskReadAction extends AbstractRepositoryTasksAction {

	private static final String ACTION_NAME = "Read";

	public static final String ID = "org.eclipse.mylar.tasklist.actions.mark.read";

	public MarkTaskReadAction(List<ITaskListElement> selectedElements) {
		super.selectedElements = selectedElements;
		setText(ACTION_NAME);
		setToolTipText("Mark " + ACTION_NAME);
		setId(ID);
		if (containsArchiveContainer(selectedElements)) {
			setEnabled(false);
		} else {
			setEnabled(selectedElements.size() > 0);
			if (selectedElements.size() == 1 && (selectedElements.get(0) instanceof Task)) {
				Task task = (Task) selectedElements.get(0);
				setEnabled(!task.isLocal());
			} else {
				setEnabled(true);
			}
		}
	}

	@Override
	protected void performActionOnTask(AbstractRepositoryTask repositoryTask) {
		TasksUiPlugin.getSynchronizationManager().setTaskRead(repositoryTask, true);
	}

}
