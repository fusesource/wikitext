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

package org.eclipse.mylar.internal.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.mylar.internal.tasklist.ui.views.TaskListView;
import org.eclipse.mylar.provisional.core.MylarPlugin;
import org.eclipse.mylar.provisional.tasklist.ITask;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

/**
 * @author Mik Kersten and Ken Sueda
 */
public class ClearContextAction implements IViewActionDelegate {

	public static final String ID = "org.eclipse.mylar.ui.clear.context";

	public void init(IViewPart view) {

	}

	public void run(IAction action) {
		ITask task = TaskListView.getDefault().getSelectedTask();
		if (task instanceof ITask) {
			boolean deleteConfirmed = MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getShell(), "Confirm clear context", "Clear context for the selected task?");
			if (!deleteConfirmed)
				return;

			MylarPlugin.getContextManager().contextDeleted((task).getHandleIdentifier());																					// ((Task)selectedObject).getContextPath());
			MylarPlugin.getContextManager().contextActivated((task).getHandleIdentifier());
																												// ((Task)selectedObject).getContextPath());
			TaskListView.getDefault().getViewer().refresh();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}
}
