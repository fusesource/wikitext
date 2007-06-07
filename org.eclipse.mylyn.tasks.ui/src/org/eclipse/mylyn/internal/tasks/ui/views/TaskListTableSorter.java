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

package org.eclipse.mylar.internal.tasks.ui.views;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.mylar.internal.tasks.ui.ITasksUiConstants;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.AbstractTaskContainer;
import org.eclipse.mylar.tasks.core.DateRangeContainer;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskListElement;
import org.eclipse.mylar.tasks.core.TaskArchive;
import org.eclipse.mylar.tasks.core.UncategorizedCategory;
import org.eclipse.ui.PlatformUI;

/**
 * @author Mik Kersten
 */
public class TaskListTableSorter extends ViewerSorter {

	public enum SortByIndex {
		PRIORITY, SUMMARY, DATE_CREATED;
	}

	private final TaskListView view;

	private TaskKeyComparator taskKeyComparator = new TaskKeyComparator();

	private SortByIndex sortByIndex;

	public TaskListTableSorter(TaskListView view, SortByIndex sortByIndex) {
		super();
		this.view = view;
		this.sortByIndex = sortByIndex;
	}

	public void setColumn(String column) {
		if (view.isFocusedMode()) {
			MessageDialog
					.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
							ITasksUiConstants.TITLE_DIALOG,
							"Manual sorting is disabled in focused mode, sort order will not take effect until focused mode is disabled.");
		}
	}

	/**
	 * compare - invoked when column is selected calls the actual comparison
	 * method for particular criteria
	 */
	@Override
	public int compare(Viewer compareViewer, Object o1, Object o2) {
		if (o1 instanceof DateRangeContainer) {
			if (o2 instanceof DateRangeContainer) {
				DateRangeContainer dateRangeTaskContainer1 = (DateRangeContainer) o1;
				DateRangeContainer dateRangeTaskContainer2 = (DateRangeContainer) o2;
				return -1 * dateRangeTaskContainer2.getStart().compareTo(dateRangeTaskContainer1.getStart());
			} else if (o2 instanceof ITask) {
				return 1;
			} else {
				return -1;
			}
		}

		if (o1 instanceof UncategorizedCategory && o2 instanceof AbstractTaskContainer) {
			return -1;
		} else if (o1 instanceof AbstractTaskContainer && o2 instanceof UncategorizedCategory) {
			return 1;
		}

		if (o1 instanceof AbstractTaskContainer && o2 instanceof TaskArchive) {
			return -1;
		} else if (o2 instanceof AbstractTaskContainer && o1 instanceof TaskArchive) {
			return 1;
		}

		if (!(o1 instanceof ITask) && o2 instanceof ITask) {
			return 1;
		}

		if (o1 instanceof ITask && !(o2 instanceof AbstractTaskContainer)) {
			return -1;
		}

		// if (o1 instanceof AbstractTaskContainer || o1 instanceof
		// AbstractRepositoryQuery) {
		if (!(o1 instanceof ITask)) {
			if (o2 instanceof AbstractTaskContainer || o2 instanceof AbstractRepositoryQuery) {

				return this.view.sortDirection
						* ((ITaskListElement) o1).getSummary()
								.compareToIgnoreCase(((ITaskListElement) o2).getSummary());
			} else {
				return -1;
			}
		} else if (o1 instanceof ITaskListElement) {
			if (!(o2 instanceof ITask)) {
				return -1;
			} else if (o2 instanceof ITaskListElement) {
				ITaskListElement element1 = (ITaskListElement) o1;
				ITaskListElement element2 = (ITaskListElement) o2;

				return compareElements(element1, element2);
			}
		} else {
			return 0;
		}
		return 0;
	}

	private int compareElements(ITaskListElement element1, ITaskListElement element2) {
		if (SortByIndex.PRIORITY.equals(sortByIndex)) {
			int result = this.view.sortDirection * element1.getPriority().compareTo(element2.getPriority());
			if (result != 0) {
				return result;
			}
		} else if (SortByIndex.DATE_CREATED.equals(sortByIndex)) {
			ITask t1 = null;
			ITask t2 = null;
			if (element1 instanceof ITask) {
				t1 = (ITask) element1;
			}
			if (element2 instanceof ITask) {
				t2 = (ITask) element2;
			}
			if (t1 != null && t2 != null) {
				if (t1.getCreationDate() != null) {
					return t1.getCreationDate().compareTo(t2.getCreationDate());
				}
			}
		} else {
			String summary1 = getSortableSummaryFromElement(element1);
			String summary2 = getSortableSummaryFromElement(element2);
			element2.getSummary();
			return this.view.sortDirection * taskKeyComparator.compare(summary1, summary2);
		}
		return 0;
	}

	public static String getSortableSummaryFromElement(ITaskListElement element) {
		String summary = element.getSummary();

		if (element instanceof AbstractRepositoryTask) {
			AbstractRepositoryTask task1 = (AbstractRepositoryTask) element;
			if (task1.getTaskKey() != null) {
				summary = task1.getTaskKey() + ": " + summary;
			}
		}
		return summary;
	}
}