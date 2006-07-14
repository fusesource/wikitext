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
import org.eclipse.mylar.internal.tasks.ui.TaskListImages;
import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * @author Mik Kersten 
 */
public class FilterArchiveContainerAction extends Action {

	public static final String ID = "org.eclipse.mylar.tasklist.actions.filter.archive";

	private static final String LABEL = "Filter Archive Category";
	
	private final TaskListView view;

	public FilterArchiveContainerAction(TaskListView view) {
		this.view = view;
		setText(LABEL);
		setToolTipText(LABEL);
		setId(ID); 
		setImageDescriptor(TaskListImages.FILTER_ARCHIVE);
		setChecked(TasksUiPlugin.getDefault().getPreferenceStore().contains(TaskListPreferenceConstants.FILTER_ARCHIVE_MODE));
	}

	@Override
	public void run() {
		TasksUiPlugin.getDefault().getPreferenceStore().setValue(TaskListPreferenceConstants.FILTER_ARCHIVE_MODE, isChecked());
		if (isChecked()) {
			view.addFilter(view.getArchiveFilter());
		} else {
			view.removeFilter(view.getArchiveFilter());
		}
		this.view.getViewer().refresh();
	}
}
