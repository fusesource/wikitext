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

package org.eclipse.mylar.internal.tasks.ui.search;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;

/**
 * Sorts search results (AbstractQueryHit) by id.
 */
public class SearchResultSorterId extends ViewerSorter {

	/**
	 * Returns a negative, zero, or positive number depending on whether the
	 * first bug's id is less than, equal to, or greater than the second bug's
	 * id.
	 * <p>
	 * 
	 * @see org.eclipse.jface.viewers.ViewerSorter#compare(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		try {
			// cast the object and get its bug id
			AbstractQueryHit entry1 = (AbstractQueryHit) e1;
			Integer id1 = Integer.parseInt(entry1.getId());

			// cast the other object and get its bug id
			AbstractQueryHit entry2 = (AbstractQueryHit) e2;
			Integer id2 = Integer.parseInt(entry2.getId());

			// if neither is null, compare the bug id's
			if (id1 != null && id2 != null) {
				return id1.compareTo(id2);
			}
		} catch (Exception ignored) {
			// ignore if there is a problem
		}

		// if that didn't work, use the default compare method
		return super.compare(viewer, e1, e2);
	}

	/**
	 * Returns the category of the given element. The category is a number used
	 * to allocate elements to bins; the bins are arranged in ascending numeric
	 * order. The elements within a bin are arranged via a second level sort
	 * criterion.
	 * <p>
	 * 
	 * @see org.eclipse.jface.viewers.ViewerSorter#category(Object)
	 */
	@Override
	public int category(Object element) {
		try {
			AbstractQueryHit hit = (AbstractQueryHit) element;
			return Integer.parseInt(hit.getId());
		} catch (Exception ignored) {
			// ignore
		}
		// if that didn't work, use the default category method
		return super.category(element);
	}
}
