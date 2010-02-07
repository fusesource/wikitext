/*******************************************************************************
 * Copyright (c) 2010 Frank Becker and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.ui.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.bugzilla.ui.BugzillaUiPlugin;
import org.eclipse.mylyn.internal.bugzilla.ui.action.ChangeAttachmentJob;
import org.eclipse.mylyn.internal.bugzilla.ui.editor.BugzillaTaskEditorPage;
import org.eclipse.mylyn.tasks.core.ITaskAttachment;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.IFormPage;

public class BugzillaAttachmentWizard extends Wizard {

	private final AttributeEditorFactory factory;

	private final TaskAttribute attachmentAttribute;

	private BugzillaAttachmentWizardPage attachmentWizardPage;

	private final Shell parentShell;

	private boolean changed = false;

	private final TaskEditor taskEditor;

	private final ITaskAttachment attachment;

	public BugzillaAttachmentWizard(Shell parentShell, AttributeEditorFactory factory,
			TaskAttribute attachmentAttribute, TaskEditor taskEditor, ITaskAttachment attachment) {
		super();
		this.factory = factory;
		this.attachmentAttribute = attachmentAttribute;
		this.parentShell = parentShell;
		this.taskEditor = taskEditor;
		this.attachment = attachment;
		setNeedsProgressMonitor(true);

	}

	@Override
	public boolean performFinish() {
		TaskAttribute attachmentTaskAttribute = attachment.getTaskAttribute();
		for (TaskAttribute child : attachmentAttribute.getAttributes().values()) {
			attachmentTaskAttribute.deepAddCopy(child);
		}

		final ChangeAttachmentJob job = new ChangeAttachmentJob(attachment, taskEditor);
		job.setUser(true);
		if (attachmentWizardPage.runInBackground()) {
			runInBackground(job);
		} else {
			runInWizard(job);
		}
		return true;
	}

/* currently not needed
	private void handleDone(final ChangeAttachmentJob job, IProgressMonitor monitor) {
		try {
			if (job.getError() != null) {
				IFormPage formPage = taskEditor.getActivePageInstance();
				if (formPage instanceof BugzillaTaskEditorPage) {
					final BugzillaTaskEditorPage bugzillaPage = (BugzillaTaskEditorPage) formPage;
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						public void run() {
							bugzillaPage.getTaskEditor()
									.setMessage(job.getError().getMessage(), IMessageProvider.ERROR);
						}
					});
				}
			} else {
				monitor.setTaskName(Messages.BugzillaAttachmentWizard_Now_synchronize_the_Task);
				IFormPage formPage = taskEditor.getActivePageInstance();
				if (formPage instanceof BugzillaTaskEditorPage) {
					final BugzillaTaskEditorPage bugzillaPage = (BugzillaTaskEditorPage) formPage;
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						public void run() {
							bugzillaPage.refreshFormContent();
						}
					});
				}
			}
		} finally {
			monitor.done();
		}
	}
*/
	
	private boolean runInWizard(final ChangeAttachmentJob job) {
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					job.run(monitor);
				}
			});
			return true;
		} catch (InvocationTargetException e) {
			StatusHandler.fail(new Status(IStatus.ERROR, BugzillaUiPlugin.ID_PLUGIN, "Unexpected error", e)); //$NON-NLS-1$
			return false;
		} catch (InterruptedException e) {
			// canceled
			return false;
		}
	}

	private void runInBackground(final ChangeAttachmentJob job) {
		job.schedule();
	}

	@Override
	public void addPages() {
		attachmentWizardPage = new BugzillaAttachmentWizardPage(parentShell, factory, attachmentAttribute,
				attachment.getTask().getTaskId());
		addPage(attachmentWizardPage);
	}

	@Override
	public boolean canFinish() {
		return isChanged();
	}

	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

}
