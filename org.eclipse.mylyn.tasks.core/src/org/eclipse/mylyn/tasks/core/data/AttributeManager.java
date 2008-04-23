/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Steffen Pingel
 */
public class AttributeManager {

	private List<IAttributeManagerListener> listeners;

	private final ITaskDataState taskDataState;

	private final Set<TaskAttribute> editedAttributes;

	public AttributeManager(ITaskDataState taskDataState) {
		this.taskDataState = taskDataState;
		this.editedAttributes = new HashSet<TaskAttribute>();
	}

	public void addAttributeManagerListener(IAttributeManagerListener listener) {
		if (listeners == null) {
			listeners = new ArrayList<IAttributeManagerListener>();
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
		editedAttributes.add(attribute);

		if (listeners != null) {
			for (IAttributeManagerListener listener : listeners.toArray(new IAttributeManagerListener[0])) {
				listener.attributeChanged(attribute);
			}
		}
	}

	public TaskData getTaskData() {
		return taskDataState.getLocalData();
	}

	public boolean hasIncomingChanges(TaskAttribute taskAttribute) {
		TaskData oldTaskData = taskDataState.getLastReadData();
		if (oldTaskData == null) {
			return false;
		}

		if (hasOutgoingChanges(taskAttribute)) {
			return false;
		}

		TaskAttribute oldAttribute = oldTaskData.getMappedAttribute(taskAttribute.getId());
		if (oldAttribute == null) {
			return true;
		}
		if (oldAttribute.getValue() != null && !oldAttribute.getValue().equals(taskAttribute.getValue())) {
			return true;
		} else if (oldAttribute.getValues() != null && !oldAttribute.getValues().equals(taskAttribute.getValues())) {
			return true;
		}
		return false;
	}

	public boolean hasOutgoingChanges(TaskAttribute taskAttribute) {
		return taskDataState.getEditsData().getRoot().getAttribute(taskAttribute.getId()) != null;
	}

	public boolean isDirty() {
		return !editedAttributes.isEmpty();
	}

	public void refresh(IProgressMonitor monitor) throws CoreException {
		taskDataState.refresh(monitor);
	}

	public void removeAttributeManagerListener(IAttributeManagerListener listener) {
		listeners.remove(listener);
	}

	public void save(IProgressMonitor monitor) throws CoreException {
		taskDataState.save(monitor);
	}

}
