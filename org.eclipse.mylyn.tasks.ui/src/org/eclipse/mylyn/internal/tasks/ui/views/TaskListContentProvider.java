/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.mylyn.internal.tasks.core.ScheduledTaskContainer;
import org.eclipse.mylyn.internal.tasks.core.TaskArchive;
import org.eclipse.mylyn.internal.tasks.ui.AbstractTaskListFilter;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPreferenceConstants;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskCategory;
import org.eclipse.mylyn.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.IWorkingSet;

/**
 * Provides custom content for the task list, e.g. guaranteed visibility of some elements, ability to suppress
 * containers showing if nothing should show under them.
 * 
 * TODO: move to viewer filter architecture?
 * 
 * @author Mik Kersten
 */
public class TaskListContentProvider extends AbstractTaskListContentProvider {

	public TaskListContentProvider(TaskListView taskListView) {
		super(taskListView);
	}

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		this.taskListView.expandToActiveTasks();
	}

	public void dispose() {
	}

	public Object[] getElements(Object parent) {
		if (parent.equals(this.taskListView.getViewSite())) {
			return applyFilter(TasksUiPlugin.getTaskListManager().getTaskList().getRootElements()).toArray();
		}
		return getChildren(parent);
	}

	/**
	 * returns first parent found
	 */
	public Object getParent(Object child) {
		// Return first parent found, first search within queries then categories.
		if (child instanceof AbstractTask) {
			Set<AbstractRepositoryQuery> queries = TasksUiPlugin.getTaskListManager()
					.getTaskList()
					.getQueriesForHandle(((AbstractTask) child).getHandleIdentifier());
			if (queries.size() > 0) {
				return queries.toArray()[0];
			}

			AbstractTaskContainer container = TasksUiPlugin.getTaskListManager().getTaskList().getContainerForHandle(
					((AbstractTask) child).getHandleIdentifier());

			if (container != null) {
				return container;
			}

		}
		// no parent found
		return null;
	}

	public Object[] getChildren(Object parent) {
		return getFilteredChildrenFor(parent).toArray();
	}

	/**
	 * NOTE: If parent is an ITask, this method checks if parent has unfiltered children (see bug 145194).
	 */
	public boolean hasChildren(Object parent) {
		if (parent instanceof AbstractRepositoryQuery) {
			AbstractRepositoryQuery t = (AbstractRepositoryQuery) parent;
			return !t.isEmpty();
		} else if (parent instanceof AbstractTask) {
			return taskHasUnfilteredChildren((AbstractTask) parent);
		} else if (parent instanceof AbstractTaskContainer) {
			AbstractTaskContainer container = (AbstractTaskContainer) parent;
			return !container.getChildren().isEmpty();
		}
		return false;
	}

	private boolean taskHasUnfilteredChildren(AbstractTask parent) {
		boolean filterSubtasks = TasksUiPlugin.getDefault().getPreferenceStore().getBoolean(
				TasksUiPreferenceConstants.FILTER_SUBTASKS);
		if (filterSubtasks)
			return false;
		Set<AbstractTask> children = parent.getChildren();
		if (children != null) {
			for (AbstractTask task : children) {
				if (!filter(parent, task)) {
					return true;
				}
			}
		}
		return false;
	}

	protected List<AbstractTaskContainer> applyFilter(Set<AbstractTaskContainer> roots) {
		String filterText = (taskListView.getFilteredTree().getFilterControl()).getText();
		if (containsNoFilterText(filterText)) {
			List<AbstractTaskContainer> filteredRoots = new ArrayList<AbstractTaskContainer>();
			for (AbstractTaskContainer element : roots) {
				// NOTE: tasks can no longer appear as root elements
				if (element instanceof AbstractRepositoryQuery) {
					if (selectQuery((AbstractRepositoryQuery) element)) {
						filteredRoots.add(element);
					}
				} else if (element instanceof AbstractTaskCategory) {
					if (selectContainer(element)) {
						filteredRoots.add(element);
					}
				}
			}
			return filteredRoots;
		} else {
			// only match working sets when filter is on
			Set<IWorkingSet> workingSets = TaskListView.getActiveWorkingSets();
			Set<AbstractTaskContainer> workingSetContainers = new HashSet<AbstractTaskContainer>();
			if (workingSets.isEmpty()) {
				return new ArrayList<AbstractTaskContainer>(roots);
			} else {
				for (IWorkingSet workingSet : workingSets) {
					IAdaptable[] elements = workingSet.getElements();
					for (IAdaptable adaptable : elements) {
						if (adaptable instanceof AbstractTaskContainer && roots.contains(adaptable)) {
							workingSetContainers.add((AbstractTaskContainer) adaptable);
						}
					}
				}
				return new ArrayList<AbstractTaskContainer>(workingSetContainers);
			}
		}
	}

	/**
	 * See bug 109693
	 */
	private boolean containsNoFilterText(String filterText) {
		return filterText == null || filterText.length() == 0;
	}

	// TODO: should only know about containers, not queries
	private boolean selectQuery(AbstractRepositoryQuery query) {
		Set<AbstractTask> hits = query.getChildren();
		if (hits.size() == 0) {
			return true;
		}
		for (AbstractTask element : hits) {
			if (!filter(query, element)) {
				return true;
			}
		}
		return false;
	}

	private boolean selectContainer(AbstractTaskContainer container) {
		// TODO: move to interest filter
		if (/*taskListView.isFocusedMode() && */container instanceof ScheduledTaskContainer) {
			ScheduledTaskContainer scheduleContainer = (ScheduledTaskContainer) container;
			if (TasksUiPlugin.getTaskActivityManager().isWeekDay(scheduleContainer)
					&& (scheduleContainer.isPresent() || scheduleContainer.isFuture())) {
				return true;
			} else if (taskListView.isFocusedMode()) {
				return false;
			}
		}

		if (filter(null, container) && !shouldAlwaysShow(container)) {
			return false;
		}

		Set<AbstractTask> children = container.getChildren();
		if (children.size() == 0) {
			return true;
		}
		for (AbstractTaskContainer child : children) {
			if (!filter(container, child)) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldAlwaysShow(AbstractTaskContainer container) {
		for (AbstractTask task : container.getChildren()) {
			if (shouldAlwaysShow(container, task)) {
				if (container instanceof TaskArchive) {
					if (TasksUiPlugin.getTaskListManager().getTaskList().getContainerForHandle(
							task.getHandleIdentifier()) == null
							&& TasksUiPlugin.getTaskListManager().getTaskList().getQueriesForHandle(
									task.getHandleIdentifier()).isEmpty()) {
						return true;
					}
				} else {
					return true;
				}
			}
		}
		return false;
	}

	private boolean shouldAlwaysShow(Object parent, AbstractTask task) {
		for (AbstractTaskListFilter filter : this.taskListView.getFilters()) {
			if (filter.shouldAlwaysShow(parent, task, !TasksUiPlugin.getDefault().getPreferenceStore().getBoolean(
					TasksUiPreferenceConstants.FILTER_SUBTASKS))) {
				return true;
			}
		}
		return false;
	}

	// TODO: This can be simplified post bug#124321
	private List<AbstractTaskContainer> getFilteredChildrenFor(Object parent) {
		if (containsNoFilterText((this.taskListView.getFilteredTree().getFilterControl()).getText())) {
			List<AbstractTaskContainer> children = new ArrayList<AbstractTaskContainer>();
			if (parent instanceof AbstractTaskCategory) {
				if (filter(null, parent)) {
					if (((AbstractTaskContainer) parent) instanceof TaskArchive) {
						for (AbstractTask task : ((AbstractTaskContainer) parent).getChildren()) {
							if (shouldAlwaysShow(parent, task)) {
								// TODO: archive logic?
								if (TasksUiPlugin.getTaskListManager().getTaskList().getQueriesForHandle(
										task.getHandleIdentifier()).size() == 0) {
									children.add(task);
								}
							}
						}
						return children;
					}
				}
				children = getFilteredRootChildren((AbstractTaskContainer) parent);
//				Set<AbstractTask> parentsTasks = ((AbstractTaskContainer) parent).getChildren();
//				for (AbstractTaskContainer element : parentsTasks) {
//					if (!filter(parent, element)) {
//						children.add(element);
//					}
//				}
				return children;
			} else if (parent instanceof AbstractRepositoryQuery) {
				return getFilteredRootChildren((AbstractTaskContainer) parent);
//				for (AbstractTaskContainer element : ((AbstractRepositoryQuery) parent).getChildren()) {
//					if (!filter(parent, element)) {
//						children.add(element);
//					}
//				}
//				return children;
			} else if (parent instanceof AbstractTask) {
				Set<AbstractTask> subTasks = ((AbstractTask) parent).getChildren();
				for (AbstractTask t : subTasks) {
					if (!filter(parent, t)) {
						children.add(t);
					}
				}
				return children;
			}
		} else {
			List<AbstractTaskContainer> children = new ArrayList<AbstractTaskContainer>();
			if (parent instanceof AbstractTaskContainer) {
				children.addAll(((AbstractTaskContainer) parent).getChildren());
				return children;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * @return all children who aren't already revealed as a sub task
	 */
	private List<AbstractTaskContainer> getFilteredRootChildren(AbstractTaskContainer parent) {
		Set<AbstractTask> parentTasks = parent.getChildren();
		Set<AbstractTaskContainer> parents = new HashSet<AbstractTaskContainer>();
		Set<AbstractTask> children = new HashSet<AbstractTask>();
		// get all children
		for (AbstractTask element : parentTasks) {
			for (AbstractTask abstractTask : element.getChildren()) {
				if (!filter(element, abstractTask)) {
					children.add(abstractTask);
				}
			}
		}
		for (AbstractTask task : parentTasks) {
			if (!filter(parent, task) && !children.contains(task)) {
				parents.add(task);
			}
		}
		List<AbstractTaskContainer> result = new ArrayList<AbstractTaskContainer>();
		result.addAll(parents);
		return result;
	}

	protected boolean filter(Object parent, Object object) {
		for (AbstractTaskListFilter filter : this.taskListView.getFilters()) {
			if (!filter.select(parent, object)) {
				return true;
			}
		}
		return false;
	}

}
