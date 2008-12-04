/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.context.core.InteractionContextManager;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.internal.tasks.ui.TaskListBackupManager;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.util.TaskDataExportOperation;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * Wizard for exporting tasklist data files to the file system. This wizard uses a single page: TaskDataExportWizardPage
 * 
 * @author Wesley Coelho
 * @author Mik Kersten
 */
public class TaskDataExportWizard extends Wizard implements IExportWizard {

	/**
	 * The name of the dialog store's section associated with the task data export wizard
	 */
	private final static String SETTINGS_SECTION = "org.eclipse.mylyn.tasklist.ui.exportWizard"; //$NON-NLS-1$

	private TaskDataExportWizardPage exportPage = null;

	public TaskDataExportWizard() {
		IDialogSettings masterSettings = TasksUiPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.TaskDataExportWizard_Export);
	}

	/**
	 * Finds or creates a dialog settings section that is used to make the dialog control settings persistent
	 */
	public IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings settings = master.getSection(SETTINGS_SECTION);
		if (settings == null) {
			settings = master.addNewSection(SETTINGS_SECTION);
		}
		return settings;
	}

	@Override
	public void addPages() {
		exportPage = new TaskDataExportWizardPage();
		exportPage.setWizard(this);
		addPage(exportPage);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// no initialization needed
	}

	@Override
	public boolean canFinish() {
		return exportPage.isPageComplete();
	}

	/**
	 * Called when the user clicks finish. Saves the task data. Waits until all overwrite decisions have been made
	 * before starting to save files. If any overwrite is canceled, no files are saved and the user must adjust the
	 * dialog.
	 */
	@Override
	public boolean performFinish() {
		boolean overwrite = exportPage.overwrite();
		boolean zip = exportPage.zip();

		Collection<AbstractTask> taskContextsToExport = TasksUiPlugin.getTaskList().getAllTasks();

		// Get file paths to check for existence
		String destDir = exportPage.getDestinationDirectory();
		final File destDirFile = new File(destDir);
		if (!destDirFile.exists() || !destDirFile.isDirectory()) {
			// This should never happen
			StatusHandler.fail(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN,
					"Could not export data because specified location does not exist or is not a folder", //$NON-NLS-1$
					new Exception()));
			return false;
		}

		final File destTaskListFile = new File(destDir + File.separator + ITasksCoreConstants.DEFAULT_TASK_LIST_FILE);
		final File destActivationHistoryFile = new File(destDir + File.separator
				+ InteractionContextManager.CONTEXT_HISTORY_FILE_NAME
				+ InteractionContextManager.CONTEXT_FILE_EXTENSION);
		final File destZipFile = new File(destDir + File.separator + TaskListBackupManager.getBackupFileName());

		// Prompt the user to confirm if ANY of the save repositoryOperations will cause
		// an overwrite
		if (!overwrite) {

			if (zip) {
				if (destZipFile.exists()) {
					if (!MessageDialog.openConfirm(getShell(), Messages.TaskDataExportWizard_Confirm_File_Replace,
							MessageFormat.format(Messages.TaskDataExportWizard_The_zip_file_X_already_exists,
									destZipFile.getPath()))) {
						return false;
					}
				}
			} else {
				if (exportPage.exportTaskList() && destTaskListFile.exists()) {
					if (!MessageDialog.openConfirm(getShell(), Messages.TaskDataExportWizard_Confirm_File_Replace,
							MessageFormat.format(Messages.TaskDataExportWizard_The_task_list_file_X_already_exists,
									destTaskListFile.getPath()))) {
						return false;
					}
				}

				if (exportPage.exportActivationHistory() && destActivationHistoryFile.exists()) {
					if (!MessageDialog.openConfirm(getShell(), Messages.TaskDataExportWizard_Confirm_File_Replace,
							MessageFormat.format(
									Messages.TaskDataExportWizard_The_task_activation_history_file_X_already_exists,
									destActivationHistoryFile.getPath()))) {
						return false;
					}
				}

				if (exportPage.exportTaskContexts()) {
					for (ITask task : taskContextsToExport) {
						File contextFile = ContextCorePlugin.getContextStore().getFileForContext(
								task.getHandleIdentifier());
						File destTaskFile = new File(destDir + File.separator + contextFile.getName());
						if (destTaskFile.exists()) {
							if (!MessageDialog.openConfirm(getShell(),
									Messages.TaskDataExportWizard_Confirm_File_Replace, MessageFormat.format(
											Messages.TaskDataExportWizard_Task_context_files_already_exist_in_X, destDir))) {
								return false;
							} else {
								break;
							}
						}
					}
				}
			}
		}

		// FileCopyJob job = new FileCopyJob(destZipFile, destTaskListFile,
		// destActivationHistoryFile);
		TaskDataExportOperation job = new TaskDataExportOperation(exportPage.getDestinationDirectory(),
				exportPage.exportTaskList(), exportPage.exportActivationHistory(), exportPage.exportTaskContexts(),
				exportPage.zip(), destZipFile.getName(), taskContextsToExport);
		IProgressService service = PlatformUI.getWorkbench().getProgressService();

		try {
			// TODO use the wizard's progress service or IProgressService.busyCursorWhile(): bug 210710 
			service.run(true, false, job);
		} catch (InvocationTargetException e) {
			StatusHandler.fail(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Could not export files", e)); //$NON-NLS-1$
		} catch (InterruptedException e) {
			StatusHandler.fail(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Could not export files", e)); //$NON-NLS-1$
		}

		exportPage.saveSettings();
		return true;
	}
}
