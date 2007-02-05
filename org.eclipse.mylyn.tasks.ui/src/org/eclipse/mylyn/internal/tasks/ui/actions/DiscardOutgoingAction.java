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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylar.internal.tasks.ui.TaskListImages;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskListElement;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask.RepositoryTaskSyncState;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * Discard outgoing changes on selected task
 * TODO: Enable multi task discard?
 * @author Rob Elves
 */
public class DiscardOutgoingAction extends Action {

	private static final String ACTION_NAME = "Clear outgoing";

	public static final String ID = "org.eclipse.mylar.tasklist.actions.mark.discard";

	private List<ITaskListElement> selectedElements;
	
	public DiscardOutgoingAction(List<ITaskListElement> selectedElements) {
		this.selectedElements = selectedElements;
		setText(ACTION_NAME);
		setToolTipText(ACTION_NAME);
		setId(ID);
		setImageDescriptor(TaskListImages.STATUS_NORMAL);
		if (selectedElements.size() == 1 && (selectedElements.get(0) instanceof AbstractRepositoryTask)) {
			AbstractRepositoryTask task = (AbstractRepositoryTask) selectedElements.get(0);
			setEnabled(task.getSyncState().equals(RepositoryTaskSyncState.OUTGOING));
		} else if (selectedElements.size() == 1 && (selectedElements.get(0) instanceof AbstractQueryHit)) {
			AbstractQueryHit hit = ((AbstractQueryHit)(selectedElements.get(0)));
			AbstractRepositoryTask task = hit.getCorrespondingTask();			
			setEnabled(task != null && task.getSyncState().equals(RepositoryTaskSyncState.OUTGOING));
		} else {
			setEnabled(false);
		}
	}

	@Override
	public void run() {
		ArrayList<AbstractRepositoryTask> toClear = new ArrayList<AbstractRepositoryTask>();
		for (Object selectedObject : selectedElements) {
			if (selectedObject instanceof AbstractRepositoryTask
					&& ((AbstractRepositoryTask) selectedObject).getSyncState()
							.equals(RepositoryTaskSyncState.OUTGOING)) {
				toClear.add(((AbstractRepositoryTask) selectedObject));
			} else if (selectedObject instanceof AbstractQueryHit) {
				ITask task = ((AbstractQueryHit) selectedObject).getCorrespondingTask();
				if (task instanceof AbstractRepositoryTask) {
					toClear.add(((AbstractRepositoryTask) task));
				}
			}
		}
		if (toClear.size() > 0) {
			boolean confirm = MessageDialog.openConfirm(null, "Confirm discard", "Discard all outgoing changes?\n\n"+(toClear.get(0)).getSummary());
			if (confirm) {
				TasksUiPlugin.getSynchronizationManager().discardOutgoing(toClear.get(0));
			}
		}
	}
}
