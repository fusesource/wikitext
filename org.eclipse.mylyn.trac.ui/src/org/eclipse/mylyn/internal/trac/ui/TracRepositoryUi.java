/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.trac.ui;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylar.internal.trac.core.ITracClient;
import org.eclipse.mylar.internal.trac.core.TracCorePlugin;
import org.eclipse.mylar.internal.trac.core.TracRepositoryConnector;
import org.eclipse.mylar.internal.trac.core.TracRepositoryQuery;
import org.eclipse.mylar.internal.trac.ui.wizard.EditTracQueryWizard;
import org.eclipse.mylar.internal.trac.ui.wizard.NewTracQueryWizard;
import org.eclipse.mylar.internal.trac.ui.wizard.NewTracTaskWizard;
import org.eclipse.mylar.internal.trac.ui.wizard.TracCustomQueryPage;
import org.eclipse.mylar.internal.trac.ui.wizard.TracRepositorySettingsPage;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.OpenRepositoryTaskJob;
import org.eclipse.mylar.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.mylar.tasks.ui.wizards.NewWebTaskWizard;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class TracRepositoryUi extends AbstractRepositoryConnectorUi {

	@Override
	public AbstractRepositorySettingsPage getSettingsPage() {
		return new TracRepositorySettingsPage(this);
	}
	
	@Override
	public boolean hasRichEditor() {
		return true;
	}

	@Override
	public WizardPage getSearchPage(TaskRepository repository, IStructuredSelection selection) {
		return new TracCustomQueryPage(repository);
	}

	@Override
	public boolean hasSearchPage() {
		return true;
	}
	
	@Override
	public IWizard getNewTaskWizard(TaskRepository repository) {
		if (TracRepositoryConnector.hasRichEditor(repository)) {
			return new NewTracTaskWizard(repository);
		} else {
			return new NewWebTaskWizard(repository, repository.getUrl() + ITracClient.NEW_TICKET_URL);
		}
	}
	
	@Override
	public IWizard getQueryWizard(TaskRepository repository, AbstractRepositoryQuery query) {
		if (query instanceof TracRepositoryQuery) {
			return new EditTracQueryWizard(repository, query);
		} else {
			return new NewTracQueryWizard(repository);
		}
	}

	@Override
	public String getRepositoryType() {
		return TracCorePlugin.REPOSITORY_KIND;
	}

	@Override
	public boolean openRemoteTask(String repositoryUrl, String idString) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		String ticketUrl = repositoryUrl + ITracClient.TICKET_URL + idString;
		OpenRepositoryTaskJob job = new OpenRepositoryTaskJob(TracCorePlugin.REPOSITORY_KIND, repositoryUrl, idString, ticketUrl, page);
		job.schedule();
		return true;
	}
	
}
