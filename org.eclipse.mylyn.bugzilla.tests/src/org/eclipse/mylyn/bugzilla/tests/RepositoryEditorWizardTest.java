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

package org.eclipse.mylar.bugzilla.tests;

import java.net.UnknownHostException;

import javax.security.auth.login.LoginException;

import junit.framework.TestCase;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.mylar.context.tests.support.MylarTestUtils;
import org.eclipse.mylar.context.tests.support.MylarTestUtils.Credentials;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaClientFactory;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaClient;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.internal.bugzilla.ui.tasklist.BugzillaRepositorySettingsPage;
import org.eclipse.mylar.internal.tasks.ui.wizards.EditRepositoryWizard;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.core.TaskRepositoryManager;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.PlatformUI;

/**
 * @author Rob Elves
 */
public class RepositoryEditorWizardTest extends TestCase {

	TaskRepositoryManager manager;

	TaskRepository repository;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		manager = TasksUiPlugin.getRepositoryManager();
		manager.clearRepositories(TasksUiPlugin.getDefault().getRepositoriesFilePath());
		repository = new TaskRepository(BugzillaCorePlugin.REPOSITORY_KIND, IBugzillaConstants.TEST_BUGZILLA_222_URL);
		Credentials credentials = MylarTestUtils.readCredentials();
		repository.setAuthenticationCredentials(credentials.username, credentials.password);

		TasksUiPlugin.getRepositoryManager().addRepository(repository,
				TasksUiPlugin.getDefault().getRepositoriesFilePath());
	}

	public void testValidationInvalidPassword() throws Exception {

		EditRepositoryWizard wizard = new EditRepositoryWizard(repository);
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
		dialog.create();
		BugzillaRepositorySettingsPage page = (BugzillaRepositorySettingsPage) wizard.getSettingsPage();
		// BugzillaClient client =
		// BugzillaClientFactory.createClient(page.getServerUrl(),
		// page.getUserName(), page.getPassword(), page.getHttpAuthUserId(),
		// page.getHttpAuthPassword(), page.getCharacterEncoding());
		page.setPassword("bogus");
		try {
			BugzillaClient client = BugzillaClientFactory.createClient(page.getServerUrl(), page.getUserName(), page
					.getPassword(), page.getHttpAuthUserId(), page.getHttpAuthPassword(), page.getCharacterEncoding());
			client.validate();
		} catch (LoginException e) {
			return;
		}
		fail("LoginException didn't occur!");
	}

	public void testValidationInvalidUserid() throws Exception {
		EditRepositoryWizard wizard = new EditRepositoryWizard(repository);
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
		dialog.create();
		BugzillaRepositorySettingsPage page = (BugzillaRepositorySettingsPage) wizard.getSettingsPage();
		page.setUserId("bogus");
		try {
			BugzillaClient client = BugzillaClientFactory.createClient(page.getServerUrl(), page.getUserName(), page
					.getPassword(), page.getHttpAuthUserId(), page.getHttpAuthPassword(), page.getCharacterEncoding());
			client.validate();
		} catch (LoginException e) {
			return;
		}
		fail("LoginException didn't occur!");
	}

	public void testValidationInvalidUrl() throws Exception {
		EditRepositoryWizard wizard = new EditRepositoryWizard(repository);
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
		dialog.create();
		BugzillaRepositorySettingsPage page = (BugzillaRepositorySettingsPage) wizard.getSettingsPage();
		page.setUrl("http://invalid");
		try {
			BugzillaClient client = BugzillaClientFactory.createClient(page.getServerUrl(), page.getUserName(), page
					.getPassword(), page.getHttpAuthUserId(), page.getHttpAuthPassword(), page.getCharacterEncoding());
			client.validate();
		} catch (UnknownHostException e) {
			return;
		}
		fail("UnknownHostException didn't occur!");
	}

	// TODO: Test locking up?
//	public void testAutoVersion() throws Exception {		
//		repository.setVersion(BugzillaRepositorySettingsPage.LABEL_AUTOMATIC_VERSION);
//		EditRepositoryWizard wizard = new EditRepositoryWizard(repository);
//		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
//		dialog.create();
//		BugzillaRepositorySettingsPage page = (BugzillaRepositorySettingsPage) wizard.getSettingsPage();
//		page.setTesting(true);
//		assertEquals(BugzillaRepositorySettingsPage.LABEL_AUTOMATIC_VERSION, page.getVersion());
//		page.validateSettings();
//		assertEquals("2.22", page.getVersion());
//	}
	
	public void testPersistChangeOfUrl() throws Exception {
		assertEquals(1, manager.getAllRepositories().size());
		String tempUid = repository.getUserName();
		String tempPass = repository.getPassword();
		EditRepositoryWizard wizard = new EditRepositoryWizard(repository);
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
		dialog.create();
		BugzillaRepositorySettingsPage page = (BugzillaRepositorySettingsPage) wizard.getSettingsPage();
		BugzillaClient client = BugzillaClientFactory.createClient(page.getServerUrl(), page.getUserName(), page
				.getPassword(), page.getHttpAuthUserId(), page.getHttpAuthPassword(), page.getCharacterEncoding());
		client.validate();
		page.setUrl(IBugzillaConstants.TEST_BUGZILLA_218_URL);
		wizard.performFinish();
		assertEquals(1, manager.getAllRepositories().size());
		TaskRepository repositoryTest = manager.getRepository(BugzillaCorePlugin.REPOSITORY_KIND,
				IBugzillaConstants.TEST_BUGZILLA_218_URL);
		assertNotNull(repositoryTest);
		assertEquals(tempUid, repositoryTest.getUserName());
		assertEquals(tempPass, repositoryTest.getPassword());
	}

	public void testPersistChangeUserId() throws Exception {
		assertEquals(1, manager.getAllRepositories().size());
		EditRepositoryWizard wizard = new EditRepositoryWizard(repository);
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
		dialog.create();
		BugzillaRepositorySettingsPage page = (BugzillaRepositorySettingsPage) wizard.getSettingsPage();
		BugzillaClient client = BugzillaClientFactory.createClient(page.getServerUrl(), page.getUserName(), page
				.getPassword(), page.getHttpAuthUserId(), page.getHttpAuthPassword(), page.getCharacterEncoding());
		client.validate();
		page.setUserId("bogus");
		wizard.performFinish();
		assertEquals(1, manager.getAllRepositories().size());
		TaskRepository repositoryTest = manager.getRepository(BugzillaCorePlugin.REPOSITORY_KIND,
				IBugzillaConstants.TEST_BUGZILLA_222_URL);
		assertNotNull(repositoryTest);
		wizard = new EditRepositoryWizard(repositoryTest);
		dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
		dialog.create();
		page = (BugzillaRepositorySettingsPage) wizard.getSettingsPage();
		try {
			client = BugzillaClientFactory.createClient(page.getServerUrl(), page.getUserName(), page.getPassword(),
					page.getHttpAuthUserId(), page.getHttpAuthPassword(), page.getCharacterEncoding());
			client.validate();
		} catch (LoginException e) {
			return;
		}
		fail("LoginException didn't occur!");
	}

}
