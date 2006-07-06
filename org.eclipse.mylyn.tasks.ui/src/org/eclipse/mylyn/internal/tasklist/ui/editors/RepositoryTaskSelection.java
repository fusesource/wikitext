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

package org.eclipse.mylar.internal.tasklist.ui.editors;

import org.eclipse.mylar.internal.tasklist.Comment;

/**
 * A selection of an element in a view.
 */
public class RepositoryTaskSelection implements IRepositoryTaskSelection {

	/**
	 * The id of the Bugzilla object that the selection was on.
	 */
	protected String id;

	/** The server of the Bugzilla object that the selection was on. */
	protected String server;

	/** The contents of the selection. */
	protected String contents;

	protected String bugSummary;

	/**
	 * The comment, if a comment was selected. If the selection was not a
	 * comment, then this is <code>null</code>.
	 */
	protected Comment comment;

	/**
	 * Creates a new <code>RepositoryTaskSelection</code> with no supplied
	 * contents or comment.
	 * 
	 * @param id
	 *            The id of the Bugzilla object that the selection was on.
	 * @param server
	 *            The server of the Bugzilla object that the selection was on.
	 */
	public RepositoryTaskSelection(String id, String server, String summary) {
		this(id, server, null, null, summary);
	}

	/**
	 * Creates a new <code>RepositoryTaskSelection</code> with no supplied
	 * comment.
	 * 
	 * @param id
	 *            The id of the Bugzilla object that the selection was on.
	 * @param server
	 *            The server of the Bugzilla object that the selection was on.
	 * @param contents
	 *            The contents of the selection.
	 */
	public RepositoryTaskSelection(String id, String server, String contents, boolean isDescription, String summary) {
		this(id, server, contents, null, summary);
		this.isDescription = isDescription;
	}

	/**
	 * Creates a new <code>RepositoryTaskSelection</code> with no supplied
	 * contents.
	 * 
	 * @param id
	 *            The id of the Bugzilla object that the selection was on.
	 * @param server
	 *            The server of the Bugzilla object that the selection was on.
	 * @param comment
	 *            The <code>Comment</code> object for this selection. If a
	 *            comment was not selected, then this should be
	 *            <code>null</code>.
	 */
	public RepositoryTaskSelection(String id, String server, Comment comment, String summary) {
		this(id, server, null, comment, summary);
	}

	/**
	 * Creates a new <code>RepositoryTaskSelection</code>.
	 * 
	 * @param id
	 *            The id of the Bugzilla object that the selection was on.
	 * @param server
	 *            The server of the Bugzilla object that the selection was on.
	 * @param contents
	 *            The contents of the selection.
	 * @param comment
	 *            The <code>Comment</code> object for this selection. If a
	 *            comment was not selected, then this should be
	 *            <code>null</code>.
	 */
	public RepositoryTaskSelection(String id, String server, String contents, Comment comment, String summary) {
		this.id = id;
		this.server = server;
		this.contents = contents;
		this.comment = comment;
		this.bugSummary = summary;
	}

	public boolean hasComment() {
		return comment != null;
	}

	public Comment getComment() {
		return comment;
	}

	public void setComment(Comment comment) {
		this.comment = comment;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public boolean isEmpty() {
		return (server == null) || ((contents == null) && (comment == null));
	}

	private boolean isCommentHeader = false;

	private boolean isDescription = false;

	public boolean isCommentHeader() {
		return isCommentHeader;
	}

	public boolean isDescription() {
		return isDescription;
	}

	public void setIsCommentHeader(boolean isCommentHeader) {
		this.isCommentHeader = isCommentHeader;
	}

	public void setIsDescription(boolean isDescription) {
		this.isDescription = isDescription;
	}

	public String getBugSummary() {
		return bugSummary;
	}
}
