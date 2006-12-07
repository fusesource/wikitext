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

package org.eclipse.mylar.internal.tasks.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylar.internal.tasks.ui.actions.AddRepositoryAction;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.ui.IWorkbench;

/**
 * @author Mik Kersten
 */
public class NewRepositoryWizard extends AbstractRepositoryClientWizard {

	public NewRepositoryWizard() {
		this(null);
	}

	public NewRepositoryWizard(String repositoryType) {
		super(repositoryType);
		setForcePreviousAndNextButtons(true);
		setWindowTitle(AddRepositoryAction.TITLE);
	}

	@Override
	public boolean performFinish() {
		if (canFinish()) {
			TaskRepository repository = abstractRepositorySettingsPage.createTaskRepository();
			abstractRepositorySettingsPage.updateProperties(repository);
			TasksUiPlugin.getRepositoryManager().addRepository(repository,
					TasksUiPlugin.getDefault().getRepositoriesFilePath());
			return true;
		}
		return false;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void setRepositorySettingsPage(AbstractRepositorySettingsPage abstractRepositorySettingsPage) {
		this.abstractRepositorySettingsPage = abstractRepositorySettingsPage;
	}

	@Override
	public boolean canFinish() {
		return super.canFinish() && abstractRepositorySettingsPage != null
				&& abstractRepositorySettingsPage.isPageComplete();
	}
}
