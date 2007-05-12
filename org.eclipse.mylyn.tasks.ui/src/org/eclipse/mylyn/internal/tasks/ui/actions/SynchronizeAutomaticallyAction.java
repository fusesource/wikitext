/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * @author Mik Kersten
 */
public class SynchronizeAutomaticallyAction extends Action {

	public static final String ID = "org.eclipse.mylar.tasks.ui.actions.synchronize.background";
	
	private static final String LABEL = "Synchronize Automatically";

	public SynchronizeAutomaticallyAction() {
		setText(LABEL);
		setToolTipText(LABEL);
		setId(ID);
		setChecked(TasksUiPlugin.getDefault().getPreferenceStore().getBoolean(TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_ENABLED));
	}

	@Override
	public void run() {
		TasksUiPlugin.getDefault().getPreferenceStore().setValue(TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_ENABLED,
				isChecked());
	}
}
