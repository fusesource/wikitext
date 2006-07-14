/*******************************************************************************
 * Copyright (c) 2003 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylar.internal.bugzilla.ui.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylar.context.core.MylarStatusHandler;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaServerFacade;
import org.eclipse.mylar.internal.bugzilla.ui.search.BugzillaSearchResultView;
import org.eclipse.mylar.internal.bugzilla.ui.tasklist.BugzillaQueryHit;
import org.eclipse.mylar.internal.tasks.ui.TaskUiUtil;

/**
 * This class is used to open a bug report in an editor.
 */
public class OpenBugsAction extends Action {

	/** The view this action works on */
	private BugzillaSearchResultView resultView;

	/**
	 * Constructor
	 * 
	 * @param text
	 *            The text for this action
	 * @param resultView
	 *            The <code>BugzillaSearchResultView</code> this action works
	 *            on
	 */
	public OpenBugsAction(String text, BugzillaSearchResultView resultView) {
		setText(text);
		this.resultView = resultView;
	}

	/**
	 * Open the selected bug reports in their own editors.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run() {

		// Get the selected items
		ISelection s = resultView.getViewer().getSelection();
		if (s instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) s;

			// go through each of the selected items and show it in an editor
			for (Iterator<BugzillaQueryHit> it = selection.iterator(); it.hasNext();) {
				BugzillaQueryHit repositoryHit = it.next();
				try {
					int id = Integer.parseInt(repositoryHit.getId());
					String bugUrl = BugzillaServerFacade.getBugUrlWithoutLogin(repositoryHit.getRepositoryUrl(), id);
					TaskUiUtil.openRepositoryTask(repositoryHit.getRepositoryUrl(), "" + repositoryHit.getId(), bugUrl);
				} catch (NumberFormatException e) {
					MylarStatusHandler.fail(e, "Could not open, malformed id: " + repositoryHit.getId(), true);
				}
			}

		}
	}

}
