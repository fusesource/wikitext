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
package org.eclipse.mylar.internal.bugzilla.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaServerFacade;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.internal.tasks.ui.ui.TaskUiUtil;

/**
 * @author Mik Kersten
 */
public class BugzillaHyperLink implements IHyperlink {

	private static final String SHOW_BUG_CGI = "/show_bug.cgi?id=";

	private IRegion region;

	private String id;

	private String repositoryUrl;

	public BugzillaHyperLink(IRegion nlsKeyRegion, String id, String repositoryUrl) {
		this.region = nlsKeyRegion;
		this.id = id;
		this.repositoryUrl = repositoryUrl;
	}

	public IRegion getHyperlinkRegion() {
		return region;
	}

	public String getTypeLabel() {
		return null;
	}

	public String getHyperlinkText() {
		return SHOW_BUG_CGI + id;
	}

	public void open() {
		// TaskRepository repository =
		// MylarTaskListPlugin.getRepositoryManager().getRepositoryForActiveTask(BugzillaPlugin.REPOSITORY_KIND);
		// TaskRepository repository =
		// MylarTaskListPlugin.getRepositoryManager().getDefaultRepository(
		// BugzillaPlugin.REPOSITORY_KIND);
		if (repositoryUrl != null) {
			TaskUiUtil.openRepositoryTask(repositoryUrl, id, repositoryUrl + BugzillaServerFacade.POST_ARGS_SHOW_BUG
					+ id);
			// OpenBugzillaReportJob job = new
			// OpenBugzillaReportJob(repository.getUrl(), id);
			// IProgressService service =
			// PlatformUI.getWorkbench().getProgressService();
			// try {
			// service.run(true, false, job);
			// } catch (Exception e) {
			// MylarStatusHandler.fail(e, "Could not open report", true);
			// }
		} else {
			MessageDialog.openError(null, IBugzillaConstants.TITLE_MESSAGE_DIALOG,
					"Could not determine repository for report");
		}
	}

}
