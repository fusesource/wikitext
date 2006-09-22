/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.trac.ui.wizard;

import org.eclipse.mylar.internal.tasks.ui.wizards.AbstractEditQueryWizard;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.TaskRepository;

/**
 * @author Steffen Pingel
 */
public class EditTracQueryWizard extends AbstractEditQueryWizard {

//	private TracCustomQueryPage queryPage;

	public EditTracQueryWizard(TaskRepository repository, AbstractRepositoryQuery query) {
		super(repository, query);
	}

	@Override
	public void addPages() {
		page = new TracCustomQueryPage(repository, query);
		page.setWizard(this);
		addPage(page);
	}

	@Override
	public boolean canFinish() {
		if (page.getNextPage() == null) {
			return page.isPageComplete();
		}
		return page.getNextPage().isPageComplete();
	}

//	@Override
//	public boolean performFinish() {
//		AbstractRepositoryQuery q = queryPage.getQuery();
//		if (q != null) {
//			TasksUiPlugin.getTaskListManager().getTaskList().deleteQuery(query);
//			TasksUiPlugin.getTaskListManager().getTaskList().addQuery(q);
//
//			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
//					repository.getKind());
//			if (connector != null) {
//				TasksUiPlugin.getSynchronizationManager().synchronize(connector, q, null);
//			}
//		}
//
//		return true;
//	}

}