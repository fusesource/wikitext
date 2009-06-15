/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylyn.internal.provisional.commons.ui.SelectionProviderAdapter;
import org.eclipse.mylyn.internal.provisional.commons.ui.WorkbenchActionSupport;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;

/**
 * @author Steffen Pingel
 */
public class TaskEditorActionGroup extends RepositoryElementActionGroup {

	private final WorkbenchActionSupport actionSupport;

	private final SynchronizeEditorAction synchronizeEditorAction = new SynchronizeEditorAction();

	private final NewTaskFromSelectionAction newTaskFromSelectionAction = new NewTaskFromSelectionAction();

	public TaskEditorActionGroup(WorkbenchActionSupport actionSupport) {
		this.actionSupport = actionSupport;
		synchronizeEditorAction.setActionDefinitionId("org.eclipse.ui.file.refresh"); //$NON-NLS-1$
		synchronizeEditorAction.setEnabled(false);
	}

	public void fillContextMenu(IMenuManager manager, TaskEditor editor, boolean addClipboard) {
		ITask task = editor.getTaskEditorInput().getTask();
		SelectionProviderAdapter selectionProvider = new SelectionProviderAdapter();
		setSelectionProvider(selectionProvider);
		selectionProvider.setSelection(new StructuredSelection(task));

		super.fillContextMenu(manager);

		if (addClipboard) {
			addClipboardActions(manager);
		}
		synchronizeEditorAction.selectionChanged(new StructuredSelection(editor));

		IStructuredSelection selection = new StructuredSelection(task);
		actionSupport.updateActions(selection);
		newTaskFromSelectionAction.selectionChanged(selection);

		manager.add(new Separator());
		if (synchronizeEditorAction.isEnabled()) {
			manager.appendToGroup(ID_SEPARATOR_REPOSITORY, synchronizeEditorAction);
		}
	}

	public void addClipboardActions(IMenuManager manager) {
		//manager.add(actionSupport.getUndoAction());
		//manager.add(actionSupport.getRedoAction());
		//manager.add(new Separator());
		manager.prependToGroup(ID_SEPARATOR_EDIT, actionSupport.getCopyAction());
		manager.prependToGroup(ID_SEPARATOR_EDIT, actionSupport.getCutAction());
		manager.appendToGroup(ID_SEPARATOR_EDIT, actionSupport.getPasteAction());
		manager.appendToGroup(ID_SEPARATOR_EDIT, actionSupport.getSelectAllAction());
		manager.appendToGroup(ID_SEPARATOR_EDIT, newTaskFromSelectionAction);
	}

	public SynchronizeEditorAction getSynchronizeEditorAction() {
		return synchronizeEditorAction;
	}

	public NewTaskFromSelectionAction getNewTaskFromSelectionAction() {
		return newTaskFromSelectionAction;
	}

}
