/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import java.util.List;

import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskElement;

/**
 * @author Rob Elves
 */
public class MarkTaskUnreadAction extends AbstractTaskAction {

	private static final String ACTION_NAME = "Unread";

	public static final String ID = "org.eclipse.mylyn.tasklist.actions.mark.unread";

	public static final String DEFINITION_ID = "org.eclipse.mylyn.tasks.ui.command.markTaskUnread";

	public MarkTaskUnreadAction(List<ITaskElement> selectedElements) {
		this.selectedElements = selectedElements;
		setText(ACTION_NAME);
		setToolTipText(ACTION_NAME);
		setToolTipText("Mark " + ACTION_NAME);
		setId(ID);
		setActionDefinitionId(DEFINITION_ID);
		setImageDescriptor(CommonImages.OVERLAY_SYNC_INCOMMING);
//		if (containsArchiveContainer(selectedElements)) {
//			setEnabled(false);
//		} else {
		if (selectedElements.size() == 1 && (selectedElements.get(0) instanceof ITask)) {
			ITask task = (ITask) selectedElements.get(0);
			setEnabled(!task.isLocal());
		} else {
			setEnabled(true);
		}
//		}
	}

	@Override
	protected void performActionOnTask(ITask repositoryTask) {
		TasksUiPlugin.getTaskDataManager().setTaskRead(repositoryTask, false);
	}

}
