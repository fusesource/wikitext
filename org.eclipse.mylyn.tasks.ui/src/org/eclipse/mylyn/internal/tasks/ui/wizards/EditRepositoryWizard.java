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

package org.eclipse.mylar.internal.tasks.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylar.internal.tasks.ui.TaskListImages;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * @author Mik Kersten
 */
public class EditRepositoryWizard extends Wizard implements INewWizard {

	private static final String TITLE = "Task Repository Settings";

	private AbstractRepositorySettingsPage abstractRepositorySettingsPage;

	private TaskRepository repository;

	public EditRepositoryWizard(TaskRepository repository) {
		super();
		this.repository = repository;
		AbstractRepositoryConnectorUi connectorUi = TasksUiPlugin.getRepositoryUi(
				repository.getKind());
		abstractRepositorySettingsPage = connectorUi.getSettingsPage();
		abstractRepositorySettingsPage.setRepository(repository);
		abstractRepositorySettingsPage.setVersion(repository.getVersion());
		abstractRepositorySettingsPage.setWizard(this);		
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(TaskListImages.BANNER_REPOSITORY);
		setWindowTitle(TITLE);
	}

	/**
	 * Custom properties should be set on the repository object to ensure they are saved.
	 */
	@Override
	public boolean performFinish() {
		if (canFinish()) {
			String oldUrl = repository.getUrl();
			String newUrl = abstractRepositorySettingsPage.getServerUrl();
			TasksUiPlugin.getTaskListManager().refactorRepositoryUrl(oldUrl, newUrl);
			
			repository.flushAuthenticationCredentials();
			repository.setUrl(newUrl);
			repository.setVersion(abstractRepositorySettingsPage.getVersion());
			repository.setCharacterEncoding(abstractRepositorySettingsPage.getCharacterEncoding());
			repository.setAuthenticationCredentials(abstractRepositorySettingsPage.getUserName(), abstractRepositorySettingsPage.getPassword());			
			repository.setRepositoryLabel(abstractRepositorySettingsPage.getRepositoryLabel());
			
			repository.setProperty(TaskRepository.AUTH_HTTP_USERNAME, abstractRepositorySettingsPage.getHttpAuthUserId());
			repository.setProperty(TaskRepository.AUTH_HTTP_PASSWORD, abstractRepositorySettingsPage.getHttpAuthPassword());
			
			repository.setProperty(TaskRepository.PROXY_USEDEFAULT, String.valueOf(abstractRepositorySettingsPage.getUseDefaultProxy()));
			repository.setProperty(TaskRepository.PROXY_HOSTNAME, abstractRepositorySettingsPage.getProxyHostname());
			repository.setProperty(TaskRepository.PROXY_PORT, abstractRepositorySettingsPage.getProxyPort());
			repository.setProxyAuthenticationCredentials(abstractRepositorySettingsPage.getProxyUsername(), abstractRepositorySettingsPage.getProxyPassword());
			
			
			abstractRepositorySettingsPage.updateProperties(repository);
			TasksUiPlugin.getRepositoryManager().notifyRepositorySettingsChagned(repository);
			TasksUiPlugin.getRepositoryManager().saveRepositories(TasksUiPlugin.getDefault().getRepositoriesFilePath());
			return true;
		}
		return false;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		addPage(abstractRepositorySettingsPage);
	}

	@Override
	public boolean canFinish() {
		return abstractRepositorySettingsPage.isPageComplete();
	}
	
	/** public for testing */
	public AbstractRepositorySettingsPage getSettingsPage() {
		return abstractRepositorySettingsPage;
	}

	public TaskRepository getRepository() {
		return repository;
	}
}
