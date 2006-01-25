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
package org.eclipse.mylar.internal.tasklist.ui.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.mylar.core.InteractionEvent;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.internal.core.util.MylarStatusHandler;
import org.eclipse.mylar.internal.tasklist.ITask;
import org.eclipse.mylar.internal.tasklist.MylarTaskListPlugin;

/**
 * @author Ken Sueda (original prototype)
 * @author Wesley Coelho (Added persistent tasks)
 * @author Mik Kersten (hardening)
 */
public class TaskActivationHistory {

	private List<ITask> history = new ArrayList<ITask>();

	private int currentIndex = -1;

	/**
	 * The number of tasks from the previous Eclipse session to load into the
	 * history at startup. (This is not the maximum size of the history, which
	 * is currently unbounded)
	 */
	private static final int NUM_SAVED_HISTORY_ITEMS_TO_LOAD = 10;

	private boolean persistentHistoryLoaded = false;

	/**
	 * Load in a number of saved history tasks from previous session. Should be
	 * called from constructor but ContextManager doesn't seem to be able to
	 * provide activity history at that point
	 * 
	 * @author Wesley Coelho
	 */
	protected void loadPersistentHistory() {
		int tasksAdded = 0;

		for (int i = MylarPlugin.getContextManager().getActivityHistory().getInteractionHistory().size() - 1; i >= 0; i--) {
			ITask prevTask = getHistoryTaskAt(i);

			if (prevTask != null && !isDuplicate(prevTask, i + 1)) {
				history.add(0, prevTask);
				currentIndex++;
				tasksAdded++;
				if (tasksAdded == NUM_SAVED_HISTORY_ITEMS_TO_LOAD) {
					break;
				}
			}
		}
	}

	/**
	 * Returns true if the specified task appears in the activity history
	 * between the starting index and the end of the history list.
	 * 
	 * @author Wesley Coelho
	 */
	protected boolean isDuplicate(ITask task, int startingIndex) {
		for (int i = startingIndex; i < MylarPlugin.getContextManager().getActivityHistory().getInteractionHistory()
				.size(); i++) {
			ITask currTask = getHistoryTaskAt(i);
			if (currTask != null && currTask.getHandleIdentifier().equals(task.getHandleIdentifier())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the task corresponding to the interaction event history item at
	 * the specified position
	 * 
	 * @author Wesley Coelho
	 */
	protected ITask getHistoryTaskAt(int pos) {
		InteractionEvent event = MylarPlugin.getContextManager().getActivityHistory().getInteractionHistory().get(pos);
		return MylarTaskListPlugin.getTaskListManager().getTaskForHandle(event.getStructureHandle(), false);
	}

	public void addTask(ITask task) {
		try {
			if (!persistentHistoryLoaded) {
				loadPersistentHistory();
				persistentHistoryLoaded = true;
			}

			if (hasNext()) {
				for (int i = currentIndex + 1; i < history.size();) {
					history.remove(i);
				}
			}
			if (history.remove(task)) {
				currentIndex--;
			}
			history.add(task);
			currentIndex++;
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "could not add task to history", false);
		}
	}

	public ITask getPreviousTask() {
		try {
			if (hasPrevious()) {
				if ((currentIndex == 0 && !history.get(currentIndex).isActive())) {
					return history.get(currentIndex);
				} else {
					return history.get(--currentIndex);
				}
			} else {
				return null;
			}
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "could not get previous task from history", false);
			return null;
		}
	}

	/**
	 * Get a list of the preceding tasks in the history. navigatedToTask(Task)
	 * should be called to notify the history if the user navigates to an
	 * arbitrary previous task from this list
	 * 
	 * @author Wesley Coelho
	 */
	public List<ITask> getPreviousTasks() {
		try {

			if (!hasPrevious()) {
				return new ArrayList<ITask>();
			}

			if (history.get(currentIndex).isActive()) {
				return history.subList(0, currentIndex);
			} else {
				return history.subList(0, currentIndex + 1);
			}
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "could not get previous tasks from history", false);
			return new ArrayList<ITask>();
		}
	}

	/**
	 * Get a list of the next tasks in the history. navigatedToTask(Task) should
	 * be called to notify the history if the user navigates to an arbitrary
	 * next task from this list
	 * 
	 * @author Wesley Coelho
	 */
	public List<ITask> getNextTasks() {
		try {
			return history.subList(currentIndex + 1, history.size());
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "could not get next tasks from history", false);
			return new ArrayList<ITask>();
		}
	}

	/**
	 * Use this method to notify the task history that the user has navigated to
	 * an arbitrary task in the history without using getNextTask() or
	 * getPreviousTask()
	 * 
	 * @author Wesley Coelho
	 */
	public void navigatedToTask(ITask task) {
		for (int i = 0; i < history.size(); i++) {
			if (history.get(i).getHandleIdentifier() != null
					&& history.get(i).getHandleIdentifier().equals(task.getHandleIdentifier())) {
				currentIndex = i;
				break;
			}
		}
	}

	public boolean hasPrevious() {
		try {
			if (!persistentHistoryLoaded) {
				loadPersistentHistory();
				persistentHistoryLoaded = true;
			}

			return (currentIndex == 0 && !history.get(currentIndex).isActive()) || currentIndex > 0;
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "could determine previous task", false);
			return false;
		}
	}

	public ITask getNextTask() {
		try {
			if (hasNext()) {
				return history.get(++currentIndex);
			} else {
				return null;
			}
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "could not get next task", false);
			return null;
		}
	}

	public boolean hasNext() {
		try {
			return currentIndex < history.size() - 1;
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "could not get next task", false);
			return false;
		}
	}

	public void clear() {
		try {
			history.clear();
			currentIndex = -1;
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "could not clear history", false);
		}
	}
}
