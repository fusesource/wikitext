/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.ui.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylar.internal.tasks.ui.wizards.NewAttachmentWizard;
import org.eclipse.mylar.internal.tasks.ui.wizards.NewAttachmentWizardDialog;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.editors.TaskEditor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * @author Mik Kersten
 */
public class AttachFileAction extends BaseSelectionListenerAction {

	private TaskEditor editor;

	public AttachFileAction() {
		super("Attach File...");
		setId("org.eclipse.mylar.tasks.ui.actions.add.attachment");
	}

	@Override
	public void run() {
		if (editor != null) {
			editor.showBusy(true);
		}
		Object selection = super.getStructuredSelection().getFirstElement();
		if (selection instanceof AbstractRepositoryTask) {
			AbstractRepositoryTask repositoryTask = (AbstractRepositoryTask) selection;
			TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(
					repositoryTask.getRepositoryKind(), repositoryTask.getRepositoryUrl());

			NewAttachmentWizard attachmentWizard = new NewAttachmentWizard(repository, repositoryTask);
			NewAttachmentWizardDialog dialog = new NewAttachmentWizardDialog(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getShell(), attachmentWizard);
			attachmentWizard.setDialog(dialog);
			dialog.create();
			int result = dialog.open();
			if (result != MessageDialog.OK && editor != null) {
				editor.showBusy(false);
			}
		}
	}

	public void setEditor(TaskEditor taskEditor) {
		this.editor = taskEditor;
	}
}
