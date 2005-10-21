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
import org.eclipse.mylar.tasklist.TaskListImages;
import org.eclipse.mylar.tasklist.ui.views.TaskListView;
import org.eclipse.ui.part.DrillDownAdapter;

public class GoUpAction extends Action {


	public static final String ID = "org.eclipse.mylar.tasklist.actions.view.go.up";
	
	private DrillDownAdapter drillDownAdapter;
	
	public GoUpAction(DrillDownAdapter drillDownAdapter) {
		this.drillDownAdapter = drillDownAdapter;
		setText("Go Up To Root");
		setToolTipText("Go Up To Root");
		setId(ID);
		setImageDescriptor(TaskListImages.GO_UP);
	}

	@Override
	public void run() {
		drillDownAdapter.goBack();
		if(TaskListView.getDefault() != null){
			TaskListView.getDefault().updateDrillDownActions();
		}
	}
}
