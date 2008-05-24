/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core.data;

import java.util.Date;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.ITaskComment;

/**
 * A comment posted by a user on a task.
 * 
 * @author Rob Elves
 * @author Steffen Pingel
 * @since 3.0
 */
public class TaskCommentMapper {

	private static final int DEFAULT_NUMBER = -1;

	private IRepositoryPerson author;

	private String commentId;

	private Date creationDate;

	private int number;

	private String text;

	private String url;

	public TaskCommentMapper() {
	}

	public IRepositoryPerson getAuthor() {
		return author;
	}

	public String getCommentId() {
		return commentId;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public int getNumber() {
		return number;
	}

	public String getText() {
		return text;
	}

	public String getUrl() {
		return url;
	}

	public void setAuthor(IRepositoryPerson author) {
		this.author = author;
	}

	public void setCommentId(String commentId) {
		this.commentId = commentId;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public static TaskCommentMapper createFrom(TaskAttribute taskAttribute) {
		Assert.isNotNull(taskAttribute);
		TaskData taskData = taskAttribute.getTaskData();
		TaskAttributeMapper mapper = taskData.getAttributeMapper();
		TaskCommentMapper comment = new TaskCommentMapper();
		comment.setCommentId(mapper.getValue(taskAttribute));
		TaskAttribute child = taskAttribute.getMappedAttribute(TaskAttribute.COMMENT_AUTHOR);
		if (child != null) {
			IRepositoryPerson person = mapper.getRepositoryPerson(child);
			if (person.getName() == null) {
				child = taskAttribute.getMappedAttribute(TaskAttribute.COMMENT_AUTHOR_NAME);
				if (child != null) {
					person.setName(child.getValue());
				}
			}
			comment.setAuthor(person);
		}
		child = taskAttribute.getMappedAttribute(TaskAttribute.COMMENT_DATE);
		if (child != null) {
			comment.setCreationDate(mapper.getDateValue(child));
		}
		child = taskAttribute.getMappedAttribute(TaskAttribute.COMMENT_NUMBER);
		if (child != null) {
			Integer value = mapper.getIntegerValue(child);
			comment.setNumber((value != null) ? value : DEFAULT_NUMBER);
		}
		child = taskAttribute.getMappedAttribute(TaskAttribute.COMMENT_URL);
		if (child != null) {
			comment.setUrl(mapper.getValue(child));
		}
		child = taskAttribute.getMappedAttribute(TaskAttribute.COMMENT_TEXT);
		if (child != null) {
			comment.setText(mapper.getValue(child));
		}
		return comment;
	}

	public void applyTo(TaskAttribute taskAttribute) {
		Assert.isNotNull(taskAttribute);
		TaskData taskData = taskAttribute.getTaskData();
		TaskAttributeMapper mapper = taskData.getAttributeMapper();
		TaskAttributeProperties.defaults().setType(TaskAttribute.TYPE_COMMENT).applyTo(taskAttribute);
		if (getCommentId() != null) {
			mapper.setValue(taskAttribute, getCommentId());
		}
		if (getAuthor() != null) {
			TaskAttribute child = taskAttribute.createAttribute(TaskAttribute.COMMENT_AUTHOR);
			TaskAttributeProperties.defaults().setType(TaskAttribute.TYPE_PERSON).applyTo(child);
			mapper.setRepositoryPerson(child, getAuthor());
		}
		if (getCreationDate() != null) {
			TaskAttribute child = taskAttribute.createAttribute(TaskAttribute.COMMENT_DATE);
			TaskAttributeProperties.defaults().setType(TaskAttribute.TYPE_DATE).applyTo(child);
			mapper.setDateValue(child, getCreationDate());
		}
		if (getNumber() != DEFAULT_NUMBER) {
			TaskAttribute child = taskAttribute.createAttribute(TaskAttribute.COMMENT_NUMBER);
			TaskAttributeProperties.defaults().setType(TaskAttribute.TYPE_INTEGER).applyTo(child);
			mapper.setIntegerValue(child, getNumber());
		}
		if (getUrl() != null) {
			TaskAttribute child = taskAttribute.createAttribute(TaskAttribute.COMMENT_URL);
			TaskAttributeProperties.defaults().setType(TaskAttribute.TYPE_URL).applyTo(child);
			mapper.setValue(child, getUrl());
		}
		if (getText() != null) {
			TaskAttribute child = taskAttribute.createAttribute(TaskAttribute.COMMENT_TEXT);
			TaskAttributeProperties.defaults().setType(TaskAttribute.TYPE_LONG_RICH_TEXT).applyTo(child);
			mapper.setValue(child, getText());
			taskAttribute.putMetaDataValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID, TaskAttribute.COMMENT_TEXT);
		}
	}

	public void applyTo(ITaskComment taskComment) {
		Assert.isNotNull(taskComment);
		if (getAuthor() != null) {
			taskComment.setAuthor(getAuthor());
		}
		if (getCreationDate() != null) {
			taskComment.setCreationDate(getCreationDate());
		}
		if (getNumber() != DEFAULT_NUMBER) {
			taskComment.setNumber(getNumber());
		}
		if (getUrl() != null) {
			taskComment.setUrl(getUrl());
		}
		if (getText() != null) {
			taskComment.setText(getText());
		}
	}
}
