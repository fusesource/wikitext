/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.tasks.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This data structure is not to be subclassed but rather used directly to hold
 * repository task data (attribute key, value pairs along with valid options for
 * each attribute).
 * 
 * @author Mik Kersten
 * @author Rob Elves
 */
public final class RepositoryTaskData extends AttributeContainer implements Serializable {

	private static final long serialVersionUID = 2304511248225237689L;

	private boolean hasLocalChanges = false;

	private boolean isNew = false;

	public static final String VAL_STATUS_NEW = "NEW";

	private String reportID;

	private String repositoryURL;

	private List<TaskComment> taskComments = new ArrayList<TaskComment>();

	private List<RepositoryAttachment> attachments = new ArrayList<RepositoryAttachment>();

	/** The operation that was selected to do to the bug */
	protected RepositoryOperation selectedOperation = null;

	/** The repositoryOperations that can be done on the report */
	protected List<RepositoryOperation> repositoryOperations = new ArrayList<RepositoryOperation>();

	/** The bugs valid keywords */
	protected List<String> validKeywords;

	/** Description of the bug */
	protected String description;

	/** Creation timestamp */
	protected Date created;

	/** Modification timestamp */
	protected Date lastModified = null;

	protected String repositoryKind;

	public RepositoryTaskData(AbstractAttributeFactory factory, String repositoryKind, String repositoryURL, String id) {
		super(factory);
		this.reportID = id;
		this.repositoryKind = repositoryKind;
		this.repositoryURL = repositoryURL;
	}

	public String getLabel() {
		if (isNew()) {
			return "<unsubmitted> " + this.getRepositoryUrl();
		} else {
			return getSummary();
		}
	}

	/**
	 * Get the resolution of the bug
	 * 
	 * @return The resolution of the bug
	 */
	public String getResolution() {
		return getAttributeValue(RepositoryTaskAttribute.RESOLUTION);
	}

	/**
	 * Get the status of the bug
	 * 
	 * @return The bugs status
	 */
	public String getStatus() {
		return getAttributeValue(RepositoryTaskAttribute.STATUS);
	}

	public String getLastModified() {
		return getAttributeValue(RepositoryTaskAttribute.DATE_MODIFIED);
	}

	public void setSelectedOperation(RepositoryOperation o) {
		selectedOperation = o;
	}

	public RepositoryOperation getSelectedOperation() {
		return selectedOperation;
	}

	/**
	 * Get all of the repositoryOperations that can be done to the bug
	 * 
	 * @return The repositoryOperations that can be done to the bug
	 */
	public List<RepositoryOperation> getOperations() {
		return repositoryOperations;
	}

	/**
	 * Get the person who reported the bug
	 * 
	 * @return The person who reported the bug
	 */
	public String getReporter() {
		return getAttributeValue(RepositoryTaskAttribute.USER_REPORTER);
	}

	/**
	 * Get an operation from the bug based on its display name
	 * 
	 * @param displayText
	 *            The display text for the operation
	 * @return The operation that has the display text
	 */
	public RepositoryOperation getOperation(String displayText) {
		Iterator<RepositoryOperation> itr = repositoryOperations.iterator();
		while (itr.hasNext()) {
			RepositoryOperation o = itr.next();
			String opName = o.getOperationName();
			opName = opName.replaceAll("</.*>", "");
			opName = opName.replaceAll("<.*>", "");
			if (opName.equals(displayText))
				return o;
		}
		return null;
	}

	/**
	 * Get the summary for the bug
	 * 
	 * @return The bugs summary
	 */
	public String getSummary() {
		return getAttributeValue(RepositoryTaskAttribute.SUMMARY);
	}

	public void setSummary(String summary) {
		setAttributeValue(RepositoryTaskAttribute.SUMMARY, summary);
	}

	public String getProduct() {
		return getAttributeValue(RepositoryTaskAttribute.PRODUCT);
	}

	/**
	 * true if this is a new, unsubmitted task false otherwise
	 */
	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

	/**
	 * Get the date that the bug was created
	 * 
	 * @return The bugs creation date
	 */
	public String getCreated() {
		return getAttributeValue(RepositoryTaskAttribute.DATE_CREATION);
	}

	/**
	 * Get the keywords for the bug
	 * 
	 * @return The keywords for the bug
	 */
	public List<String> getKeywords() {

		// get the selected keywords for the bug
		StringTokenizer st = new StringTokenizer(getAttributeValue(RepositoryTaskAttribute.KEYWORDS), ",", false);
		List<String> keywords = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String s = st.nextToken().trim();
			keywords.add(s);
		}

		return keywords;
	}

	/**
	 * Add an operation to the bug
	 * 
	 * @param o
	 *            The operation to add
	 */
	public void addOperation(RepositoryOperation o) {
		repositoryOperations.add(o);
	}

	public List<String> getCC() {
		return getAttributeValues(RepositoryTaskAttribute.USER_CC);
	}

	public void removeCC(String email) {
		removeAttributeValue(RepositoryTaskAttribute.USER_CC, email);
	}

	public String getAssignedTo() {
		return getAttributeValue(RepositoryTaskAttribute.USER_ASSIGNED);
	}

	/**
	 * Get the new comment that is to be added to the bug
	 */
	public String getNewComment() {
		RepositoryTaskAttribute attribute = getAttribute(RepositoryTaskAttribute.COMMENT_NEW);
		return (attribute != null) ? attribute.getValue() : "";
	}

	/**
	 * Set the new comment that will be added to the bug
	 */
	public void setNewComment(String newComment) {
		setAttributeValue(RepositoryTaskAttribute.COMMENT_NEW, newComment);
	}

	public void addComment(TaskComment taskComment) {
		taskComments.add(taskComment);
	}

	public List<TaskComment> getComments() {
		return taskComments;
	}

	public void setDescription(String description) {
		setAttributeValue(RepositoryTaskAttribute.DESCRIPTION, description);
	}

	public String getDescription() {
		RepositoryTaskAttribute attribute = getDescriptionAttribute();
		return (attribute != null) ? attribute.getValue() : "";
	}

	public RepositoryTaskAttribute getDescriptionAttribute() {
		RepositoryTaskAttribute attribute = getAttribute(RepositoryTaskAttribute.DESCRIPTION);
		// TODO: Remove the following after 1.0 release as we now just have a
		// summary attribute
		if (attribute == null) {
			List<TaskComment> coms = this.getComments();
			if (coms != null && coms.size() > 0) {
				return coms.get(0).getAttribute(RepositoryTaskAttribute.COMMENT_TEXT);
			}
		}
		return attribute;
	}

	public void addAttachment(RepositoryAttachment attachment) {
		attachments.add(attachment);
	}

	public List<RepositoryAttachment> getAttachments() {
		return attachments;
	}

	public String getId() {
		return reportID;
	}

	/**
	 * @return the server for this report
	 */
	public String getRepositoryUrl() {
		return repositoryURL;
	}

	public boolean hasLocalChanges() {
		return hasLocalChanges;
	}

	public void setHasLocalChanges(boolean b) {
		hasLocalChanges = b;
	}

	@Override
	public List<String> getAttributeValues(String key) {
		RepositoryTaskAttribute attribute = getAttribute(key);
		if (attribute != null) {
			return attribute.getValues();
		}
		return new ArrayList<String>();
	}

	public void removeAttributeValue(String key, String value) {
		RepositoryTaskAttribute attrib = getAttribute(key);
		if (attrib != null) {
			attrib.removeValue(value);
		}
	}

	public String getRepositoryKind() {
		return repositoryKind;
	}

	@Override
	public void setAttributeFactory(AbstractAttributeFactory factory) {
		super.setAttributeFactory(factory);
		for (TaskComment taskComment : taskComments) {
			taskComment.setAttributeFactory(factory);
		}
		for (RepositoryAttachment attachment : attachments) {
			attachment.setAttributeFactory(factory);
		}
	}

}
