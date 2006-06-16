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

package org.eclipse.mylar.internal.bugzilla.ui;

import org.eclipse.mylar.internal.bugzilla.ui.search.BugzillaSearchResult;
import org.eclipse.mylar.internal.bugzilla.ui.search.IBugzillaResultEditorMatchAdapter;
import org.eclipse.mylar.internal.tasklist.ui.editors.AbstractBugEditorInput;
import org.eclipse.mylar.internal.tasklist.ui.editors.ExistingBugEditorInput;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

/**
 * @author Mik Kersten (clean-up)
 */
public class BugzillaResultMatchAdapter implements IBugzillaResultEditorMatchAdapter {
 
	/** An empty array of matches */
	private final Match[] EMPTY_ARR = new Match[0];

	private BugzillaSearchResult result;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.text.IEditorMatchAdapter#isShownInEditor(org.eclipse.search.ui.text.Match,
	 *      org.eclipse.ui.IEditorPart)
	 */
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		if (result == null)
			return false;
		IEditorInput ei = editor.getEditorInput();
		if (ei instanceof ExistingBugEditorInput) {
			AbstractBugEditorInput bi = (AbstractBugEditorInput) ei;
			return match.getElement().equals(bi.getRepositoryTaskData());
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.text.IEditorMatchAdapter#computeContainedMatches(org.eclipse.search.ui.text.AbstractTextSearchResult,
	 *      org.eclipse.ui.IEditorPart)
	 */
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		if (result == null)
			return EMPTY_ARR;
		IEditorInput ei = editor.getEditorInput();
		if (ei instanceof ExistingBugEditorInput) {
			AbstractBugEditorInput bi = (AbstractBugEditorInput) ei;
			return result.getMatches(bi.getRepositoryTaskData());
		}
		return EMPTY_ARR;
	}

	public void setResult(BugzillaSearchResult result) {
		this.result = result;
	}

}
