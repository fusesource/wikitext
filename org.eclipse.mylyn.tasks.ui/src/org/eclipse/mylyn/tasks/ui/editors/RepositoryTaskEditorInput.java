/*******************************************************************************
 * Copyright (c) 2003 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.tasks.ui.editors;

import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;

/**
 * @author Mik Kersten
 * @author Rob Elves
 */
public class RepositoryTaskEditorInput extends AbstractTaskEditorInput {

	protected String taskId;

	protected String url;

	protected AbstractTask repositoryTask = null;

	public RepositoryTaskEditorInput(TaskRepository repository, String handle, String taskUrl, String taskId) {
		super(repository, handle);
		this.taskId = taskId;
		this.url = taskUrl;
		AbstractTask task = TasksUiPlugin.getTaskListManager().getTaskList().getTask(handle);
		if (task != null && task instanceof AbstractTask) {
			this.repositoryTask = (AbstractTask) task;
		}
	}

	public AbstractTask getRepositoryTask() {
		return repositoryTask;
	}

	public String getName() {
		if (repositoryTask != null) {
			String idLabel = repositoryTask.getTaskKey();

			String label = "";
			if (idLabel != null) {
				label += idLabel + ": ";
			}
			label += repositoryTask.getSummary();
			return label;
		} else if (getTaskData() != null && getTaskData().getLabel() != null) {
			return getTaskData().getTaskKey() + ": " + getTaskData().getLabel();
		} else if (taskId != null) {
			return taskId;
		} else {
			return "<unknown>";
		}
	}

	/**
	 * @return The taskId of the bug for this editor input.
	 */
	public String getId() {
		return taskId;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((repositoryTask == null) ? 0 : repositoryTask.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final RepositoryTaskEditorInput other = (RepositoryTaskEditorInput) obj;
		if (repositoryTask == null) {
			if (other.repositoryTask != null) {
				return false;
			} else if (!other.getId().equals(this.getId())) {
				return false;
			}
		} else if (!repositoryTask.equals(other.repositoryTask))
			return false;
		return true;
	}

	/**
	 * @return url for the repositoryTask/hit. Used by TaskEditor when opening
	 *         browser
	 */
	public String getUrl() {
		return url;
	}

}
