/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.tasks.tests;

import junit.framework.TestCase;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.mylar.internal.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.tests.connector.MockRepositoryUi;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @author Steffen Pingel
 */
public class RepositorySettingsPageTest extends TestCase {

	public void testNeedsEncoding() {
		TaskRepository repository = new TaskRepository("kind", "http://localhost/");
		repository.setCharacterEncoding("UTF-8");

		MockRepositorySettingsPage page = new MockRepositorySettingsPage(new MockRepositoryUi());
		page.setNeedsEncoding(true);

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		page.createControl(shell);
		page.setVisible(true);

		TaskRepository repository2 = page.createTaskRepository();
		assertEquals("UTF-8", repository2.getCharacterEncoding());
	}

	public void testNeedsEncodingFalse() {
		MockRepositorySettingsPage page = new MockRepositorySettingsPage(new MockRepositoryUi());
		page.setNeedsEncoding(false);

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		page.createControl(shell);
		page.setVisible(true);

		page.createTaskRepository();
	}

	public void testNeedsAnonyoumousLoginFalse() {
		MockRepositorySettingsPage page = new MockRepositorySettingsPage(new MockRepositoryUi());
		page.setNeedsAnonymousLogin(false);

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		page.createControl(shell);
		page.setVisible(true);

		assertNull(page.getAnonymousButton());
	}

	public void testNeedsAnonyoumousLoginNoRepository() {
		MockRepositorySettingsPage page = new MockRepositorySettingsPage(new MockRepositoryUi());
		page.setNeedsAnonymousLogin(true);

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		page.createControl(shell);
		page.setVisible(true);

		assertNotNull(page.getAnonymousButton());

		assertFalse(page.getAnonymousButton().getSelection());
		assertTrue(page.getUserNameEditor().getTextControl(page.getParent()).isEnabled());
		assertTrue(page.getPasswordEditor().getTextControl(page.getParent()).isEnabled());
		assertEquals("", page.getUserName());
		assertEquals("", page.getPassword());
	}

	public void testNeedsAnonyoumousLogin() {
		TaskRepository repository = new TaskRepository("kind", "http://localhost/");
		MockRepositorySettingsPage page = new MockRepositorySettingsPage(new MockRepositoryUi());
		page.setNeedsAnonymousLogin(true);
		page.setRepository(repository);

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		page.createControl(shell);
		page.setVisible(true);

		assertNotNull(page.getAnonymousButton());

		assertTrue(page.getAnonymousButton().getSelection());
		assertFalse(page.getUserNameEditor().getTextControl(page.getParent()).isEnabled());
		assertFalse(page.getPasswordEditor().getTextControl(page.getParent()).isEnabled());
		assertEquals("", page.getUserName());
		assertEquals("", page.getPassword());

		page.getAnonymousButton().setSelection(false);
		page.getAnonymousButton().notifyListeners(SWT.Selection, new Event());
		assertTrue(page.getUserNameEditor().getTextControl(page.getParent()).isEnabled());
		assertTrue(page.getPasswordEditor().getTextControl(page.getParent()).isEnabled());
		page.getUserNameEditor().setStringValue("user");
		page.getPasswordEditor().setStringValue("password");
		assertEquals("user", page.getUserName());
		assertEquals("password", page.getPassword());

		page.getAnonymousButton().setSelection(true);
		page.getAnonymousButton().notifyListeners(SWT.Selection, new Event());
		assertFalse(page.getUserNameEditor().getTextControl(page.getParent()).isEnabled());
		assertFalse(page.getPasswordEditor().getTextControl(page.getParent()).isEnabled());
		assertEquals("", page.getUserName());
		assertEquals("", page.getPassword());

		page.getAnonymousButton().setSelection(false);
		page.getAnonymousButton().notifyListeners(SWT.Selection, new Event());
		assertTrue(page.getUserNameEditor().getTextControl(page.getParent()).isEnabled());
		assertTrue(page.getPasswordEditor().getTextControl(page.getParent()).isEnabled());
		assertEquals("user", page.getUserNameEditor().getStringValue());
		assertEquals("password", page.getPasswordEditor().getStringValue());
		assertEquals("user", page.getUserName());
		assertEquals("password", page.getPassword());
	}

	private class MockRepositorySettingsPage extends AbstractRepositorySettingsPage {

		private Composite parent;

		public MockRepositorySettingsPage(AbstractRepositoryConnectorUi repositoryUi) {
			super("title", "description", repositoryUi);
		}

		@Override
		protected void createAdditionalControls(Composite parent) {
			this.parent = parent;
		}

		@Override
		protected boolean isValidUrl(String name) {
			// ignore
			return false;
		}

		@Override
		protected void validateSettings() {
			// ignore
		}

		Button getAnonymousButton() {
			return anonymousButton;
		}

		StringFieldEditor getUserNameEditor() {
			return userNameEditor;
		}

		StringFieldEditor getPasswordEditor() {
			return repositoryPasswordEditor;
		}

		Composite getParent() {
			return parent;
		}
	}

}
