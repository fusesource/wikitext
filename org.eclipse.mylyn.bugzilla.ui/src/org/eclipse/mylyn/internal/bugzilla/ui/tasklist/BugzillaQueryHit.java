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

package org.eclipse.mylar.internal.bugzilla.ui.tasklist;

import org.eclipse.mylar.internal.bugzilla.core.BugzillaServerFacade;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * @author Ken Sueda
 * @author Mik Kersten (hardening of prototype)
 */
public class BugzillaQueryHit extends AbstractQueryHit {

	private BugzillaTask task;

	private String status;

	public BugzillaQueryHit(String description, String priority, String repositoryUrl, String id, BugzillaTask task, String status) {
		super(repositoryUrl, description, id);
		super.priority = priority;
		this.task = task;
		this.status = status;
	}

	public BugzillaTask getCorrespondingTask() {
		return task;
	}

	public void setCorrespondingTask(AbstractRepositoryTask task) {
		if (task instanceof BugzillaTask) {
			this.task = (BugzillaTask)task;
		}
	}

	public String getPriority() {
		if (task != null) {
			return task.getPriority();
		} else {
			return priority;
		}
	}

	public String getDescription() {
		// return HtmlStreamTokenizer.unescape(description);
		if (task != null) {
			return task.getDescription();
		} else {
			return description;
		}
	}

	public String getUrl() {
		Integer idInt = new Integer(id);
		return BugzillaServerFacade.getBugUrlWithoutLogin(repositoryUrl, idInt);
	}

	public AbstractRepositoryTask getOrCreateCorrespondingTask() { 
		
		ITask existingTask = TasksUiPlugin.getTaskListManager().getTaskList().getTask(
				getHandleIdentifier());
		 
		if (existingTask instanceof BugzillaTask) {
			this.task = (BugzillaTask)existingTask;
		} else {
			task = new BugzillaTask(this, true);
			//task.setSyncState(RepositoryTaskSyncState.INCOMING);
			TasksUiPlugin.getTaskListManager().getTaskList().addTask(task);			
		} 	
		return task;
	}

	public boolean isCompleted() {
		if (status != null
				&& (status.startsWith("RESO") || status.startsWith("CLO") || status.startsWith("VERI") || status
						.startsWith("FIXED"))) {
			return true;
		} 
		return false;
	}

	public void setHandleIdentifier(String id) {
		// can't change the handle
	}
	
}
