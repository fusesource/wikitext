/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.trac.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylar.internal.trac.core.ITracClient;
import org.eclipse.mylar.internal.trac.core.TracCorePlugin;
import org.eclipse.mylar.internal.trac.core.TracRepositoryConnector;
import org.eclipse.mylar.internal.trac.core.TracRepositoryQuery;
import org.eclipse.mylar.internal.trac.ui.wizard.EditTracQueryWizard;
import org.eclipse.mylar.internal.trac.ui.wizard.NewTracQueryWizard;
import org.eclipse.mylar.internal.trac.ui.wizard.TracCustomQueryPage;
import org.eclipse.mylar.internal.trac.ui.wizard.TracRepositorySettingsPage;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.TaskHyperlink;
import org.eclipse.mylar.tasks.ui.WebHyperlink;
import org.eclipse.mylar.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.mylar.tasks.ui.wizards.NewTaskWizard;
import org.eclipse.mylar.tasks.ui.wizards.NewWebTaskWizard;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class TracRepositoryUi extends AbstractRepositoryConnectorUi {

	Pattern taskPattern = Pattern.compile("#(\\d*)");

	Pattern wikiPattern = Pattern.compile("\\[wiki:([^\\]]*)\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
			| Pattern.MULTILINE);

	
	@Override
	public IHyperlink[] findHyperlinks(TaskRepository repository, String text, int lineOffset, int regionOffset) {
		List<IHyperlink> links = new ArrayList<IHyperlink>();

		Matcher m = taskPattern.matcher(text);
		while (m.find()) {
			String id = m.group(1);
			if (lineOffset >= m.start() && lineOffset <= m.end()) {
				IRegion linkRegion = new Region(regionOffset + m.start(), m.end() - m.start());
				links.add(new TaskHyperlink(linkRegion, repository, id));
			}
		}

		m = wikiPattern.matcher(text);
		while (m.find()) {
			String id = m.group(1);
			if (lineOffset >= m.start() && lineOffset <= m.end()) {
				IRegion linkRegion = new Region(regionOffset + m.start(), m.end() - m.start());
				links.add(new WebHyperlink(linkRegion, repository.getUrl() + ITracClient.WIKI_URL + id));
			}
		}

		return links.isEmpty() ? null : links.toArray(new IHyperlink[0]);
	}

	public String getTaskKindLabel(AbstractRepositoryTask repositoryTask) {
		return "Ticket";
	}
	
	@Override
	public AbstractRepositorySettingsPage getSettingsPage() {
		return new TracRepositorySettingsPage(this);
	}
	
	@Override
	public boolean hasRichEditor() {
		return true;
	}

	@Override
	public WizardPage getSearchPage(TaskRepository repository, IStructuredSelection selection) {
		return new TracCustomQueryPage(repository);
	}

	@Override
	public boolean hasSearchPage() {
		return true;
	}
	
	@Override
	public IWizard getNewTaskWizard(TaskRepository repository) {
		if (TracRepositoryConnector.hasRichEditor(repository)) {
			return new NewTaskWizard(repository);
		} else {
			return new NewWebTaskWizard(repository, repository.getUrl() + ITracClient.NEW_TICKET_URL);
		}
	}
	
	@Override
	public IWizard getQueryWizard(TaskRepository repository, AbstractRepositoryQuery query) {
		if (query instanceof TracRepositoryQuery) {
			return new EditTracQueryWizard(repository, query);
		} else {
			return new NewTracQueryWizard(repository);
		}
	}

	@Override
	public String getRepositoryType() {
		return TracCorePlugin.REPOSITORY_KIND;
	}

}
