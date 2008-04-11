/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.wizards;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.mylyn.internal.tasks.core.ITaskRepositoryFilter;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;

/**
 * @author Mik Kersten
 * @author Eugene Kuleshov
 */
public class NewQueryWizard extends MultiRepositoryAwareWizard {

	private static final String TITLE = "New Repository Query";

	public NewQueryWizard() {
		super(new SelectRepositoryPageForNewQuery(), TITLE);
	}

	private static final class SelectRepositoryPageForNewQuery extends SelectRepositoryPage {
		public SelectRepositoryPageForNewQuery() {
			super(ITaskRepositoryFilter.CAN_QUERY);
		}

		@Override
		protected IWizard createWizard(TaskRepository taskRepository) {
			AbstractRepositoryConnectorUi repositoryUi = TasksUiPlugin.getConnectorUi(taskRepository.getConnectorKind());
			return repositoryUi.getQueryWizard(taskRepository, null);
		}
	}

}
