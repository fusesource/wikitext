/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.mylyn.internal.tasks.ui.AddExistingTaskJob;
import org.eclipse.mylyn.internal.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.internal.tasks.ui.commands.RemoteTaskSelectionDialog;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * @author Mik Kersten
 */
public class OpenRepositoryTask extends Action implements IWorkbenchWindowActionDelegate {

	private static final String OPEN_REMOTE_TASK_DIALOG_DIALOG_SETTINGS = "org.eclipse.mylyn.tasks.ui.open.remote";

	public void run(IAction action) {
		RemoteTaskSelectionDialog dlg = new RemoteTaskSelectionDialog(PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getShell());
		dlg.setTitle("Open Repository Task");

		IDialogSettings settings = TasksUiPlugin.getDefault().getDialogSettings();
		IDialogSettings dlgSettings = settings.getSection(OPEN_REMOTE_TASK_DIALOG_DIALOG_SETTINGS);
		if (dlgSettings == null) {
			dlgSettings = settings.addNewSection(OPEN_REMOTE_TASK_DIALOG_DIALOG_SETTINGS);
		}
		dlg.setDialogBoundsSettings(dlgSettings, Dialog.DIALOG_PERSISTLOCATION | Dialog.DIALOG_PERSISTSIZE);

		if (dlg.open() == Window.OK) {
			if (dlg.getSelectedTask() != null) {
				openExistingTask(dlg);
			} else {
				openRemoteTask(dlg);
			}
		}
	}

	/**
	 * Selected a existing task; handle category move, if needed
	 */
	private void openExistingTask(RemoteTaskSelectionDialog dlg) {
		if (dlg.shouldAddToTaskList()) {
			TasksUiPlugin.getTaskListManager().getTaskList().moveToContainer(dlg.getSelectedTask(),
					dlg.getSelectedCategory());
		}
		TasksUiUtil.refreshAndOpenTaskListElement(dlg.getSelectedTask());
	}

	/**
	 * Selected a repository, so try to obtain the task using taskId
	 */
	private void openRemoteTask(RemoteTaskSelectionDialog dlg) {
		String[] selectedIds = dlg.getSelectedIds();
		if (dlg.shouldAddToTaskList()) {
			for (String id : selectedIds) {
				final IProgressService svc = PlatformUI.getWorkbench().getProgressService();
				final AddExistingTaskJob job = new AddExistingTaskJob(dlg.getSelectedTaskRepository(), id, dlg
						.getSelectedCategory());
				job.schedule();
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

					public void run() {
						svc.showInDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), job);
					}

				});
			}
		} else {
			boolean openSuccessful = false;
			for (String id : selectedIds) {
				boolean opened = TasksUiUtil.openRepositoryTask(dlg.getSelectedTaskRepository(), id);
				if (opened) {
					openSuccessful = true;
				}
			}
			if (!openSuccessful) {
				MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
						ITasksUiConstants.TITLE_DIALOG, 
						"Could not find matching repository task.");
			}
		}
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
