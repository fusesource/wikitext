/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylyn.internal.tasks.ui.actions.ActivateTaskDialogAction;
import org.eclipse.mylyn.internal.tasks.ui.actions.TaskActivateAction;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskActivationHistory;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskElementLabelProvider;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;

/**
 * Copied from ActivateTaskHistoryDropDownAction
 * 
 * TODO: refactor that one into command contribution
 */
public class TaskHistoryDropDown extends CompoundContributionItem {

	private final static int MAX_ITEMS_TO_DISPLAY = 12;

	private boolean scopeToWorkingSet;

	private TaskActivationHistory taskHistory;

	private TaskElementLabelProvider labelProvider = new TaskElementLabelProvider(false);

	public TaskHistoryDropDown() {
		this(null);
	}

	public TaskHistoryDropDown(String id) {
		super(id);
		scopeToWorkingSet = false;
		taskHistory = TasksUiPlugin.getTaskListManager().getTaskActivationHistory();
	}

	protected IContributionItem[] getContributionItems() {
		List<AbstractTask> tasks = new ArrayList<AbstractTask>(taskHistory.getPreviousTasks());
		Set<IWorkingSet> sets = TaskListView.getActiveWorkingSets();
		if (scopeToWorkingSet && !sets.isEmpty()) {
			Set<AbstractTask> allWorkingSetTasks = new HashSet<AbstractTask>();
			for (IWorkingSet workingSet : sets) {
				IAdaptable[] elements = workingSet.getElements();
				for (IAdaptable adaptable : elements) {
					if (adaptable instanceof AbstractTaskContainer) {
						allWorkingSetTasks.addAll(((AbstractTaskContainer) adaptable).getChildren());
					}
				}
			}
			List<AbstractTask> allScopedTasks = new ArrayList<AbstractTask>(tasks);
			for (AbstractTask task : tasks) {
				if (!allWorkingSetTasks.contains(task)) {
					allScopedTasks.remove(task);
				}
			}
			tasks = allScopedTasks;
		}

		if (tasks.size() > MAX_ITEMS_TO_DISPLAY) {
			tasks = tasks.subList(tasks.size() - MAX_ITEMS_TO_DISPLAY, tasks.size());
		}

		List<IContributionItem> items = new ArrayList<IContributionItem>();
		for (int i = tasks.size() - 1; i >= 0; i--) {
			AbstractTask currTask = tasks.get(i);
			Action taskNavAction = new TaskNavigateAction(currTask);
			ActionContributionItem item = new ActionContributionItem(taskNavAction);
			if (currTask.isActive()) {
				taskNavAction.setChecked(true);
			}
			items.add(item);
		}

		Separator separator = new Separator();
		items.add(separator);

		AbstractTask active = TasksUiPlugin.getTaskListManager().getTaskList().getActiveTask();
		if (active != null) {
			Action deactivateAction = new DeactivateTaskAction();
			ActionContributionItem item = new ActionContributionItem(deactivateAction);
			items.add(item);
		} else {
			Action activateDialogAction = new ActivateDialogAction(new ActivateTaskDialogAction());
			ActionContributionItem item = new ActionContributionItem(activateDialogAction);
			items.add(item);
		}

		return items.toArray(new IContributionItem[items.size()]);
	}

	/**
	 * Action for navigating to a specified task. This class should be protected but has been made public for testing
	 * only
	 */
	public class TaskNavigateAction extends Action {

		private static final int MAX_LABEL_LENGTH = 40;

		private AbstractTask targetTask;

		public TaskNavigateAction(AbstractTask task) {
			targetTask = task;
			String taskDescription = task.getSummary();
			if (taskDescription.length() > MAX_LABEL_LENGTH) {
				taskDescription = taskDescription.subSequence(0, MAX_LABEL_LENGTH - 3) + "...";
			}
			setText(taskDescription);
			setEnabled(true);
			setToolTipText(task.getSummary());
			Image image = labelProvider.getImage(task);
			setImageDescriptor(ImageDescriptor.createFromImage(image));
		}

		@Override
		public void run() {
			if (targetTask.isActive()) {
				return;
			}
			new TaskActivateAction().run(targetTask);
			// taskHistory.navigatedToTask(targetTask);
			taskHistory.addTask(targetTask);
//			setButtonStatus();
//			view.refreshAndFocus(false);
//			TasksUiUtil.refreshAndOpenTaskListElement(targetTask);
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

}
