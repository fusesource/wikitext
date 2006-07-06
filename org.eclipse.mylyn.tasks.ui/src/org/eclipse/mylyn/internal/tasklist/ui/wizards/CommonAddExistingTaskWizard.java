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

package org.eclipse.mylar.internal.tasklist.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylar.internal.tasklist.ui.views.TaskListView;
import org.eclipse.mylar.provisional.tasklist.AbstractRepositoryConnector;
import org.eclipse.mylar.provisional.tasklist.ITask;
import org.eclipse.mylar.provisional.tasklist.MylarTaskListPlugin;
import org.eclipse.mylar.provisional.tasklist.TaskCategory;
import org.eclipse.mylar.provisional.tasklist.TaskRepository;
import org.eclipse.ui.IWorkbench;

/**
 * @author Brock Janiczak
 * @author Mik Kersten
 */
public class CommonAddExistingTaskWizard extends Wizard {

	private final TaskRepository repository;

	private ExistingTaskWizardPage page;

	public CommonAddExistingTaskWizard(TaskRepository repository) {
		this.repository = repository;
		setNeedsProgressMonitor(true);
		init();
	}

	@Override
	public final boolean performFinish() {
		AbstractRepositoryConnector connector = MylarTaskListPlugin.getRepositoryManager().getRepositoryConnector(
				this.repository.getKind());
		ITask newTask = connector.createTaskFromExistingKey(repository, getTaskId());

		if (newTask != null && TaskListView.getFromActivePerspective() != null) {
			Object selectedObject = ((IStructuredSelection) TaskListView.getFromActivePerspective().getViewer()
					.getSelection()).getFirstElement();

			if (selectedObject instanceof TaskCategory) {
				MylarTaskListPlugin.getTaskListManager().getTaskList().moveToContainer(((TaskCategory) selectedObject),
						newTask);
			} else {
				MylarTaskListPlugin.getTaskListManager().getTaskList().moveToRoot(newTask);
			}
			if (TaskListView.getFromActivePerspective() != null) {
				TaskListView.getFromActivePerspective().getViewer().setSelection(new StructuredSelection(newTask));
			}
		}

		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	private void init() {
		super.setForcePreviousAndNextButtons(true);
	}

	public void addPages() {
		super.addPages();
		this.page = new ExistingTaskWizardPage();
		addPage(page);
	}

	protected String getTaskId() {
		return page.getTaskId();
	}
}
