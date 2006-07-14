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

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.security.auth.login.LoginException;

import junit.framework.TestCase;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaPlugin;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaServerFacade;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.internal.bugzilla.ui.tasklist.BugzillaRepositorySettingsPage;
import org.eclipse.mylar.internal.tasks.ui.ui.wizards.EditRepositoryWizard;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TaskRepositoryManager;
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
		manager.clearRepositories();
		repository = new TaskRepository(BugzillaPlugin.REPOSITORY_KIND, IBugzillaConstants.TEST_BUGZILLA_222_URL);
		// Valid user name and password must be set for tests to pass
		try {
			Properties properties = new Properties();
			URL localURL = FileLocator.toFileURL(BugzillaTestPlugin.getDefault().getBundle().getEntry(
					"credentials.properties"));
			properties.load(new FileInputStream(new File(localURL.getFile())));
			repository.setAuthenticationCredentials(properties.getProperty("username"), properties
					.getProperty("password"));
		} catch (Throwable t) {
			fail("Must define credentials in <plug-in dir>/credentials.properties");
		}

		TasksUiPlugin.getRepositoryManager().addRepository(repository);
	}

	public void testValidationInvalidPassword() throws Exception {

		EditRepositoryWizard wizard = new EditRepositoryWizard(repository);
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
		dialog.create();
		BugzillaRepositorySettingsPage page = (BugzillaRepositorySettingsPage) wizard.getSettingsPage();
		BugzillaServerFacade.validateCredentials(page.getServerUrl(), page.getUserName(), page.getPassword());
		page.setPassword("bogus");
		try {
			BugzillaServerFacade.validateCredentials(page.getServerUrl(), page.getUserName(), page.getPassword());
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
		BugzillaServerFacade.validateCredentials(page.getServerUrl(), page.getUserName(), page.getPassword());
		page.setUserId("bogus");
		try {
			BugzillaServerFacade.validateCredentials(page.getServerUrl(), page.getUserName(), page.getPassword());
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
		BugzillaServerFacade.validateCredentials(page.getServerUrl(), page.getUserName(), page.getPassword());
		page.setUrl("http://invalid");
		try {
			BugzillaServerFacade.validateCredentials(page.getServerUrl(), page.getUserName(), page.getPassword());
		} catch (UnknownHostException e) {
			return;
		}
		fail("UnknownHostException didn't occur!");
	}

	public void testPersistChangeOfUrl() throws Exception {
		assertEquals(1, manager.getAllRepositories().size());
		String tempUid = repository.getUserName();
		String tempPass = repository.getPassword();
		EditRepositoryWizard wizard = new EditRepositoryWizard(repository);
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
		dialog.create();
		BugzillaRepositorySettingsPage page = (BugzillaRepositorySettingsPage) wizard.getSettingsPage();
		BugzillaServerFacade.validateCredentials(page.getServerUrl(), page.getUserName(), page.getPassword());
		page.setUrl(IBugzillaConstants.TEST_BUGZILLA_218_URL);
		wizard.performFinish();
		assertEquals(1, manager.getAllRepositories().size());
		TaskRepository repositoryTest = manager.getRepository(BugzillaPlugin.REPOSITORY_KIND,
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
		BugzillaServerFacade.validateCredentials(page.getServerUrl(), page.getUserName(), page.getPassword());
		page.setUserId("bogus");
		wizard.performFinish();
		assertEquals(1, manager.getAllRepositories().size());
		TaskRepository repositoryTest = manager.getRepository(BugzillaPlugin.REPOSITORY_KIND,
				IBugzillaConstants.TEST_BUGZILLA_222_URL);
		assertNotNull(repositoryTest);
		wizard = new EditRepositoryWizard(repositoryTest);
		dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
		dialog.create();
		page = (BugzillaRepositorySettingsPage) wizard.getSettingsPage();
		try {
			BugzillaServerFacade.validateCredentials(page.getServerUrl(), page.getUserName(), page.getPassword());
		} catch (LoginException e) {
			return;
		}
		fail("LoginException didn't occur!");
	}

}
