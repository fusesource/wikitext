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
package org.eclipse.mylar.internal.tasklist.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylar.internal.tasklist.ui.views.TaskActivationHistory;
import org.eclipse.mylar.internal.tasklist.ui.views.TaskElementLabelProvider;
import org.eclipse.mylar.internal.tasklist.ui.views.TaskListView;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

/**
 * This abstract class contains some common code used by NextTaskDropDownAction
 * and PreviousTaskDropDownAction
 * 
 * @author Wesley Coelho
 * @author Mik Kersten
 */
public abstract class TaskNavigateDropDownAction extends Action implements IMenuCreator {
	protected final TaskListView view;

	protected TaskActivationHistory taskHistory;

	protected Menu dropDownMenu = null;

	protected TaskElementLabelProvider labelProvider = new TaskElementLabelProvider();

	/** Maximum number of items to appear in the drop-down menu */
	protected final static int MAX_ITEMS_TO_DISPLAY = 12;

	public TaskNavigateDropDownAction(TaskListView view, TaskActivationHistory history) {
		super();
		this.view = view;
		taskHistory = history;
		setMenuCreator(this);
	}

	/**
	 * Action for navigating to a specified task. This class should be protected
	 * but has been made public for testing only
	 */
	public class TaskNavigateAction extends Action {

		private ITask targetTask;

		private static final int MAX_LABEL_LENGTH = 40;

		public TaskNavigateAction(ITask task) {
			targetTask = task;
			String taskDescription = task.getDescription();
			if (taskDescription.length() > MAX_LABEL_LENGTH) {
				taskDescription = taskDescription.subSequence(0, MAX_LABEL_LENGTH - 3) + "...";
			}
			setText(taskDescription);
			setEnabled(true);
			setToolTipText(task.getDescription());
			Image image = labelProvider.getImage(task);
			setImageDescriptor(ImageDescriptor.createFromImage(image));
		}

		public void run() {
			new TaskActivateAction().run(targetTask);
			taskHistory.navigatedToTask(targetTask);
			setButtonStatus();
			view.refreshAndFocus(false);
		}
	}

	public void dispose() {
		if (dropDownMenu != null) {
			dropDownMenu.dispose();
			dropDownMenu = null;
		}
	}

	public Menu getMenu(Control parent) {
		if (dropDownMenu != null) {
			dropDownMenu.dispose();
		}
		dropDownMenu = new Menu(parent);
		addActionsToMenu();
		return dropDownMenu;
	}

	public Menu getMenu(Menu parent) {
		if (dropDownMenu != null) {
			dropDownMenu.dispose();
		}
		dropDownMenu = new Menu(parent);
		addActionsToMenu();
		return dropDownMenu;
	}

	protected void setButtonStatus() {
		view.getPreviousTaskAction().setEnabled(taskHistory.hasPrevious());
		view.getNextTaskAction().setEnabled(taskHistory.hasNext());
	}

	protected abstract void addActionsToMenu();

}
