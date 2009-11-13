/*******************************************************************************
 * Copyright (c) 2004, 2009 Maarten Meijer and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Maarten Meijer - initial API and implementation
 *     Tasktop Technologies - improvements
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.DefaultTaskMapping;
import org.eclipse.mylyn.internal.tasks.core.LocalTask;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * @author Maarten Meijer
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class CloneTaskAction extends BaseSelectionListenerAction implements IViewActionDelegate {

	private static final String ID = "org.eclipse.mylyn.tasklist.actions.clone"; //$NON-NLS-1$

	protected ISelection selection;

	public CloneTaskAction() {
		super(Messages.CloneTaskAction_Clone_Label);
		setId(ID);
		setImageDescriptor(TasksUiImages.TASK_NEW);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			super.selectionChanged((IStructuredSelection) selection);
		}
	}

	public void run(IAction action) {
		run();
	}

	@Override
	public void run() {
		for (Object selectedObject : getStructuredSelection().toList()) {
			if (selectedObject instanceof AbstractTask) {
				AbstractTask task = (AbstractTask) selectedObject;

				String description = Messages.CloneTaskAction_Cloned_from_ + CopyTaskDetailsAction.getTextForTask(task);
				if (task instanceof LocalTask) {
					String notes = task.getNotes();
					if (!"".equals(notes)) { //$NON-NLS-1$
						description += "\n\n" + notes; //$NON-NLS-1$
					}
				}

				ITaskMapping taskSelection = new DefaultTaskMapping();
				((DefaultTaskMapping) taskSelection).setDescription(description);

				TaskData taskData;
				try {
					taskData = TasksUi.getTaskDataManager().getTaskData(task);
				} catch (CoreException e) {
					TasksUiInternal.displayStatus(Messages.CloneTaskAction_Clone_Task_Failed, e.getStatus());
					continue;
				}

				if (taskData != null) {
					AbstractRepositoryConnector connector = TasksUi.getRepositoryConnector(taskData.getConnectorKind());
					ITaskMapping mapping = connector.getTaskMapping(taskData);
					if (mapping.getDescription() != null) {
						((DefaultTaskMapping) taskSelection).setDescription(description + "\n\n" //$NON-NLS-1$
								+ mapping.getDescription());

						TaskAttribute attrDescription = mapping.getTaskData().getRoot().getMappedAttribute(
								TaskAttribute.DESCRIPTION);
						if (attrDescription != null) {
							attrDescription.getMetaData().setReadOnly(false);
						}

					}
					mapping.merge(taskSelection);
					taskSelection = mapping;
				}

				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				if (!TasksUiUtil.openNewTaskEditor(shell, taskSelection, null)) {
					// do not process other tasks if canceled
					return;
				}
			}
		}
	}

	public void init(IViewPart view) {
		// ignore
	}

}
