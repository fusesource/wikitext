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

package org.eclipse.mylar.internal.bugzilla.ui.tasklist;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaClient;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaRepositoryQuery;
import org.eclipse.mylar.internal.bugzilla.ui.search.BugzillaSearchPage;
import org.eclipse.mylar.internal.bugzilla.ui.wizard.NewBugzillaTaskWizard;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.OpenRepositoryTaskJob;
import org.eclipse.mylar.tasks.ui.search.AbstractRepositoryQueryPage;
import org.eclipse.mylar.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * @author Mik Kersten
 */
public class BugzillaRepositoryUi extends AbstractRepositoryConnectorUi {

	@Override
	public AbstractRepositorySettingsPage getSettingsPage() {
		return new BugzillaRepositorySettingsPage(this);
	}

	@Override
	public AbstractRepositoryQueryPage getSearchPage(TaskRepository repository, IStructuredSelection selection) {
		return new BugzillaSearchPage(repository);
	}

	@Override
	public IWizard getNewTaskWizard(TaskRepository taskRepository) {
		return new NewBugzillaTaskWizard(taskRepository);
	}

	@Override
	public IWizard getQueryWizard(TaskRepository repository, AbstractRepositoryQuery query) {
		if (query instanceof BugzillaRepositoryQuery) {
			return new EditBugzillaQueryWizard(repository, (BugzillaRepositoryQuery) query);
		} else {
			return new NewBugzillaQueryWizard(repository);
		}
	}

	@Override
	public boolean hasSearchPage() {
		return true;
	}

	@Override
	public boolean hasRichEditor() {
		return true;
	}

	@Override
	public String getRepositoryType() {
		return BugzillaCorePlugin.REPOSITORY_KIND;
	}

	@Override
	public boolean openRemoteTask(String repositoryUrl, String idString) {
		int id = -1;
		try {
			id = Integer.parseInt(idString);
		} catch (NumberFormatException e) {
			return false;
		}
		String bugUrl = BugzillaClient.getBugUrlWithoutLogin(repositoryUrl, id);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		OpenRepositoryTaskJob job = new OpenRepositoryTaskJob(BugzillaCorePlugin.REPOSITORY_KIND, repositoryUrl, idString,
				bugUrl, page);
		job.schedule();
		return true;
	}

}
