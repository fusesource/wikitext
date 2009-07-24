/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskDataModelEvent.EventKind;

/**
 * @author Steffen Pingel
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class TaskDataModel {

	private List<TaskDataModelListener> listeners;

	private final ITask task;

	private final TaskRepository taskRepository;

	private final Set<TaskAttribute> unsavedChangedAttributes;

	private final ITaskDataWorkingCopy workingCopy;

	public TaskDataModel(TaskRepository taskRepository, ITask task, ITaskDataWorkingCopy taskDataState) {
		Assert.isNotNull(taskRepository);
		Assert.isNotNull(task);
		Assert.isNotNull(taskDataState);
		this.task = task;
		this.taskRepository = taskRepository;
		this.workingCopy = taskDataState;
		this.unsavedChangedAttributes = new HashSet<TaskAttribute>();
	}

	public void addModelListener(TaskDataModelListener listener) {
		if (listeners == null) {
			listeners = new ArrayList<TaskDataModelListener>();
		}
		listeners.add(listener);
	}

	/**
	 * Invoke upon change to attribute value.
	 * 
	 * @param attribute
	 *            changed attribute
	 */
	public void attributeChanged(TaskAttribute attribute) {
		if (attribute.getParentAttribute() != getTaskData().getRoot()) {
			throw new RuntimeException(
					"Editing is only supported for attributes that are attached to the root of task data"); //$NON-NLS-1$
		}

		unsavedChangedAttributes.add(attribute);

		if (this.listeners != null) {
			final TaskDataModelEvent event = new TaskDataModelEvent(this, EventKind.CHANGED, attribute);
			TaskDataModelListener[] listeners = this.listeners.toArray(new TaskDataModelListener[0]);
			for (final TaskDataModelListener listener : listeners) {
				SafeRunner.run(new ISafeRunnable() {
					public void handleException(Throwable e) {
						StatusHandler.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN, "Listener failed", e)); //$NON-NLS-1$
					}

					public void run() throws Exception {
						listener.attributeChanged(event);
					}
				});
			}
		}
	}

	public Set<TaskAttribute> getChangedAttributes() {
		Set<TaskAttribute> changedAttributes = new LinkedHashSet<TaskAttribute>();
		changedAttributes.addAll(workingCopy.getEditsData().getRoot().getAttributes().values());
		changedAttributes.addAll(unsavedChangedAttributes);
		return changedAttributes;
	}

	public Set<TaskAttribute> getChangedOldAttributes() {
		Set<TaskAttribute> newChangedAttributes = getChangedAttributes();
		Set<TaskAttribute> oldAttributes = new LinkedHashSet<TaskAttribute>();
		TaskData repositoryReadData = workingCopy.getRepositoryData();
		if (repositoryReadData != null) {
			for (TaskAttribute taskAttribute : newChangedAttributes) {
				TaskAttribute attOld = repositoryReadData.getRoot().getAttribute(taskAttribute.getId());
				if (attOld != null) {
					oldAttributes.add(attOld);
				}
			}
		}
		return oldAttributes;
	}

	public ITask getTask() {
		return task;
	}

	public TaskData getTaskData() {
		return workingCopy.getLocalData();
	}

	public TaskRepository getTaskRepository() {
		return taskRepository;
	}

	public boolean hasBeenRead() {
		return workingCopy.getLastReadData() != null;
	}

	public boolean hasIncomingChanges(TaskAttribute taskAttribute) {
		TaskData lastReadData = workingCopy.getLastReadData();
		if (lastReadData == null) {
			return true;
		}

		if (hasOutgoingChanges(taskAttribute)) {
			return false;
		}

		TaskAttribute oldAttribute = lastReadData.getRoot().getMappedAttribute(taskAttribute.getPath());
		if (oldAttribute == null) {
			return true;
		}

		return !getTaskData().getAttributeMapper().equals(taskAttribute, oldAttribute);
	}

	public boolean hasOutgoingChanges(TaskAttribute taskAttribute) {
		return workingCopy.getEditsData().getRoot().getMappedAttribute(taskAttribute.getPath()) != null;
	}

	public boolean isDirty() {
		return unsavedChangedAttributes.size() > 0 || !workingCopy.isSaved();
	}

	public void refresh(IProgressMonitor monitor) throws CoreException {
		workingCopy.refresh(monitor);
	}

	public void removeModelListener(TaskDataModelListener listener) {
		listeners.remove(listener);
	}

	public void revert() {
		workingCopy.revert();
		unsavedChangedAttributes.clear();
	}

	public void save(IProgressMonitor monitor) throws CoreException {
		workingCopy.save(unsavedChangedAttributes, monitor);
		unsavedChangedAttributes.clear();
	}

}
