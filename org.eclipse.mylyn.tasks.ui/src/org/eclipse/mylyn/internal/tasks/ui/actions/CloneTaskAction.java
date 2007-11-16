/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskSelection;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * @author Maarten Meijer
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class CloneTaskAction extends BaseSelectionListenerAction implements IViewActionDelegate {

	private static final String LABEL = "Clone This Task";

	private static final String ID = "org.eclipse.mylyn.tasklist.actions.clone";

	protected ISelection selection;

	public CloneTaskAction() {
		super(LABEL);
		setId(ID);
		setImageDescriptor(TasksUiImages.TASK_NEW);
		setAccelerator(SWT.MOD1 + 'd');
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			super.selectionChanged((IStructuredSelection) selection);
		}
	}

	public void run(IAction action) {
		run();
	}

	@Override
	public void run() {
		try {
			for (Object selectedObject : getStructuredSelection().toList()) {
				if (selectedObject instanceof AbstractTask) {
					AbstractTask task = (AbstractTask) selectedObject;

					String description = "Clone of " + CopyTaskDetailsAction.getTextForTask(task);

					final TaskSelection taskSelection;
					RepositoryTaskData taskData = TasksUiPlugin.getTaskDataManager().getNewTaskData(
							task.getRepositoryUrl(), task.getTaskId());
					if (taskData != null) {
						taskSelection = new TaskSelection(taskData);
						taskSelection.getTaskData().setDescription(description + "\n\n> " + taskData.getDescription());
					} else {
						taskSelection = new TaskSelection(task);
						taskSelection.getTaskData().setDescription(description);
					}

					NewTaskAction action = new NewTaskAction();
					if (action.showWizard(taskSelection) != Dialog.OK) {
						return;
					}
				}
			}
		} catch (NullPointerException npe) {
			StatusHandler.fail(npe, "Could not remove task from category, it may still be refreshing.", true);
		}
	}

	public void init(IViewPart view) {
		// ignore
	}

}