/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.mylar.internal.tasks.ui.preferences.TasksPreferencePage;
import org.eclipse.mylar.tasks.ui.TasksUiUtil;

/**
 * @author Mik Kersten
 */
public class OpenTasksUiPreferencesAction extends Action {

	public static final String ID = "org.eclipse.mylar.tasks.ui.actions.preferences.open";
	
	private static final String LABEL = "Preferences...";

	public OpenTasksUiPreferencesAction() {
		setText(LABEL);
		setToolTipText(LABEL);
		setId(ID);
	}

	@Override
	public void run() {
		TasksUiUtil.showPreferencePage(TasksPreferencePage.ID, new TasksPreferencePage());
	}
}
