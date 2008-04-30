/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.mylyn.internal.tasks.core.Person;
import org.eclipse.mylyn.internal.tasks.core.TaskGroup;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskContainer;

/**
 * This implementation of <code>SearchResultContentProvider</code> is used for the table view of a Bugzilla search
 * result.
 * 
 * @author Rob Elves (moved into task.ui)
 * @author Mik Kersten
 * @author Frank Becker
 */
public class SearchResultTreeContentProvider extends SearchResultContentProvider {

	private final List<Object> elements = new ArrayList<Object>();

	private final Map<String, Person> owners = new HashMap<String, Person>();

	private final Map<String, TaskGroup> completeState = new HashMap<String, TaskGroup>();

	public enum GroupBy {
		NONE, OWNER, COMPLETION;
	}

	private GroupBy selectedGroup;

	public SearchResultTreeContentProvider() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof RepositorySearchResult) {
			searchResult = (RepositorySearchResult) newInput;
			clear();
			elementsChanged(searchResult.getElements());
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement == searchResult) {
			if (selectedGroup == GroupBy.OWNER) {
				return owners.values().toArray();
			} else if (selectedGroup == GroupBy.COMPLETION) {
				return completeState.values().toArray();
			} else {
				return elements.toArray();
			}
		} else {
			return EMPTY_ARR;
		}
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof AbstractTaskContainer) {
			return ((AbstractTaskContainer) parentElement).getChildren().toArray();
		} else {
			return EMPTY_ARR;
		}
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof AbstractTaskContainer) {
			return !((AbstractTaskContainer) element).getChildren().isEmpty();
		} else {
			return !(element instanceof AbstractTask);
		}
	}

	@Override
	public void elementsChanged(Object[] updatedElements) {
		for (Object object : updatedElements) {
			elements.add(object);

			if (object instanceof AbstractTask) {
				AbstractTask task = ((AbstractTask) object);
				String owner = task.getOwner();
				if (owner == null) {
					owner = "<unknown>";
				}
				Person person = owners.get(owner);
				if (person == null) {
					person = new Person(owner, task.getConnectorKind(), task.getRepositoryUrl());
					owners.put(owner, person);
				}
				person.internalAddChild(task);

				TaskGroup completeIncomplete = null;
				if (task.isCompleted()) {
					completeIncomplete = completeState.get("Complete");
					if (completeIncomplete == null) {
						completeIncomplete = new TaskGroup("group-complete", "Complete", GroupBy.COMPLETION.name());
						completeState.put("Complete", completeIncomplete);
					}
				} else {
					completeIncomplete = completeState.get("Incomplete");
					if (completeIncomplete == null) {
						completeIncomplete = new TaskGroup("group-incomplete", "Incomplete", GroupBy.COMPLETION.name());
						completeState.put("Incomplete", completeIncomplete);
					}
				}
				completeIncomplete.internalAddChild(task);
			}
		}
	}

	@Override
	public void clear() {
		elements.clear();
		owners.clear();
		completeState.clear();
	}

	public GroupBy getSelectedGroup() {
		return selectedGroup;
	}

	public void setSelectedGroup(GroupBy selectedGroup) {
		this.selectedGroup = selectedGroup;
	}

}
