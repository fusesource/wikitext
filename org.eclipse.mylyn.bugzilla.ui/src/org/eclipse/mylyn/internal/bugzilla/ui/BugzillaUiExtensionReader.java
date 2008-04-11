/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.ui;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaLanguageSettings;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.monitor.core.StatusHandler;

/**
 * @author Frank Becker
 */
public class BugzillaUiExtensionReader {

	public static final String EXTENSION_LANGUAGES = "org.eclipse.mylyn.bugzilla.core.languages";

	public static final String EXTENSION_TMPL_LANGUAGE = "language";

	public static final String ATTR_LANG_VALUE = "value";

	public static final String ELMNT_LANG_NAME = "name";

	public static final String ELMNT_LANG_ERROR_LOGIN = "error_login";

	public static final String ELMNT_LANG_ERROR_COLLISION = "error_collision";

	public static final String ELMNT_LANG_ERROR_COMMENT_REQIRED = "error_comment_required";

	public static final String ELMNT_LANG_ERROR_LOGGED_OUT = "error_logged_out";

	public static final String ELMNT_LANG_BAD_LOGIN = "bad_login";

	public static final String ELMNT_LANG_PROCESSED = "processed";

	public static final String ELMNT_LANG_CHANGES_SUBMITTED = "changes_submitted";

	private static boolean coreExtensionsRead = false;

	public static void initStartupExtensions() {
		if (!coreExtensionsRead) {
			IExtensionRegistry registry = Platform.getExtensionRegistry();

			IExtensionPoint templatesExtensionPoint = registry.getExtensionPoint(EXTENSION_LANGUAGES);
			IExtension[] templateExtensions = templatesExtensionPoint.getExtensions();
			for (int i = 0; i < templateExtensions.length; i++) {
				IConfigurationElement[] elements = templateExtensions[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					if (elements[j].getName().equals(EXTENSION_TMPL_LANGUAGE)) {
						readLanguageTemplate(elements[j]);
					}
				}
			}

			coreExtensionsRead = true;
		}

	}

	private static void readLanguageTemplate(IConfigurationElement element) {
		String languageName = element.getAttribute(ELMNT_LANG_NAME);

		if (languageName != null) {
			BugzillaRepositoryConnector connector = (BugzillaRepositoryConnector) TasksUiPlugin.getRepositoryManager()
					.getRepositoryConnector(BugzillaCorePlugin.REPOSITORY_KIND);

			BugzillaLanguageSettings bugzillaLanguageSettings = new BugzillaLanguageSettings(languageName);

			for (IConfigurationElement configElement : element.getChildren()) {
				String name = configElement.getName();
				if (name != null && name.equals("languageAttribute")) {
					String command = configElement.getAttribute("command");
					String response = configElement.getAttribute("response");
					bugzillaLanguageSettings.addLanguageAttribute(command, response);
				}
			}
			connector.addLanguageSetting(bugzillaLanguageSettings);
		} else {
			StatusHandler.log(new Status(IStatus.WARNING, BugzillaUiPlugin.PLUGIN_ID, "Could not load language template extension " + element.getName()));
		}
	}
}
