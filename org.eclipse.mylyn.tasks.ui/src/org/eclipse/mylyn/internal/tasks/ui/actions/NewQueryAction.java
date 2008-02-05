/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.mylyn.internal.tasks.ui.LocalTaskConnectorUi;
import org.eclipse.mylyn.internal.tasks.ui.wizards.NewQueryWizard;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

/**
 * @author Mik Kersten
 * @author Eugene Kuleshov
 */
public class NewQueryAction extends Action implements IViewActionDelegate, IExecutableExtension {

	private static final String WIZARD_LABEL = "Add or modify repository query";

	private boolean skipRepositoryPage;

	public void run(IAction action) {
		IWizard wizard = null;
		List<TaskRepository> repositories = TasksUiPlugin.getRepositoryManager().getAllRepositories();
		if (repositories.size() == 2) {
			// NOTE: this click-saving should be generalized
			for (TaskRepository taskRepository : repositories) {
				AbstractRepositoryConnectorUi connectorUi = TasksUiPlugin.getConnectorUi(taskRepository.getConnectorKind());
				if (!(connectorUi instanceof LocalTaskConnectorUi)) {
					wizard = connectorUi.getQueryWizard(taskRepository, null);
					if (wizard == null) {
						continue;
					}
					((Wizard) wizard).setForcePreviousAndNextButtons(true);
				}
			}
		} else if (skipRepositoryPage) {
			TaskRepository taskRepository = TasksUiUtil.getSelectedRepository();
			AbstractRepositoryConnectorUi connectorUi = TasksUiPlugin.getConnectorUi(taskRepository.getConnectorKind());
			wizard = connectorUi.getQueryWizard(taskRepository, null);
			((Wizard) wizard).setForcePreviousAndNextButtons(true);
			if (connectorUi instanceof LocalTaskConnectorUi) {
				wizard.performFinish();
				return;
			}
		} else {
			wizard = new NewQueryWizard();
		}

		try {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			if (shell != null && !shell.isDisposed()) {
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.create();
				dialog.setTitle(WIZARD_LABEL);
				dialog.setBlockOnOpen(true);
				if (dialog.open() == Dialog.CANCEL) {
					dialog.close();
					return;
				}
			}
		} catch (Exception e) {
			StatusHandler.fail(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, e.getMessage(), e));
		}
	}

	public void init(IViewPart view) {
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		if ("skipFirstPage".equals(data)) {
			this.skipRepositoryPage = true;
		}
	}

}
