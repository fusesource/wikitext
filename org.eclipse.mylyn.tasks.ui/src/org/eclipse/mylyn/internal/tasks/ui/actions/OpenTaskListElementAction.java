/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

/**
 * @author Mik Kersten
 */
public class OpenTaskListElementAction extends Action {

	public static final String ID = "org.eclipse.mylyn.tasklist.actions.open";

	private final StructuredViewer viewer;

	public OpenTaskListElementAction(StructuredViewer view) {
		this.viewer = view;
		setText("Open");
		setToolTipText("Open Task List Element");
		setId(ID);
	}

	@Override
	public void run() {
		runWithEvent(null);
	}

	@Override
	public void runWithEvent(Event event) {
		ISelection selection = viewer.getSelection();
		List<?> list = ((IStructuredSelection) selection).toList();
		for (Object element : list) {
			if (element instanceof AbstractTask && event != null && (event.keyCode & SWT.MOD1) != 0) {
				TasksUiUtil.openTaskInBackground((AbstractTask) element);
			} else if (element instanceof AbstractTaskContainer) {
				TasksUiUtil.refreshAndOpenTaskListElement((AbstractTaskContainer) element);
			}
		}
	}
}
