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

import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.internal.tasks.core.data.DefaultTaskSchema;
import org.eclipse.mylyn.internal.tasks.core.data.DefaultTaskSchema.Field;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;

/**
 * @author Steffen Pingel
 * @since 3.0
 */
public class TaskMapper implements ITaskMapping {

	private final boolean createNonExistingAttributes;

	private final TaskData taskData;

	public TaskMapper(TaskData taskData) {
		this(taskData, false);
	}

	public TaskMapper(TaskData taskData, boolean createNonExistingAttributes) {
		this.createNonExistingAttributes = createNonExistingAttributes;
		Assert.isNotNull(taskData);
		this.taskData = taskData;
	}

	public boolean applyTo(ITask task) {
		boolean changed = false;
		if (hasChanges(task.getCompletionDate(), getCompletionDate(), TaskAttribute.DATE_COMPLETION)) {
			task.setCompletionDate(getCompletionDate());
			changed = true;
		}
		if (hasChanges(task.getCreationDate(), getCreationDate(), TaskAttribute.DATE_CREATION)) {
			task.setCreationDate(getCreationDate());
			changed = true;
		}
		if (hasChanges(task.getModificationDate(), getModificationDate(), TaskAttribute.DATE_MODIFICATION)) {
			task.setModificationDate(getModificationDate());
			changed = true;
		}
		if (hasChanges(task.getDueDate(), getDueDate(), TaskAttribute.DATE_DUE)) {
			task.setDueDate(getDueDate());
			changed = true;
		}
		if (hasChanges(task.getOwner(), getOwner(), TaskAttribute.USER_ASSIGNED)) {
			task.setOwner(getOwner());
			changed = true;
		}
		if (hasChanges(task.getPriority(), getPriorityLevelString(), TaskAttribute.PRIORITY)) {
			task.setPriority(getPriorityLevelString());
			changed = true;
		}
		if (hasChanges(task.getSummary(), getSummary(), TaskAttribute.SUMMARY)) {
			task.setSummary(getSummary());
			changed = true;
		}
		if (hasChanges(task.getTaskKey(), getTaskKey(), TaskAttribute.TASK_KEY)) {
			task.setTaskKey(getTaskKey());
			changed = true;
		}
		if (hasChanges(task.getTaskKind(), getTaskKind(), TaskAttribute.TASK_KIND)) {
			task.setTaskKind(getTaskKind());
			changed = true;
		}
		if (hasChanges(task.getUrl(), getTaskUrl(), TaskAttribute.TASK_URL)) {
			task.setUrl(getTaskUrl());
			changed = true;
		}
		return changed;
	}

	private String getPriorityLevelString() {
		return (getPriorityLevel() != null) ? getPriorityLevel().toString() : PriorityLevel.getDefault().toString();
	}

	private boolean hasChanges(Object existingValue, Object newValue, String attributeId) {
		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(attributeId);
		if (attribute != null) {
			return areNotEquals(existingValue, newValue);
		}
		return false;
	}

	private boolean areNotEquals(Object existingProperty, Object newProperty) {
		return (existingProperty != null) ? !existingProperty.equals(newProperty) : newProperty != null;
	}

	private void copyAttributeValue(TaskAttribute sourceAttribute, TaskAttribute targetAttribute) {
		if (targetAttribute == null) {
			return;
		}
		if (!targetAttribute.getMetaData().isReadOnly()) {
			targetAttribute.clearValues();
			if (targetAttribute.getOptions().size() > 0) {
				List<String> values = sourceAttribute.getValues();
				for (String value : values) {
					if (targetAttribute.getOptions().containsKey(value)) {
						targetAttribute.addValue(value);
					}
				}
			} else {
				List<String> values = sourceAttribute.getValues();
				for (String value : values) {
					targetAttribute.addValue(value);
				}
			}
		}
	}

	/**
	 * TODO update comment Sets attribute values from <code>sourceTaskData</code> on <code>targetTaskData</code>. Sets
	 * the following attributes:
	 * <ul>
	 * <li>summary
	 * <li>description
	 * </ul>
	 * Other attribute values are only set if they exist on <code>sourceTaskData</code> and <code>targetTaskData</code>.
	 * 
	 * @param sourceTaskData
	 *            the source task data values are copied from, the connector kind of repository of
	 *            <code>sourceTaskData</code> can be different from <code>targetTaskData</code>
	 * @param targetTaskData
	 *            the target task data values are copied to, the connector kind matches the one of this task data
	 *            handler
	 * @since 2.2
	 */
	public void merge(ITaskMapping source) {
		if (source.getTaskData() != null && this.getTaskData() != null
				&& source.getTaskData().getConnectorKind().equals(this.getTaskData().getConnectorKind())) {
			// task data objects are from the same connector, copy all attributes
			for (TaskAttribute sourceAttribute : source.getTaskData().getRoot().getAttributes().values()) {
				copyAttributeValue(sourceAttribute, this.getTaskData().getRoot().getAttribute(sourceAttribute.getId()));
			}
		} else {
			if (source.getCc() != null) {
				setCc(source.getCc());
			}
			if (source.getDescription() != null) {
				setDescription(source.getDescription());
			}
			if (source.getComponent() != null) {
				setComponent(source.getComponent());
			}
			if (source.getKeywords() != null) {
				setKeywords(source.getKeywords());
			}
			if (source.getOwner() != null) {
				setOwner(source.getOwner());
			}
			if (source.getPriorityLevel() != null) {
				setPriorityLevel(source.getPriorityLevel());
			}
			if (source.getProduct() != null) {
				setProduct(source.getProduct());
			}
			if (source.getSeverity() != null) {
				setSeverity(source.getSeverity());
			}
			if (source.getSummary() != null) {
				setSummary(source.getSummary());
			}
			if (source.getVersion() != null) {
				setVersion(source.getVersion());
			}
		}
	}

	private TaskAttribute createAttribute(String attributeKey, String type) {
		TaskAttribute attribute;
		Field field = DefaultTaskSchema.getField(attributeKey);
		if (field != null) {
			attribute = field.createAttribute(taskData.getRoot());
		} else {
			attribute = taskData.getRoot().createMappedAttribute(attributeKey);
			attribute.getMetaData().defaults().setType(type);
		}
		return attribute;
	}

	public List<String> getCc() {
		return getValues(TaskAttribute.USER_CC);
	}

	public Date getCompletionDate() {
		return getDateValue(TaskAttribute.DATE_COMPLETION);
	}

	public String getComponent() {
		return getValue(TaskAttribute.COMPONENT);
	}

	public Date getCreationDate() {
		return getDateValue(TaskAttribute.DATE_CREATION);
	}

	private Date getDateValue(String attributeKey) {
		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(attributeKey);
		if (attribute != null) {
			return taskData.getAttributeMapper().getDateValue(attribute);
		}
		return null;
	}

	public String getDescription() {
		return getValue(TaskAttribute.DESCRIPTION);
	}

	public Date getDueDate() {
		return getDateValue(TaskAttribute.DATE_DUE);
	}

	public List<String> getKeywords() {
		return getValues(TaskAttribute.KEYWORDS);
	}

	private TaskAttribute getWriteableAttribute(String attributeKey, String type) {
		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(attributeKey);
		if (createNonExistingAttributes) {
			if (attribute == null) {
				attribute = createAttribute(attributeKey, type);
			}
		} else if (attribute != null && attribute.getMetaData().isReadOnly()) {
			return null;
		}
		return attribute;
	}

	public Date getModificationDate() {
		return getDateValue(TaskAttribute.DATE_MODIFICATION);
	}

	public String getOwner() {
		return getValue(TaskAttribute.USER_ASSIGNED);
	}

	public String getPriority() {
		return getValue(TaskAttribute.PRIORITY);
	}

	public PriorityLevel getPriorityLevel() {
		String value = getPriority();
		return (value != null) ? PriorityLevel.fromString(value) : null;
	}

	public String getProduct() {
		return getValue(TaskAttribute.PRODUCT);
	}

	public String getReporter() {
		return getValue(TaskAttribute.USER_REPORTER);
	}

	public String getResolution() {
		return getValue(TaskAttribute.RESOLUTION);
	}

	/**
	 * @since 3.2
	 */
	public String getSeverity() {
		return getValue(TaskAttribute.SEVERITY);
	}

	public String getSummary() {
		return getValue(TaskAttribute.SUMMARY);
	}

	public String getStatus() {
		return getValue(TaskAttribute.STATUS);
	}

	public TaskData getTaskData() {
		return taskData;
	}

	public String getTaskKey() {
		return getValue(TaskAttribute.TASK_KEY);
	}

	public String getTaskKind() {
		return getValue(TaskAttribute.TASK_KIND);
	}

	public String getTaskStatus() {
		return getValue(TaskAttribute.STATUS);
	}

	public String getTaskUrl() {
		return getValue(TaskAttribute.TASK_URL);
	}

	public String getValue(String attributeKey) {
		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(attributeKey);
		if (attribute != null) {
			return taskData.getAttributeMapper().getValueLabel(attribute);
		}
		return null;
	}

	private List<String> getValues(String attributeKey) {
		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(attributeKey);
		if (attribute != null) {
			return taskData.getAttributeMapper().getValueLabels(attribute);
		}
		return null;
	}

	/**
	 * @since 3.2
	 */
	public String getVersion() {
		return getValue(TaskAttribute.VERSION);
	}

	public boolean hasChanges(ITask task) {
		boolean changed = false;
		changed |= hasChanges(task.getCompletionDate(), getCompletionDate(), TaskAttribute.DATE_COMPLETION);
		changed |= hasChanges(task.getCreationDate(), getCreationDate(), TaskAttribute.DATE_CREATION);
		changed |= hasChanges(task.getModificationDate(), getModificationDate(), TaskAttribute.DATE_MODIFICATION);
		changed |= hasChanges(task.getDueDate(), getDueDate(), TaskAttribute.DATE_DUE);
		changed |= hasChanges(task.getOwner(), getOwner(), TaskAttribute.USER_ASSIGNED);
		changed |= hasChanges(task.getPriority(), getPriorityLevelString(), TaskAttribute.PRIORITY);
		changed |= hasChanges(task.getSummary(), getSummary(), TaskAttribute.SUMMARY);
		changed |= hasChanges(task.getTaskKey(), getTaskKey(), TaskAttribute.TASK_KEY);
		changed |= hasChanges(task.getTaskKind(), getTaskKind(), TaskAttribute.TASK_KIND);
		changed |= hasChanges(task.getUrl(), getTaskUrl(), TaskAttribute.TASK_URL);
		return changed;
	}

//	private boolean hasChanges(Object value, String attributeKey) {
//		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(attributeKey);
//		if (attribute != null) {
//			if (TaskAttribute.TYPE_BOOLEAN.equals(attribute.getMetaData().getType())) {
//				return areNotEquals(value, taskData.getAttributeMapper().getBooleanValue(attribute));
//			} else if (TaskAttribute.TYPE_DATE.equals(attribute.getMetaData().getType())) {
//				return areNotEquals(value, taskData.getAttributeMapper().getDateValue(attribute));
//			} else if (TaskAttribute.TYPE_INTEGER.equals(attribute.getMetaData().getType())) {
//				return areNotEquals(value, taskData.getAttributeMapper().getIntegerValue(attribute));
//			} else {
//				return areNotEquals(value, taskData.getAttributeMapper().getValue(attribute));
//			}
//		}
//		return false;
//	}

	public void setCc(List<String> cc) {
		setValues(TaskAttribute.USER_CC, cc);
	}

	public void setCompletionDate(Date dateCompleted) {
		setDateValue(TaskAttribute.DATE_COMPLETION, dateCompleted);
	}

	public void setComponent(String component) {
		setValue(TaskAttribute.COMPONENT, component);
	}

	public void setCreationDate(Date dateCreated) {
		setDateValue(TaskAttribute.DATE_CREATION, dateCreated);
	}

	private TaskAttribute setDateValue(String attributeKey, Date value) {
		TaskAttribute attribute = getWriteableAttribute(attributeKey, TaskAttribute.TYPE_DATE);
		if (attribute != null) {
			taskData.getAttributeMapper().setDateValue(attribute, value);
		}
		return attribute;
	}

	public void setDescription(String description) {
		setValue(TaskAttribute.DESCRIPTION, description);
	}

	public void setDueDate(Date value) {
		setDateValue(TaskAttribute.DATE_DUE, value);
	}

	public void setKeywords(List<String> keywords) {
		setValues(TaskAttribute.KEYWORDS, keywords);
	}

	public void setModificationDate(Date dateModified) {
		setDateValue(TaskAttribute.DATE_MODIFICATION, dateModified);
	}

	// TODO use Person class?
	public void setOwner(String owner) {
		setValue(TaskAttribute.USER_ASSIGNED, owner);
	}

	public void setPriority(String priority) {
		setValue(TaskAttribute.PRIORITY, priority);
	}

	public void setPriorityLevel(PriorityLevel priority) {
		setPriority(priority.toString());
	}

	public void setProduct(String product) {
		setValue(TaskAttribute.PRODUCT, product);
	}

	// TODO use Person class?
	public void setReporter(String reporter) {
		setValue(TaskAttribute.USER_REPORTER, reporter);
	}

	/**
	 * @since 3.2
	 */
	public void setSeverity(String severity) {
		setValue(TaskAttribute.SEVERITY, severity);
	}

	public void setSummary(String summary) {
		setValue(TaskAttribute.SUMMARY, summary);
	}

	public void setStatus(String status) {
		setValue(TaskAttribute.STATUS, status);
	}

	public void setTaskKind(String taskKind) {
		setValue(TaskAttribute.TASK_KIND, taskKind);
	}

	/**
	 * @since 3.3
	 */
	public void setTaskKey(String taskKey) {
		setValue(TaskAttribute.TASK_KEY, taskKey);
	}

	public void setTaskUrl(String taskUrl) {
		setValue(TaskAttribute.TASK_URL, taskUrl);
	}

	/**
	 * @since 3.2
	 */
	public void setVersion(String version) {
		setValue(TaskAttribute.VERSION, version);
	}

	public TaskAttribute setValue(String attributeKey, String value) {
		TaskAttribute attribute = getWriteableAttribute(attributeKey, TaskAttribute.TYPE_SHORT_TEXT);
		if (attribute != null) {
			taskData.getAttributeMapper().setValue(attribute, value);
		}
		return attribute;
	}

	private TaskAttribute setValues(String attributeKey, List<String> values) {
		TaskAttribute attribute = getWriteableAttribute(attributeKey, TaskAttribute.TYPE_SHORT_TEXT);
		if (attribute != null) {
			taskData.getAttributeMapper().setValues(attribute, values);
		}
		return attribute;
	}

}
