/*******************************************************************************
 * Copyright (c) 2004 - 20067 Mylyn committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.search;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.search.SearchHitCollector;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.PlatformUI;

/**
 * Used for add the last search result to the Task List.
 * 
 * @author Balazs Brinkus (bug 172699)
 * @author Mik Kersten
 */
public class CreateQueryFromSearchAction extends Action {

	/** The view this action works on */
	private final RepositorySearchResultView resultView;

	/**
	 * Constructor
	 * 
	 * @param text
	 *            The text for this action
	 * @param resultView
	 *            The <code>RepositorySearchResultView</code> this action works on
	 */
	public CreateQueryFromSearchAction(String text, RepositorySearchResultView resultView) {
		setText(text);
		setImageDescriptor(TasksUiImages.QUERY_NEW);
		this.resultView = resultView;
	}

	/**
	 * Add the search result to the Task List.
	 */
	@Override
	public void run() {
		ISelection selection = resultView.getViewer().getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.getFirstElement() instanceof AbstractTask) {
				ISearchQuery[] queries = NewSearchUI.getQueries();
				AbstractTask task = (AbstractTask) structuredSelection.getFirstElement();
				AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
						task.getConnectorKind());
				if (queries.length != 0 && connector != null) {
					SearchHitCollector searchHitCollector = (SearchHitCollector) queries[0];
					AbstractRepositoryQuery query = searchHitCollector.getRepositoryQuery();
					InputDialog dialog = new InputDialog(PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow()
							.getShell(), "Create Query", "Name of query to be added to the " + TaskListView.LABEL_VIEW
							+ ": ", "", null);
					int dialogResult = dialog.open();
					if (dialogResult == Window.OK) {
						query.setHandleIdentifier(dialog.getValue());
						TasksUiPlugin.getTaskListManager().getTaskList().addQuery(query);
						TasksUi.synchronizeQuery(connector, query, null, true);
					}
				}
			}
		}
	}

}
