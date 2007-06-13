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
package org.eclipse.mylyn.internal.tasks.ui.actions;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylyn.internal.tasks.core.LocalTask;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylyn.tasks.core.ITaskListElement;
import org.eclipse.swt.SWT;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * @author Mik Kersten
 */
public class RenameAction extends BaseSelectionListenerAction {

	private static final String LABEL_NAME = "Rename";

	public static final String ID = "org.eclipse.mylyn.tasklist.actions.rename";

	private TaskListView view;

	public RenameAction(TaskListView view) {
		super(LABEL_NAME);
		this.view = view;
		setId(ID);
		setAccelerator(SWT.F2);
	}

	@Override
	public void run() {
		Object selectedObject = ((IStructuredSelection) this.view.getViewer().getSelection()).getFirstElement();
		if (selectedObject instanceof ITaskListElement) {
			ITaskListElement element = (ITaskListElement) selectedObject;
			view.setInRenameAction(true);
			view.getViewer().editElement(element, 0);
			view.setInRenameAction(false);
		}
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		return selection.getFirstElement() instanceof LocalTask;
	}
}
