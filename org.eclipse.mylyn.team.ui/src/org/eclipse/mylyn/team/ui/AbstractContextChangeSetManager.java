/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.team.ui;

import java.util.Set;

import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContextListener;
import org.eclipse.mylyn.context.core.IInteractionElement;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.ITaskListChangeListener;
import org.eclipse.mylyn.tasks.core.TaskContainerDelta;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * Manages changes sets along with task context and activation.
 * 
 * @author Mik Kersten
 * @since 1.0
 */
public abstract class AbstractContextChangeSetManager implements IInteractionContextListener {

	protected boolean isEnabled = false;

	public void enable() {
		if (!isEnabled) {
			isEnabled = true;
			TasksUi.getTaskList().addChangeListener(TASKLIST_CHANGE_LISTENER);
			if (TasksUiPlugin.getTaskListManager().getTaskList().isInitialized()) {
				initContextChangeSets(); // otherwise listener will do it
			}

			if (ContextCore.getContextManager().isContextActive()) {
				contextActivated(ContextCore.getContextManager().getActiveContext());
			}
			ContextCore.getContextManager().addListener(this);
		}
	}

	public void disable() {
		ContextCore.getContextManager().removeListener(this);
		TasksUi.getTaskList().removeChangeListener(TASKLIST_CHANGE_LISTENER);
		isEnabled = false;
	}

	protected abstract void initContextChangeSets();

	protected abstract void updateChangeSetLabel(AbstractTask task);

	private final ITaskListChangeListener TASKLIST_CHANGE_LISTENER = new ITaskListChangeListener() {

		public void taskListRead() {
			initContextChangeSets();
		}

		public void containersChanged(Set<TaskContainerDelta> containers) {
			for (TaskContainerDelta taskContainerDelta : containers) {
				if (taskContainerDelta.getContainer() instanceof AbstractTask) {
					AbstractTask task = (AbstractTask) taskContainerDelta.getContainer();
					switch (taskContainerDelta.getKind()) {
					case CHANGED:
						updateChangeSetLabel(task);
						break;
					}
				}
			}
		}

	};

	public void elementDeleted(IInteractionElement node) {
		// TODO: handle?
	}

	public void landmarkAdded(IInteractionElement node) {
		// ignore
	}

	public void landmarkRemoved(IInteractionElement node) {
		// ignore
	}

	public void relationsChanged(IInteractionElement node) {
		// ignore
	}
}