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

import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;

/**
 * @author Mik Kersten
 * @author Rob Elves
 */
public class BugzillaTask extends AbstractRepositoryTask {

	private String severity;

	private String product;
	
	public BugzillaTask(String repositoryUrl, String id, String label, boolean newTask) {
		super(repositoryUrl, id, label, newTask);
		if (newTask) {
			setSyncState(RepositoryTaskSyncState.INCOMING);
		}
		setTaskUrl(BugzillaClient.getBugUrlWithoutLogin(repositoryUrl, id));
	}

//	public BugzillaTask(BugzillaQueryHit hit, boolean newTask) {
//		this(hit.getRepositoryUrl(), hit.getTaskId(), hit.getSummary(), newTask);
//		setPriority(hit.getPriority());
//	}

	@Override
	public String getTaskKind() {
		return IBugzillaConstants.BUGZILLA_TASK_KIND;
	}

	@Override
	public String toString() {
		return "Bugzilla task: " + getHandleIdentifier();
	}

	@Override
	public String getRepositoryKind() {
		return BugzillaCorePlugin.REPOSITORY_KIND;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

}
