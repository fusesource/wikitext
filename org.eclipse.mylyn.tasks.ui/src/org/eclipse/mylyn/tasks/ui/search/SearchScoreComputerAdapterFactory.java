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

package org.eclipse.mylar.tasks.ui.search;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.mylar.internal.tasks.ui.TaskSearchPage;
import org.eclipse.mylar.tasks.core.ITaskListElement;
import org.eclipse.search.ui.ISearchPageScoreComputer;

/**
 * Implements a {@link IAdapterFactory} for {@link ISearchPageScoreComputer}s
 * which ranks {@link ITaskListElement}s high for the task search page
 * 
 * @author Willian Mitsuda
 */
public class SearchScoreComputerAdapterFactory implements IAdapterFactory {

	private final ISearchPageScoreComputer computer = new ISearchPageScoreComputer() {

		public int computeScore(String pageId, Object input) {
			if (!TaskSearchPage.ID.equals(pageId)) {
				return ISearchPageScoreComputer.UNKNOWN;
			}
			if (input instanceof ITaskListElement) {
				return 100;
			}
			return ISearchPageScoreComputer.LOWEST;
		}

	};

	@SuppressWarnings("unchecked")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (ISearchPageScoreComputer.class.equals(adapterType)) {
			return computer;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public Class[] getAdapterList() {
		return new Class[] { ISearchPageScoreComputer.class };
	}

}