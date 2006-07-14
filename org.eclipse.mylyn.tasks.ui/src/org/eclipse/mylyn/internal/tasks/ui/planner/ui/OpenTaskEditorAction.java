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

package org.eclipse.mylar.internal.tasks.ui.planner.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.mylar.internal.tasks.ui.ui.TaskUiUtil;
import org.eclipse.mylar.tasks.core.ITask;

/**
 * @author Mik Kersten
 * @author Ken Sueda
 * @author Rob Elves
 */
public class OpenTaskEditorAction extends Action {

	public static final String ID = "org.eclipse.mylar.taskplannereditor.actions.open";

	private final TableViewer viewer;

	/**
	 * @param view
	 */
	public OpenTaskEditorAction(TableViewer view) {
		this.viewer = view;
		setText("Open");
		setToolTipText("Open Element");
		setId(ID);
	}

	@Override
	public void run() {
		ISelection selection = viewer.getSelection();
		Object object = ((IStructuredSelection) selection).getFirstElement();
		if (object instanceof ITask) {
			TaskUiUtil.openEditor((ITask)object, false);
		}
//		if (obj instanceof Task) {
//			((Task) obj).openTaskInEditor(false);
//		}
	}
}
