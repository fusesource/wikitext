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
/*
 * Created on 14-Jan-2005
 */
package org.eclipse.mylar.internal.bugzilla.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.TaskComment;

/**
 * @author Mik Kersten
 */
public class BugzillaTask extends AbstractRepositoryTask {

	private static final String DEADLINE_FORMAT = "yyyy-MM-dd";

	private static final String COMMENT_FORMAT = "yyyy-MM-dd HH:mm";

	public BugzillaTask(String repositoryUrl, String id, String label, boolean newTask) {
		super(repositoryUrl, id, label, newTask);
		if (newTask) {
			setSyncState(RepositoryTaskSyncState.INCOMING);
		}
		isDirty = false;
		initTaskUrl(taskId);
	}

	public BugzillaTask(BugzillaQueryHit hit, boolean newTask) {
		this(hit.getRepositoryUrl(), hit.getTaskId(), hit.getSummary(), newTask);
		setPriority(hit.getPriority());
		initTaskUrl(taskId);
	}

	private void initTaskUrl(String taskId) {
		try {
			String url = BugzillaClient.getBugUrlWithoutLogin(repositoryUrl, Integer.parseInt(taskId));
			if (url != null) {
				super.setTaskUrl(url);
			}
		} catch (Exception e) {
			MylarStatusHandler.fail(e, "Task initialization failed due to malformed id or URL: "
					+ getHandleIdentifier(), false);
		}
	}

	@Override
	public String getSummary() {
		if (this.isDownloaded() || !super.getSummary().startsWith("<")) {
			return super.getSummary();
		} else {
			if (isSynchronizing()) {
				return "<synchronizing>";
			} else {
				return "";
			}
		}
	}

	@Override
	public String getTaskKind() {
		return IBugzillaConstants.BUGZILLA_TASK_KIND;
	}

	@Override
	public String toString() {
		return "bugzilla report taskId: " + getHandleIdentifier();
	}

	@Override
	public boolean isCompleted() {
		if (taskData != null) {
			if (taskData.getStatus() != null) {
				return taskData.getStatus().equals(IBugzillaConstants.VALUE_STATUS_RESOLVED)
						|| taskData.getStatus().equals(IBugzillaConstants.VALUE_STATUS_CLOSED)
						|| taskData.getStatus().equals(IBugzillaConstants.VALUE_STATUS_VERIFIED);
			} else {
				return false;
			}
		} else {
			return super.isCompleted();
		}
	}

	@Override
	public Date getCompletionDate() {
		try {
			if (taskData != null) {
				if (isCompleted()) {
					List<TaskComment> taskComments = taskData.getComments();
					if (taskComments != null && !taskComments.isEmpty()) {
						// TODO: fix not to be based on comment
						return new SimpleDateFormat(COMMENT_FORMAT).parse(taskComments.get(taskComments.size() - 1)
								.getCreated());
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		return super.getCompletionDate();
	}

	@Override
	public String getRepositoryKind() {
		return BugzillaCorePlugin.REPOSITORY_KIND;
	}

	@Override
	public String getPriority() {
		if (taskData != null && taskData.getAttribute(BugzillaReportElement.PRIORITY.getKeyString()) != null) {
			return taskData.getAttribute(BugzillaReportElement.PRIORITY.getKeyString()).getValue();
		} else {
			return super.getPriority();
		}
	}

	@Override
	public String getOwner() {
		if (taskData != null && taskData.getAttribute(BugzillaReportElement.ASSIGNED_TO.getKeyString()) != null) {
			return taskData.getAttribute(BugzillaReportElement.ASSIGNED_TO.getKeyString()).getValue();
		} else {
			return super.getOwner();
		}
	}

	@Override
	public Date getDueDate() {

		if (taskData != null && taskData.getAttribute(BugzillaReportElement.ESTIMATED_TIME.getKeyString()) != null) {
			// HACK: if estimated_time field exists, time tracking is enabled
			try {
				String dueStr = taskData.getAttributeValue(BugzillaReportElement.DEADLINE.getKeyString());
				if (dueStr != null) {
					return new SimpleDateFormat(DEADLINE_FORMAT).parse(dueStr);
				}
			} catch (Exception e) {
				return null;
			}
			return null;
		} else {
			return super.getDueDate();
		}

	}
}
