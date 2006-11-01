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

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylar.internal.tasks.ui.TaskListImages;
import org.eclipse.mylar.internal.tasks.ui.search.AbstractRepositoryQueryPage;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * @author Mik Kersten
 */
public abstract class AbstractEditQueryWizard extends Wizard {

	private static final String TITLE = "Edit Repository Query";

	protected final TaskRepository repository;

	protected AbstractRepositoryQuery query;

	protected AbstractRepositoryQueryPage page;

	public AbstractEditQueryWizard(TaskRepository repository, AbstractRepositoryQuery query) {
		this.repository = repository;
		this.query = query;
		setNeedsProgressMonitor(true);
		setWindowTitle(TITLE);
		setDefaultPageImageDescriptor(TaskListImages.BANNER_REPOSITORY);
	}

	@Override
	public boolean performFinish() {
		AbstractRepositoryQuery queryToRun = null;
		if (page != null) {
			// TODO: get rid of this?
			queryToRun = page.getQuery();
		} else {
			queryToRun = this.query;
		}
		if (queryToRun != null) {
			TasksUiPlugin.getTaskListManager().getTaskList().deleteQuery(queryToRun);
			TasksUiPlugin.getTaskListManager().getTaskList().addQuery(queryToRun);

			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
					repository.getKind());
			if (connector != null) {
				TasksUiPlugin.getSynchronizationManager().synchronize(connector, queryToRun, null);
			}
		}

		return true;
	}
}
