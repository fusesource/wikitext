/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.ui.editors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * A form page that contains a browser control.
 * 
 * @since 3.0
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class BrowserFormPage extends FormPage {

	public static final String ID_EDITOR = "org.eclipse.mylyn.tasks.ui.editor.browser";

	private Browser browser;

	public BrowserFormPage(FormEditor editor, String title) {
		super(editor, ID_EDITOR, title);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		try {
			ScrolledForm form = managedForm.getForm();
			form.getBody().setLayout(new FillLayout());
			browser = new Browser(form.getBody(), SWT.NONE);
			managedForm.getForm().setContent(browser);
			String url = getUrl();
			if (url != null) {
				browser.setUrl(url);
			}
		} catch (SWTError e) {
			// TODO review error handling
			StatusHandler.fail(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Could not create browser page: "
					+ e.getMessage(), e));
		} catch (RuntimeException e) {
			// TODO review error handling
			StatusHandler.log(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Could not create browser page", e));
		}
	}

	/**
	 * Returns a reference to the browser control.
	 */
	public Browser getBrowser() {
		return browser;
	}

	/**
	 * Returns the initial URL that is displayed in the browser control. The default implementation tries to determine
	 * the URL from the editor input.
	 * <p>
	 * Subclasses should override this method to display a specific URL.
	 * 
	 * @return the URL to load when the page is created; null, if no URL should be loaded
	 */
	protected String getUrl() {
		IEditorInput input = getEditorInput();
		if (input instanceof TaskEditorInput) {
			return ((TaskEditorInput) input).getTask().getUrl();
		}
		return null;
	}

}
