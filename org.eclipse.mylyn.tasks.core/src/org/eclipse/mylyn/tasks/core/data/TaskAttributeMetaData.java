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

import java.util.Map;

/**
 * @author Steffen Pingel
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class TaskAttributeMetaData {

//	public enum DetailLevel {
//		/** A little bit of detail, e.g. a task showing in the Task List. */
//		LOW,
//		/** More detail, e.g. a task showing in a tool tip. */
//		MEDIUM,
//		/** A lot of detail, e.g. a task showing in an editor. */
//		//HIGH
//	};

	private final TaskAttribute taskAttribute;

	TaskAttributeMetaData(TaskAttribute taskAttribute) {
		this.taskAttribute = taskAttribute;
	}

	public TaskAttributeMetaData defaults() {
		setLabel(null);
		setKind(null);
		setReadOnly(true);
		setType(TaskAttribute.TYPE_SHORT_TEXT);
		return this;
	}

	public TaskAttributeMetaData clear() {
		taskAttribute.clearMetaDataMap();
		return this;
	}

	/**
	 * @deprecated not use, see {@link #setDefaultOption(String)}
	 */
	@Deprecated
	public String getDefaultOption() {
		return taskAttribute.getMetaDatum(TaskAttribute.META_DEFAULT_OPTION);
	}

//	public DetailLevel getDetailLevel() {
//		try {
//			return DetailLevel.valueOf(taskAttribute.getMetaDatum(TaskAttribute.META_DEFAULT_OPTION));
//		} catch (IllegalArgumentException e) {
//			return null;
//		}
//	}

	public String getKind() {
		return taskAttribute.getMetaDatum(TaskAttribute.META_ATTRIBUTE_KIND);
	}

	public String getLabel() {
		return taskAttribute.getMetaDatum(TaskAttribute.META_LABEL);
	}

	public String getType() {
		return taskAttribute.getMetaDatum(TaskAttribute.META_ATTRIBUTE_TYPE);
	}

	public String getValue(String key) {
		return taskAttribute.getMetaDatum(key);
	}

	public Map<String, String> getValues() {
		return taskAttribute.getMetaDataMap();
	}

	public boolean isReadOnly() {
		return Boolean.parseBoolean(taskAttribute.getMetaDatum(TaskAttribute.META_READ_ONLY));
	}

	public TaskAttributeMetaData putValue(String key, String value) {
		taskAttribute.putMetaDatum(key, value);
		return this;
	}

	/**
	 * The default option property is not used. Connectors are expected to set default values in
	 * {@link AbstractTaskDataHandler#initializeTaskData(org.eclipse.mylyn.tasks.core.TaskRepository, TaskData, org.eclipse.mylyn.tasks.core.ITaskMapping, org.eclipse.core.runtime.IProgressMonitor)}
	 * .
	 * 
	 * @deprecated Not used, set default value in
	 *             {@link AbstractTaskDataHandler#initializeTaskData(org.eclipse.mylyn.tasks.core.TaskRepository, TaskData, org.eclipse.mylyn.tasks.core.ITaskMapping, org.eclipse.core.runtime.IProgressMonitor)}
	 *             instead.
	 */
	@Deprecated
	public TaskAttributeMetaData setDefaultOption(String defaultOption) {
		if (defaultOption != null) {
			taskAttribute.putMetaDatum(TaskAttribute.META_DEFAULT_OPTION, defaultOption);
		} else {
			taskAttribute.removeMetaDatum(TaskAttribute.META_DEFAULT_OPTION);
		}
		return this;
	}

//	public TaskAttributeMetaData setDetailLevel(DetailLevel detailLevel) {
//		if (detailLevel != null) {
//			taskAttribute.putMetaDatum(TaskAttribute.META_DETAIL_LEVEL, detailLevel.name());
//		} else {
//			taskAttribute.removeMetaDatum(TaskAttribute.META_DETAIL_LEVEL);
//		}
//		return this;
//	}	

	public TaskAttributeMetaData setKind(String value) {
		if (value != null) {
			taskAttribute.putMetaDatum(TaskAttribute.META_ATTRIBUTE_KIND, value);
		} else {
			taskAttribute.removeMetaDatum(TaskAttribute.META_ATTRIBUTE_KIND);
		}
		return this;
	}

	public TaskAttributeMetaData setLabel(String value) {
		if (value != null) {
			taskAttribute.putMetaDatum(TaskAttribute.META_LABEL, value);
		} else {
			taskAttribute.removeMetaDatum(TaskAttribute.META_LABEL);
		}
		return this;
	}

	public TaskAttributeMetaData setReadOnly(boolean value) {
		taskAttribute.putMetaDatum(TaskAttribute.META_READ_ONLY, Boolean.toString(value));
		return this;
	}

	public TaskAttributeMetaData setType(String value) {
		if (value != null) {
			taskAttribute.putMetaDatum(TaskAttribute.META_ATTRIBUTE_TYPE, value);
		} else {
			taskAttribute.removeMetaDatum(TaskAttribute.META_ATTRIBUTE_TYPE);
		}
		return this;
	}

}
