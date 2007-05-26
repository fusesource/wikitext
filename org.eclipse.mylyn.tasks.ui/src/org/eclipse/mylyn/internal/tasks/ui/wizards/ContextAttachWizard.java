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
import org.eclipse.mylar.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.ContextUiUtil;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * @author Rob Elves
 * @author Steffen Pingel
 */
public class ContextAttachWizard extends Wizard {

	public static final String WIZARD_TITLE = "Attach context";

	private final TaskRepository repository;

	private final AbstractRepositoryTask task;

	private ContextAttachWizardPage wizardPage;

	public ContextAttachWizard(AbstractRepositoryTask task) {
		repository = TasksUiPlugin.getRepositoryManager().getRepository(task.getRepositoryKind(),
				task.getRepositoryUrl());
		this.task = task;
		setWindowTitle(ContextRetrieveWizard.TITLE);
		setDefaultPageImageDescriptor(TasksUiImages.BANNER_REPOSITORY_CONTEXT);
	}

	@Override
	public void addPages() {
		wizardPage = new ContextAttachWizardPage(repository, task);
		addPage(wizardPage);
		super.addPages();
	}

	@Override
	public final boolean performFinish() {
		return ContextUiUtil.uploadContext(repository, task, wizardPage.getComment(), getContainer());
	}

}
