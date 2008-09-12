/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.context.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.ui.IViewActionDelegate;

/**
 * Controls enablement
 * 
 * @author Mik Kersten
 */
public abstract class TaskContextAction extends Action implements IViewActionDelegate {

	protected ISelection selection;

	protected ITask getSelectedTask(ISelection newSelection) {
		if (selection instanceof StructuredSelection) {
			Object selectedObject = ((StructuredSelection) selection).getFirstElement();
			if (selectedObject instanceof ITask) {
				return (ITask) selectedObject;
			}
//			else if (selectedObject instanceof AbstractQueryHit) {
//				return ((AbstractQueryHit) selectedObject).getCorrespondingTask();
//			}
		}
		return null;
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
		ITask selectedTask = getSelectedTask(selection);
		if (selectedTask != null) {
			action.setEnabled(ContextCore.getContextManager().hasContext(selectedTask.getHandleIdentifier()));
		} else {
			action.setEnabled(false);
		}
	}
}
