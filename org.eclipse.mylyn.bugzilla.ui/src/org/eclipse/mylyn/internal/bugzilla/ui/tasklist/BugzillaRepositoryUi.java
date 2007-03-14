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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaRepositoryQuery;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.internal.bugzilla.ui.search.BugzillaSearchPage;
import org.eclipse.mylar.internal.bugzilla.ui.wizard.NewBugzillaTaskWizard;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.TaskHyperlink;
import org.eclipse.mylar.tasks.ui.search.AbstractRepositoryQueryPage;
import org.eclipse.mylar.tasks.ui.wizards.AbstractRepositorySettingsPage;

/**
 * @author Mik Kersten
 * @author Eugene Kuleshov
 */
public class BugzillaRepositoryUi extends AbstractRepositoryConnectorUi {
	private static final int TASK_NUM_GROUP = 3;

	private static final String regexp = "(duplicate of|bug|task)(\\s#|#|#\\s|\\s|)(\\s\\d+|\\d+)";

	private static final Pattern PATTERN = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);

	@Override
	public IHyperlink[] findHyperlinks(TaskRepository repository, String text, int lineOffset, int regionOffset) {
		ArrayList<IHyperlink> hyperlinksFound = new ArrayList<IHyperlink>();

		Matcher m = PATTERN.matcher(text);
		while (m.find()) {
			if (lineOffset >= m.start() && lineOffset <= m.end()) {
				IHyperlink link = extractHyperlink(repository, regionOffset, m);
				if (link != null)
					hyperlinksFound.add(link);
			}
		}

		if (hyperlinksFound.size() > 0) {
			return hyperlinksFound.toArray(new IHyperlink[1]);
		}
		return null;
	}

	private static IHyperlink extractHyperlink(TaskRepository repository, int regionOffset, Matcher m) {

		int start = -1;

		if (m.group().startsWith("duplicate")) {
			start = m.start() + m.group().indexOf(m.group(TASK_NUM_GROUP));
		} else {
			start = m.start();
		}

		int end = m.end();

		if (end == -1)
			end = m.group().length();

		try {

			String bugId = m.group(TASK_NUM_GROUP).trim();
			start += regionOffset;
			end += regionOffset;

			IRegion sregion = new Region(start, end - start);
			return new TaskHyperlink(sregion, repository, bugId);

		} catch (NumberFormatException e) {
			return null;
		}
	}
	

	public String getTaskKindLabel(AbstractRepositoryTask repositoryTask) {
		return IBugzillaConstants.BUGZILLA_TASK_KIND;
	}
	
	@Override
	public AbstractRepositorySettingsPage getSettingsPage() {
		return new BugzillaRepositorySettingsPage(this);
	}

	@Override
	public AbstractRepositoryQueryPage getSearchPage(TaskRepository repository, IStructuredSelection selection) {
		return new BugzillaSearchPage(repository);
	}

	@Override
	public IWizard getNewTaskWizard(TaskRepository taskRepository) {
		return new NewBugzillaTaskWizard(taskRepository);
	}

	@Override
	public IWizard getQueryWizard(TaskRepository repository, AbstractRepositoryQuery query) {
		if (query instanceof BugzillaRepositoryQuery) {
			return new EditBugzillaQueryWizard(repository, (BugzillaRepositoryQuery) query);
		} else {
			return new NewBugzillaQueryWizard(repository);
		}
	}

	@Override
	public boolean hasSearchPage() {
		return true;
	}

	@Override
	public boolean hasRichEditor() {
		return true;
	}

	@Override
	public String getRepositoryType() {
		return BugzillaCorePlugin.REPOSITORY_KIND;
	}

}
