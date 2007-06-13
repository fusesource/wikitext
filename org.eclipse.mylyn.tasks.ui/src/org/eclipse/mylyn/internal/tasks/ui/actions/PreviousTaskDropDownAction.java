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
package org.eclipse.mylyn.internal.tasks.ui.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskActivationHistory;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.PlatformUI;

/**
 * @author Wesley Coelho
 * @author Mik Kersten
 * @author Leo Dos Santos
 */
public class PreviousTaskDropDownAction extends TaskNavigateDropDownAction {

	public static final String ID = "org.eclipse.mylyn.tasklist.actions.navigate.previous";

	public PreviousTaskDropDownAction(TaskActivationHistory history) {
		super(history);
		setText("Previous Task");
		setToolTipText("Previous Task");
		setId(ID);
		setEnabled(true);
		setImageDescriptor(TasksUiImages.NAVIGATE_PREVIOUS);
	}

	@Override
	protected void addActionsToMenu() {
		List<AbstractTask> tasks = taskHistory.getPreviousTasks();

		if (tasks.size() > MAX_ITEMS_TO_DISPLAY) {
			tasks = tasks.subList(tasks.size() - MAX_ITEMS_TO_DISPLAY, tasks.size());
		}

		for (int i = tasks.size() - 1; i >= 0; i--) {
			AbstractTask currTask = tasks.get(i);
			Action taskNavAction = new TaskNavigateAction(currTask);
			ActionContributionItem item = new ActionContributionItem(taskNavAction);
			if (currTask.isActive()) {
				taskNavAction.setChecked(true);
			}
			item.fill(dropDownMenu, -1);
		}

		Separator separator = new Separator();
		separator.fill(dropDownMenu, -1);

		AbstractTask active = TasksUiPlugin.getTaskListManager().getTaskList().getActiveTask();
		if (active != null) {
			Action deactivateAction = new DeactivateTaskAction();
			ActionContributionItem item = new ActionContributionItem(deactivateAction);
			item.fill(dropDownMenu, -1);
		} else {
			Action activateDialogAction = new ActivateDialogAction(new ActivateTaskDialogAction());
			ActionContributionItem item = new ActionContributionItem(activateDialogAction);
			item.fill(dropDownMenu, -1);
		}
	}

	@Override
	public void run() {
		if (taskHistory.hasPrevious()) {
			AbstractTask previousTask = taskHistory.getPreviousTask();
			new TaskActivateAction().run(previousTask);
			setButtonStatus();
			// view.refreshAndFocus(false);
			// TasksUiUtil.refreshAndOpenTaskListElement(previousTask);
		}
	}

	public class DeactivateTaskAction extends Action {

		public DeactivateTaskAction() {
			setText("Deactivate Task");
			setToolTipText("Deactivate Task");
			setEnabled(true);
			setChecked(false);
			setImageDescriptor(null);
					//TasksUiImages.TASK_INACTIVE);
		}

		@Override
		public void run() {
			AbstractTask active = TasksUiPlugin.getTaskListManager().getTaskList().getActiveTask();
			if (active != null) {
				TasksUiPlugin.getTaskListManager().deactivateTask(active);
			}
		}

	}

	public class ActivateDialogAction extends Action {

		private ActivateTaskDialogAction dialogAction;

		public ActivateDialogAction(ActivateTaskDialogAction action) {
			dialogAction = action;
			dialogAction.init(PlatformUI.getWorkbench().getActiveWorkbenchWindow());

			setText("Activate Task...");
			setToolTipText("Activate Task...");
			setEnabled(true);
			setChecked(false);
			setImageDescriptor(null);
					//TasksUiImages.TASK_ACTIVE);
		}

		@Override
		public void run() {
			dialogAction.run(null);
		}

	}

}
