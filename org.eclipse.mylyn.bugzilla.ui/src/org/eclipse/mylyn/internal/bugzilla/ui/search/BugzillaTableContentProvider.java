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

package org.eclipse.mylar.internal.bugzilla.ui.search;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.internal.ui.SearchPreferencePage;

/**
 * This implementation of <code>BugzillaContentProvider</code> is used for the
 * table view of a Bugzilla search result.
 */
public class BugzillaTableContentProvider extends BugzillaContentProvider implements IStructuredContentProvider {

	/** The page the Bugzilla search results are displayed in */
	private BugzillaSearchResultView bugPage;

	/**
	 * Constructor
	 * 
	 * @param page
	 *            The page the Bugzilla search results are displayed in
	 */
	public BugzillaTableContentProvider(BugzillaSearchResultView page) {
		bugPage = page;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof BugzillaSearchResult) {
			bugResult = (BugzillaSearchResult) newInput;
		}
	}

	@Override
	public void elementsChanged(Object[] updatedElements) {
		TableViewer viewer = getViewer();
		boolean tableLimited = SearchPreferencePage.isTableLimited();
		for (int i = 0; i < updatedElements.length; i++) {
			if (bugResult.getMatchCount(updatedElements[i]) > 0) {
				if (viewer.testFindItem(updatedElements[i]) != null)
					viewer.update(updatedElements[i], null);
				else {
					if (!tableLimited || viewer.getTable().getItemCount() < SearchPreferencePage.getTableLimit())
						viewer.add(updatedElements[i]);
				}
			} else
				viewer.remove(updatedElements[i]);
		}
	}

	/**
	 * Returns the viewer the bug results are displayed in.
	 */
	private TableViewer getViewer() {
		return (TableViewer) bugPage.getViewer();
	}

	@Override
	public void clear() {
		getViewer().refresh();
	}

	/**
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof BugzillaSearchResult) {
			Object[] elements = ((BugzillaSearchResult) inputElement).getElements();
			int tableLimit = SearchPreferencePage.getTableLimit();
			if (SearchPreferencePage.isTableLimited() && elements.length > tableLimit) {
				Object[] shownElements = new Object[tableLimit];
				System.arraycopy(elements, 0, shownElements, 0, tableLimit);
				return shownElements;
			}
			return elements;
		}
		return EMPTY_ARR;
	}

}
