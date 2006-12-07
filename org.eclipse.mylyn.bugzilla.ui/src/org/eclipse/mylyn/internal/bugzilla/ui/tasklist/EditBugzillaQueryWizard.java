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

import org.eclipse.mylar.internal.bugzilla.core.BugzillaRepositoryQuery;
import org.eclipse.mylar.internal.bugzilla.ui.search.BugzillaSearchPage;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.wizards.AbstractEditQueryWizard;

/**
 * @author Rob Elves
 */
public class EditBugzillaQueryWizard extends AbstractEditQueryWizard {

	public EditBugzillaQueryWizard(TaskRepository repository, BugzillaRepositoryQuery query) {
		super(repository, query);
	}

	@Override
	public void addPages() {
		if (((BugzillaRepositoryQuery) query).isCustomQuery()) {
			page = new BugzillaCustomQueryWizardPage(repository, (BugzillaRepositoryQuery) query);
		} else {
			page = new BugzillaSearchPage(repository, (BugzillaRepositoryQuery) query);
		}
		addPage(page);
	}

	@Override
	public boolean canFinish() {
		if (page != null && page.isPageComplete()) {
			return true;
		}
		return false;
	}
}
