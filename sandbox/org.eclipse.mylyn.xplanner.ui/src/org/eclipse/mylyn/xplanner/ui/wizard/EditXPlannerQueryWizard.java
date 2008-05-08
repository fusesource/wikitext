/*******************************************************************************
 * Copyright (c) 2007 - 2007 CodeGear and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.xplanner.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractLegacyRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryWizard;
import org.eclipse.mylyn.xplanner.ui.XPlannerCustomQuery;

/**
 * @author Ravi Kumar
 * @author Helen Bershadskaya
 */
public class EditXPlannerQueryWizard extends AbstractRepositoryQueryWizard {

	private AbstractXPlannerQueryWizardPage queryPage;

	public EditXPlannerQueryWizard(TaskRepository repository, IRepositoryQuery query) {
		super(repository, query);
		setForcePreviousAndNextButtons(true);
	}

	@Override
	public void addPages() {
		queryPage = XPlannerQueryWizardUtils.addQueryWizardFirstPage(this, repository, (XPlannerCustomQuery) query);
	}

	@Override
	public boolean performFinish() {
		List<RepositoryQuery> queries = new ArrayList<RepositoryQuery>();

		// always delete existing query, because new one(s) will get created below
		TasksUi.getTasksModel().deleteQuery(query);

		if (queryPage instanceof MultipleQueryPage) {
			queries = ((MultipleQueryPage) queryPage).getQueries();
		} else {
			final RepositoryQuery query = (RepositoryQuery) queryPage.getQuery();
			if (query != null) {
				queries.add(query);
			}
		}

		for (RepositoryQuery query : queries) {
			updateQuery(query);
		}

		return true;
	}

	private void updateQuery(final RepositoryQuery query) {
		// just in case one with this definition already exists...
		TasksUiInternal.getTaskList().deleteQuery(query);
		// make sure query reflects changed name, if it was changed
		if (query instanceof XPlannerCustomQuery) {
			XPlannerCustomQuery xplannerQuery = (XPlannerCustomQuery) query;
			String handleIdentifier = xplannerQuery.getHandleIdentifier();
			String queryName = xplannerQuery.getQueryName();
			if (!handleIdentifier.equals(queryName)) {
				xplannerQuery.setHandleIdentifier(queryName);
			}
		}
		TasksUiInternal.getTaskList().addQuery(query);

		AbstractLegacyRepositoryConnector connector = (AbstractLegacyRepositoryConnector) TasksUi.getRepositoryManager()
				.getRepositoryConnector(repository.getConnectorKind());
		if (connector != null) {
			TasksUiInternal.synchronizeQuery(connector, query, null, true);
		}
	}

	@Override
	public boolean canFinish() {
		boolean canFinish = false;
		if (queryPage != null) {
			if (queryPage.getNextPage() == null) {
				canFinish = queryPage.isPageComplete();
			} else {
				canFinish = queryPage.getNextPage().isPageComplete();
			}
		}

		return canFinish;
	}
}
