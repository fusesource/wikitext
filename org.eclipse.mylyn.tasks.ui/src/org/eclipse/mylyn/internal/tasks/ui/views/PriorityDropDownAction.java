/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.mylar.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.tasks.core.Task;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

/**
 * @author Mik Kersten
 */
class PriorityDropDownAction extends Action implements IMenuCreator {

	/**
	 * 
	 */
	private final TaskListView taskListView;

	private static final String FILTER_PRIORITY_LABEL = "Filter Priority Lower Than";

	private Action priority1;
	
	private Action priority2;
	
	private Action priority3;
	
	private Action priority4;
	
	private Action priority5;
	
	private Menu dropDownMenu = null;

	public PriorityDropDownAction(TaskListView taskListView) {
		super();
		this.taskListView = taskListView;
		setText(FILTER_PRIORITY_LABEL);
		setToolTipText(FILTER_PRIORITY_LABEL);
		setImageDescriptor(TasksUiImages.FILTER_PRIORITY);
		setMenuCreator(this);
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
	
	public void addActionsToMenu() {
		priority1 = new Action("", AS_CHECK_BOX) {
			@Override
			public void run() {
				TasksUiPlugin.getDefault().getPreferenceStore().setValue(
						TaskListPreferenceConstants.SELECTED_PRIORITY, Task.PriorityLevel.P1.toString());
				// MylarTaskListPlugin.setCurrentPriorityLevel(Task.PriorityLevel.P1);
				TaskListView.FILTER_PRIORITY.displayPrioritiesAbove(TaskListView.PRIORITY_LEVELS[0]);
				PriorityDropDownAction.this.taskListView.getViewer().refresh();
			}
		};
		priority1.setEnabled(true);
		priority1.setText(Task.PriorityLevel.P1.getDescription());
		priority1.setImageDescriptor(TasksUiImages.PRIORITY_1);
		ActionContributionItem item = new ActionContributionItem(priority1);
		item.fill(dropDownMenu, -1);

		priority2 = new Action("", AS_CHECK_BOX) {
			@Override
			public void run() {
				TasksUiPlugin.getDefault().getPreferenceStore().setValue(
						TaskListPreferenceConstants.SELECTED_PRIORITY, Task.PriorityLevel.P2.toString());
				// MylarTaskListPlugin.setCurrentPriorityLevel(Task.PriorityLevel.P2);
				TaskListView.FILTER_PRIORITY.displayPrioritiesAbove(TaskListView.PRIORITY_LEVELS[1]);
//				updateCheckedState(priority1, priority2, priority3, priority4, priority5);
				PriorityDropDownAction.this.taskListView.getViewer().refresh();
			}
		};
		priority2.setEnabled(true);
		priority2.setText(Task.PriorityLevel.P2.getDescription());
		priority2.setImageDescriptor(TasksUiImages.PRIORITY_2);
		item = new ActionContributionItem(priority2);
		item.fill(dropDownMenu, -1);

		priority3 = new Action("", AS_CHECK_BOX) {
			@Override
			public void run() {
				TasksUiPlugin.getDefault().getPreferenceStore().setValue(
						TaskListPreferenceConstants.SELECTED_PRIORITY, Task.PriorityLevel.P3.toString());
				// MylarTaskListPlugin.setCurrentPriorityLevel(Task.PriorityLevel.P3);
				TaskListView.FILTER_PRIORITY.displayPrioritiesAbove(TaskListView.PRIORITY_LEVELS[2]);
//				updateCheckedState(priority1, priority2, priority3, priority4, priority5);
				PriorityDropDownAction.this.taskListView.getViewer().refresh();
			}
		};
		priority3.setEnabled(true);
		priority3.setText(Task.PriorityLevel.P3.getDescription());
		priority3.setImageDescriptor(TasksUiImages.PRIORITY_3);
		item = new ActionContributionItem(priority3);
		item.fill(dropDownMenu, -1);

		priority4 = new Action("", AS_CHECK_BOX) {
			@Override
			public void run() {
				TasksUiPlugin.getDefault().getPreferenceStore().setValue(
						TaskListPreferenceConstants.SELECTED_PRIORITY, Task.PriorityLevel.P4.toString());
				// MylarTaskListPlugin.setCurrentPriorityLevel(Task.PriorityLevel.P4);
				TaskListView.FILTER_PRIORITY.displayPrioritiesAbove(TaskListView.PRIORITY_LEVELS[3]);
//				updateCheckedState(priority1, priority2, priority3, priority4, priority5);
				PriorityDropDownAction.this.taskListView.getViewer().refresh();
			}
		};
		priority4.setEnabled(true);
		priority4.setText(Task.PriorityLevel.P4.getDescription());
		priority4.setImageDescriptor(TasksUiImages.PRIORITY_4);
		item = new ActionContributionItem(priority4);
		item.fill(dropDownMenu, -1);

		priority5 = new Action("", AS_CHECK_BOX) {
			@Override
			public void run() {
				TasksUiPlugin.getDefault().getPreferenceStore().setValue(
						TaskListPreferenceConstants.SELECTED_PRIORITY, Task.PriorityLevel.P5.toString());
				// MylarTaskListPlugin.setCurrentPriorityLevel(Task.PriorityLevel.P5);
				TaskListView.FILTER_PRIORITY.displayPrioritiesAbove(TaskListView.PRIORITY_LEVELS[4]);
//				updateCheckedState(priority1, priority2, priority3, priority4, priority5);
				PriorityDropDownAction.this.taskListView.getViewer().refresh();
			}
		};
		priority5.setEnabled(true);
		priority5.setImageDescriptor(TasksUiImages.PRIORITY_5);
		priority5.setText(Task.PriorityLevel.P5.getDescription());
		item = new ActionContributionItem(priority5);
		item.fill(dropDownMenu, -1);

		updateCheckedState();
//		updateCheckedState(priority1, priority2, priority3, priority4, priority5);
	}

	void updateCheckedState() {
		if (priority1 == null) {
			return;
		}
		String priority = TaskListView.getCurrentPriorityLevel();

		priority1.setChecked(false);
		priority2.setChecked(false);
		priority3.setChecked(false);
		priority4.setChecked(false);
		priority5.setChecked(false);
		
		if (priority.equals(TaskListView.PRIORITY_LEVELS[0])) {
			priority1.setChecked(true);
		} else if (priority.equals(TaskListView.PRIORITY_LEVELS[1])) {
			priority1.setChecked(true);
			priority2.setChecked(true);
		} else if (priority.equals(TaskListView.PRIORITY_LEVELS[2])) {
			priority1.setChecked(true);
			priority2.setChecked(true);
			priority3.setChecked(true);
		} else if (priority.equals(TaskListView.PRIORITY_LEVELS[3])) {
			priority1.setChecked(true);
			priority2.setChecked(true);
			priority3.setChecked(true);
			priority4.setChecked(true);
		} else if (priority.equals(TaskListView.PRIORITY_LEVELS[4])) {
			priority1.setChecked(true);
			priority2.setChecked(true);
			priority3.setChecked(true);
			priority4.setChecked(true);
			priority5.setChecked(true);
		}
	}

	@Override
	public void run() {
		this.setChecked(isChecked());
	}
}