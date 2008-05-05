/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.context.ui.wizards;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.internal.context.ui.ContextUiUtil;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryAttachment;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * @author Rob Elves
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class ContextRetrieveWizard extends Wizard {

	private static final String TITLE = "Task Repository";

	private final TaskRepository repository;

	private final ITask task;

	private ContextRetrieveWizardPage wizardPage;

	public ContextRetrieveWizard(ITask task) {
		repository = TasksUi.getRepositoryManager().getRepository(task.getConnectorKind(), task.getRepositoryUrl());
		this.task = task;
		setWindowTitle(TITLE);
		setDefaultPageImageDescriptor(TasksUiImages.BANNER_REPOSITORY_CONTEXT);
	}

	@Override
	public void addPages() {
		wizardPage = new ContextRetrieveWizardPage(repository, task);
		addPage(wizardPage);
		super.addPages();
	}

	@Override
	public final boolean performFinish() {
		RepositoryAttachment attachment = wizardPage.getSelectedContext();
		return ContextUiUtil.downloadContext(task, attachment, getContainer());
	}

}
