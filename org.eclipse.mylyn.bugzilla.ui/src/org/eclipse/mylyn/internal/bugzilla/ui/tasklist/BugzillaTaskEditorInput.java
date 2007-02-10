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
 * Created on 1-Feb-2005
 */
package org.eclipse.mylar.internal.bugzilla.ui.tasklist;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaTask;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.editors.RepositoryTaskEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * @author Eric Booth
 * @author Mik Kersten
 * @author Rob Elves
 */
public class BugzillaTaskEditorInput extends RepositoryTaskEditorInput {

	private String bugTitle = "";

	private BugzillaTask bugTask;

	public BugzillaTaskEditorInput(TaskRepository repository, BugzillaTask bugTask, boolean offline) {
		super(repository, bugTask.getHandleIdentifier(), bugTask.getTaskUrl());
		this.bugTask = bugTask;
		updateOptions(getTaskData());
		updateOptions(getOldTaskData());
	}

	protected void setBugTitle(String str) {
		// 03-20-03 Allows editor to store title (once it is known)
		bugTitle = str;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return bugTitle;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	/**
	 * @return Returns the <code>BugzillaTask</code>
	 */
	public BugzillaTask getBugTask() {
		return bugTask;
	}

	// TODO: repository configuration update (remove at some point)
	private void updateOptions(RepositoryTaskData taskData) {
		try {
			if (taskData != null) {
				BugzillaRepositoryConnector bugzillaConnector = (BugzillaRepositoryConnector) TasksUiPlugin
						.getRepositoryManager().getRepositoryConnector(BugzillaCorePlugin.REPOSITORY_KIND);
				bugzillaConnector.updateAttributeOptions(repository, taskData);
			}
		} catch (Exception e) {
			// ignore
		}
	}
}
