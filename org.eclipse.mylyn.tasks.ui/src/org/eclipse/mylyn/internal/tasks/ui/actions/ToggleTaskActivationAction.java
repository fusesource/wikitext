/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskActivityListener;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * @author Mik Kersten
 */
public class ToggleTaskActivationAction extends Action implements ITaskActivityListener {

	public static final String ID = "org.eclipse.mylyn.tasks.ui.actions.task.activation.toggle";

	private static final String LABEL_ACTIVATE = "Activate Task";

	private static final String LABEL_DEACTIVATE = "Deactivate Task";

	private final ITask task;

	private final IToolBarManager toolBarManager;

	/**
	 * @param task
	 * 		cannot be null
	 * @param toolBarManager
	 * 		cannot be null
	 */
	public ToggleTaskActivationAction(ITask task, IToolBarManager toolBarManager) {
		this.task = task;
		this.toolBarManager = toolBarManager;
		setId(ID);
		setImageDescriptor(TasksUiImages.CONTEXT_ACTIVE_CENTERED);
		update();
		TasksUi.getTaskActivityManager().addActivityListener(this);
	}

	public void dispose() {
		TasksUi.getTaskActivityManager().removeActivityListener(this);
	}

	private void update() {
		setChecked(task.isActive());
		if (task.isActive()) {
			setText(LABEL_DEACTIVATE);
			setToolTipText(LABEL_DEACTIVATE);
		} else {
			setText(LABEL_ACTIVATE);
			setToolTipText(LABEL_ACTIVATE);
		}
	}

	@Override
	public void run() {
		if (!task.isActive()) {
			TasksUi.getTaskActivityManager().activateTask(task);
		} else {
			TasksUi.getTaskActivityManager().deactivateTask(task);
		}
		update();
//		toolBarManager.add(this);
	}

	public void taskActivated(ITask task) {
		update();
		toolBarManager.update(true);
	}

	public void taskDeactivated(ITask task) {
		update();
		toolBarManager.update(true);
	}

	public void preTaskActivated(ITask task) {
		// ignore
	}

	public void preTaskDeactivated(ITask task) {
		// ignore		
	}

	public void activityReset() {
		// ignore
	}

	public void elapsedTimeUpdated(ITask task, long newElapsedTime) {
		// ignore
	}

}
