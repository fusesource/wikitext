/*******************************************************************************
 * Copyright (c) 2004, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Peter Stibrany - fix for bug 247077
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.actions.AddRepositoryAction;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskRepositoryPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class NewRepositoryWizard extends Wizard implements INewWizard {

	private AbstractRepositoryConnector connector;

	/**
	 * If not null, indicates that the wizard will initially jump to a specific connector page
	 */
	private final String connectorKind;

	private TaskRepository taskRepository;

	private SelectRepositoryConnectorPage selectConnectorPage;

	private ITaskRepositoryPage settingsPage;

	private String lastConnectorKind;

	public NewRepositoryWizard() {
		this(null);
	}

	public NewRepositoryWizard(String connectorKind) {
		this.connectorKind = connectorKind;
		setDefaultPageImageDescriptor(TasksUiImages.BANNER_REPOSITORY);
		setForcePreviousAndNextButtons(connectorKind == null);
		setNeedsProgressMonitor(true);
		setWindowTitle(AddRepositoryAction.TITLE);
	}

	@Override
	public void addPages() {
		if (connectorKind != null
				&& TasksUi.getRepositoryManager().getRepositoryConnector(connectorKind).canCreateRepository()) {
			connector = TasksUi.getRepositoryManager().getRepositoryConnector(connectorKind);
			updateSettingsPage();
			if (settingsPage != null) {
				addPage(settingsPage);
			}
		} else {
			selectConnectorPage = new SelectRepositoryConnectorPage();
			addPage(selectConnectorPage);
		}
	}

	@Override
	public boolean canFinish() {
		return (selectConnectorPage == null || selectConnectorPage.isPageComplete())
				&& !(getContainer().getCurrentPage() == selectConnectorPage) && settingsPage != null
				&& settingsPage.isPageComplete();
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == selectConnectorPage) {
			connector = selectConnectorPage.getConnector();
			updateSettingsPage();
			return settingsPage;
		}
		return super.getNextPage(page);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public boolean performFinish() {
		if (canFinish()) {
			taskRepository = new TaskRepository(connector.getConnectorKind(), settingsPage.getRepositoryUrl());
			settingsPage.applyTo(taskRepository);
			TasksUi.getRepositoryManager().addRepository(taskRepository);
			return true;
		}
		return false;
	}

	public TaskRepository getTaskRepository() {
		return taskRepository;
	}

	private void updateSettingsPage() {
		assert connector != null;
		if (!connector.getConnectorKind().equals(lastConnectorKind)) {
			AbstractRepositoryConnectorUi connectorUi = TasksUiPlugin.getConnectorUi(connector.getConnectorKind());
			settingsPage = connectorUi.getSettingsPage(null);
			if (settingsPage == null) {
				TasksUiInternal.displayFrameworkError(NLS.bind(
						"The connector implementation is incomplete: AbstractRepositoryConnectorUi.getSettingsPage() for connector ''{0}'' returned null. Please contact the vendor of the connector to resolve the problem.", connector.getConnectorKind())); //$NON-NLS-1$
			}
			settingsPage.setWizard(this);
			lastConnectorKind = connector.getConnectorKind();
		}
	}

}
