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

package org.eclipse.mylar.internal.tasks.ui.actions;

import java.util.Iterator;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.mylar.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.editors.AbstractRepositoryTaskEditor;
import org.eclipse.mylar.tasks.ui.editors.TaskEditor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * @author Rob Elves
 */
public class SynchronizeEditorAction extends BaseSelectionListenerAction {

	private static final String LABEL = "Synchronize";

	public static final String ID = "org.eclipse.mylar.tasklist.actions.synchronize.editor";

	public SynchronizeEditorAction() {
		super(LABEL);
		setToolTipText(LABEL);
		setId(ID);
		setImageDescriptor(TasksUiImages.REFRESH_SMALL);
		// setAccelerator(SWT.MOD1 + 'r');
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		if (super.getStructuredSelection() != null) {
			for (Iterator iter = super.getStructuredSelection().iterator(); iter.hasNext();) {
				runWithSelection(iter.next());
			}
		}
	}

	private void runWithSelection(final Object selectedObject) {
		AbstractRepositoryTask repositoryTask = null;
		if (selectedObject instanceof TaskEditor) {
			TaskEditor editor = (TaskEditor) selectedObject;
			ITask task = editor.getTaskEditorInput().getTask();
			if (task instanceof AbstractRepositoryTask) {
				repositoryTask = (AbstractRepositoryTask) task;
			}
		} else if (selectedObject instanceof AbstractRepositoryTaskEditor) {
			AbstractRepositoryTaskEditor editor = (AbstractRepositoryTaskEditor) selectedObject;
			repositoryTask = editor.getRepositoryTask();
		}

		if (repositoryTask != null) {
			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
					repositoryTask.getRepositoryKind());
			if (connector != null) {
				TasksUiPlugin.getSynchronizationManager().synchronize(connector, repositoryTask, false,
						new JobChangeAdapter() {

							@Override
							public void done(IJobChangeEvent event) {
								PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

									public void run() {
										if (selectedObject instanceof TaskEditor) {
											TaskEditor editor = (TaskEditor) selectedObject;
											editor.refreshEditorContents();
										} else if (selectedObject instanceof AbstractRepositoryTaskEditor) {
											((AbstractRepositoryTaskEditor) selectedObject).refreshEditor();
										}
									}
								});

							}
						});
			}
		}

	}
}
