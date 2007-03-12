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
package org.eclipse.mylar.tasks.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.mylar.tasks.core.TaskRepository;

/**
 * @author Eugene Kuleshov
 * @author Steffen Pingel
 */
public class TaskHyperlink implements IHyperlink {

	private final IRegion region;

	private final TaskRepository repository;

	private final String key;

	public TaskHyperlink(IRegion region, TaskRepository repository, String key) {
		this.region = region;
		this.repository = repository;
		this.key = key;
	}

	public IRegion getHyperlinkRegion() {
		return region;
	}

	public String getTypeLabel() {
		return null;
	}

	public String getHyperlinkText() {
		return "Open Task " + key;
	}

	public void open() {
		if (repository != null) {
			TasksUiUtil.openRepositoryTask(repository, key);
		} else {
			MessageDialog.openError(null, "Mylar", "Could not determine repository for report");
		}
	}

}
