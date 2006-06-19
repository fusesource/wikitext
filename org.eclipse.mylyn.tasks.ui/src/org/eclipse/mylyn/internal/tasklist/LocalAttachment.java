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

package org.eclipse.mylar.internal.tasklist;

import java.io.Serializable;

/**
 * A class representing a local attachment.
 * 
 * @author Jeff Pound
 */
public class LocalAttachment implements Serializable {

	private static final long serialVersionUID = -4477699536552617389L;

	/** The report to which this attachment will be attached */
	private RepositoryTaskData repositoryTaskData;

	private String filePath;
	
	private String comment = "";
	
	private String description = "";
	
	private String contentType = "";
	
	private boolean isPatch = false;

	private boolean deleteAfterUpload = false;

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isPatch() {
		return isPatch;
	}

	public void setPatch(boolean isPatch) {
		this.isPatch = isPatch;
	}

	public RepositoryTaskData getReport() {
		return repositoryTaskData;
	}

	public void setReport(RepositoryTaskData report) {
		this.repositoryTaskData = report;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}


	public void setDeleteAfterUpload(boolean d) {
		deleteAfterUpload = d;
	}

	public boolean getDeleteAfterUpload() {
		return deleteAfterUpload;
	}
}
