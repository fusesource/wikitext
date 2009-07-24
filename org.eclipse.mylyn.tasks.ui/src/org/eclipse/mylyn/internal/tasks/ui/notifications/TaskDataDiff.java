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

package org.eclipse.mylyn.internal.tasks.ui.notifications;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.internal.tasks.core.RepositoryModel;
import org.eclipse.mylyn.tasks.core.ITaskComment;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;

/**
 * @author Steffen Pingel
 */
public class TaskDataDiff {

	private static final int MAX_CHANGED_ATTRIBUTES = 2;

	private final String[] ATTRIBUTES_IDS = new String[] { TaskAttribute.SUMMARY, TaskAttribute.DESCRIPTION,
			TaskAttribute.PRODUCT, TaskAttribute.PRIORITY, TaskAttribute.USER_ASSIGNED, TaskAttribute.STATUS, };

	private final TaskData newTaskData;

	private final TaskData oldTaskData;

	private final Set<ITaskComment> newComments = new LinkedHashSet<ITaskComment>();

	private final Set<TaskAttributeDiff> changedAttributes = new LinkedHashSet<TaskAttributeDiff>();

	private final RepositoryModel repositoryModel;

	public TaskDataDiff(RepositoryModel repositoryModel, TaskData newTaskData, TaskData oldTaskData) {
		Assert.isNotNull(repositoryModel);
		Assert.isNotNull(newTaskData);
		this.repositoryModel = repositoryModel;
		this.newTaskData = newTaskData;
		this.oldTaskData = oldTaskData;
		parse();
	}

	public Collection<ITaskComment> getNewComments() {
		return newComments;
	}

	public Collection<TaskAttributeDiff> getChangedAttributes() {
		return changedAttributes;
	}

	private void parse() {
		for (String attributeId : ATTRIBUTES_IDS) {
			TaskAttribute newAttribute = newTaskData.getRoot().getMappedAttribute(attributeId);
			TaskAttribute oldAttribute = null;
			if (oldTaskData != null) {
				oldAttribute = oldTaskData.getRoot().getMappedAttribute(attributeId);
			}
			if (oldAttribute == null && newAttribute == null) {
				continue;
			}
			addChangedAttribute(oldAttribute, newAttribute, true);
		}

		// other attributes that have changed on newTaskData
		for (TaskAttribute newAttribute : newTaskData.getRoot().getAttributes().values()) {
			TaskAttribute oldAttribute = null;
			if (oldTaskData != null) {
				oldAttribute = oldTaskData.getRoot().getMappedAttribute(newAttribute.getPath());
			}
			addChangedAttribute(oldAttribute, newAttribute, false);
		}
		// other attributes that have been removed from newTaskData
		if (oldTaskData != null) {
			for (TaskAttribute oldAttribute : oldTaskData.getRoot().getAttributes().values()) {
				TaskAttribute newAttribute = newTaskData.getRoot().getMappedAttribute(oldAttribute.getPath());
				if (newAttribute == null) {
					addChangedAttribute(oldAttribute, newAttribute, false);
				}
			}
		}
	}

	private void addChangedAttribute(TaskAttribute oldAttribute, TaskAttribute newAttribute, boolean ignoreKind) {
		TaskAttribute attribute;
		if (newAttribute != null) {
			attribute = newAttribute;
		} else {
			attribute = oldAttribute;
		}
		String type = attribute.getMetaData().getType();
		if (TaskAttribute.TYPE_COMMENT.equals(type)) {
			addChangedComment(oldAttribute, newAttribute);
		} else if (TaskAttribute.TYPE_OPERATION.equals(type)) {
			// ignore
		} else if (ignoreKind || attribute.getMetaData().getKind() != null) {
			TaskAttributeDiff diff = new TaskAttributeDiff(oldAttribute, newAttribute);
			if (diff.hasChanges()) {
				changedAttributes.add(diff);
			}
		}
	}

	private void addChangedComment(TaskAttribute oldAttribute, TaskAttribute newAttribute) {
		if (oldAttribute == null) {
			ITaskComment comment = repositoryModel.createTaskComment(newAttribute);
			if (comment != null) {
				newComments.add(comment);
			}
		}
	}

	@Override
	public String toString() {
		return toString(60, true);
	}

	// TODO implement trim based on text width
	public String toString(int maxWidth, boolean includeNewest) {
		StringBuilder sb = new StringBuilder();
		String sep = ""; //$NON-NLS-1$
		// append first comment
		int newCommentCount = newComments.size();
		if (newCommentCount > 0) {
			Iterator<ITaskComment> iter = newComments.iterator();
			ITaskComment comment = iter.next();
			if (includeNewest) {
				while (iter.hasNext()) {
					comment = iter.next();
				}
			}

			sb.append(TaskDiffUtil.trim(TaskDiffUtil.commentToString(comment), 60));
			if (newCommentCount > 1) {
				sb.append(" (" + (newCommentCount - 1) + Messages.TaskDataDiff_more_); //$NON-NLS-1$
			}
			sep = "\n"; //$NON-NLS-1$
		}
		// append changed attributes		
		int n = 0;
		for (TaskAttributeDiff attributeDiff : changedAttributes) {
			String label = attributeDiff.getLabel();
			if (label != null) {
				sb.append(sep);
				sb.append(" "); //$NON-NLS-1$
				sb.append(label);
				sb.append(" "); //$NON-NLS-1$
				sb.append(TaskDiffUtil.trim(TaskDiffUtil.listToString(attributeDiff.getRemovedValues()), 28));
				sb.append(" -> "); //$NON-NLS-1$
				sb.append(TaskDiffUtil.trim(TaskDiffUtil.listToString(attributeDiff.getAddedValues()), 28));
				if (++n == MAX_CHANGED_ATTRIBUTES) {
					break;
				}
				sep = "\n"; //$NON-NLS-1$
			}
		}
		return sb.toString();
	}
}
