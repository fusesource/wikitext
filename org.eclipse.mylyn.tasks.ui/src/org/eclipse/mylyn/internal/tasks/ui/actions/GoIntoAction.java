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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.mylar.internal.tasks.ui.TaskListImages;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * @author Mik Kersten
 */
public class GoIntoAction extends Action implements IViewActionDelegate {

	private static final String LABEL = "Go Into";
	public static final String ID = "org.eclipse.mylar.tasklist.actions.view.go.into";

	//		
	// private DrillDownAdapter drillDownAdapter;
	//	
	public GoIntoAction() {
		setId(ID);
		setText(LABEL);
		setToolTipText(LABEL);
		setImageDescriptor(TaskListImages.GO_INTO);
	}

	public void init(IViewPart view) {
		// TODO Auto-generated method stub

	}

	public void run() {
		if (TaskListView.getFromActivePerspective() != null) {
			TaskListView.getFromActivePerspective().getFilteredTree().setFilterText("");
			TaskListView.getFromActivePerspective().goIntoCategory();
		}
	}

	public void run(IAction action) {
		run();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}
}
