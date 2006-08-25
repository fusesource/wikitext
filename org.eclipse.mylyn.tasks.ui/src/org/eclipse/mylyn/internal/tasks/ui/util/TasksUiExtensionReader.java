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
package org.eclipse.mylar.internal.tasks.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.mylar.context.core.MylarStatusHandler;
import org.eclipse.mylar.internal.tasks.ui.IDynamicSubMenuContributor;
import org.eclipse.mylar.internal.tasks.ui.ITaskEditorFactory;
import org.eclipse.mylar.internal.tasks.ui.TaskListImages;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.ITaskListExternalizer;
import org.eclipse.mylar.tasks.core.RepositoryTemplate;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryUi;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * @author Mik Kersten
 * @author Shawn Minto
 * @author Rob Elves
 */
public class TasksUiExtensionReader {

	public static final String EXTENSION_REPOSITORIES = "org.eclipse.mylar.tasks.ui.repositories";

	public static final String EXTENSION_TEMPLATES = "org.eclipse.mylar.tasks.ui.templates";

	public static final String EXTENSION_TMPL_REPOSITORY = "repository";

	public static final String ELMNT_TMPL_LABEL = "label";

	public static final String ELMNT_TMPL_URLREPOSITORY = "urlRepository";

	public static final String ELMNT_TMPL_REPOSITORYKIND = "repositoryKind";

	public static final String ELMNT_TMPL_ANONYMOUS = "anonymous";

	public static final String ELMNT_TMPL_VERSION = "version";

	public static final String ELMNT_TMPL_URLNEWTASK = "urlNewTask";

	public static final String ELMNT_TMPL_URLTASK = "urlTask";

	public static final String ELMNT_TMPL_URLTASKQUERY = "urlTaskQuery";

	public static final String ELMNT_TMPL_NEWACCOUNTURL = "urlNewAccount";

	public static final String ELMNT_TMPL_ADDAUTO = "addAutomatically";

	public static final String ELMNT_REPOSITORY_CONNECTOR = "repositoryConnector";

	public static final String ELMNT_REPOSITORY_UI= "repositoryUi";
	
	public static final String ELMNT_EXTERNALIZER = "externalizer";

	public static final String ATTR_BRANDING_ICON = "brandingIcon";

	public static final String ELMNT_TYPE = "type";

	public static final String ELMNT_QUERY_PAGE = "queryPage";

	public static final String ELMNT_SETTINGS_PAGE = "settingsPage";

	public static final String EXTENSION_TASK_CONTRIBUTOR = "org.eclipse.mylar.tasks.ui.providers";

	public static final String ATTR_ACTION_CONTRIBUTOR_CLASS = "taskHandlerClass";

	public static final String DYNAMIC_POPUP_ELEMENT = "dynamicPopupMenu";

	public static final String ATTR_CLASS = "class";

	public static final String EXTENSION_EDITORS = "org.eclipse.mylar.tasks.ui.editors";

	public static final String ELMNT_EDITOR_FACTORY = "editorFactory";

	public static final String ELMNT_HYPERLINK_LISTENER = "hyperlinkListener";

	public static final String ELMNT_HYPERLINK_DETECTOR = "hyperlinkDetector";

	private static boolean extensionsRead = false;

	public static void initExtensions(TaskListWriter writer) {
		List<ITaskListExternalizer> externalizers = new ArrayList<ITaskListExternalizer>();
		if (!extensionsRead) {
			IExtensionRegistry registry = Platform.getExtensionRegistry();

			// HACK: has to be read first
			IExtensionPoint repositoriesExtensionPoint = registry.getExtensionPoint(EXTENSION_REPOSITORIES);
			IExtension[] repositoryExtensions = repositoriesExtensionPoint.getExtensions();
			for (int i = 0; i < repositoryExtensions.length; i++) {
				IConfigurationElement[] elements = repositoryExtensions[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					if (elements[j].getName().equals(ELMNT_REPOSITORY_CONNECTOR)) {
						readRepositoryConnector(elements[j]);
					} else if (elements[j].getName().equals(ELMNT_REPOSITORY_UI)) {
						readRepositoryUi(elements[j]);
					} else if (elements[j].getName().equals(ELMNT_EXTERNALIZER)) {
						readExternalizer(elements[j], externalizers);
					}
				}
			}

			IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTENSION_TASK_CONTRIBUTOR);
			IExtension[] extensions = extensionPoint.getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				IConfigurationElement[] elements = extensions[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					if (elements[j].getName().equals(DYNAMIC_POPUP_ELEMENT)) {
						readDynamicPopupContributor(elements[j]);
					}
				}
			}

			IExtensionPoint templatesExtensionPoint = registry.getExtensionPoint(EXTENSION_TEMPLATES);
			IExtension[] templateExtensions = templatesExtensionPoint.getExtensions();
			for (int i = 0; i < templateExtensions.length; i++) {
				IConfigurationElement[] elements = templateExtensions[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					if (elements[j].getName().equals(EXTENSION_TMPL_REPOSITORY)) {
						readRepositoryTemplate(elements[j]);
					}
				}
			}

			IExtensionPoint editorsExtensionPoint = registry.getExtensionPoint(EXTENSION_EDITORS);
			IExtension[] editors = editorsExtensionPoint.getExtensions();
			for (int i = 0; i < editors.length; i++) {
				IConfigurationElement[] elements = editors[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					if (elements[j].getName().equals(ELMNT_EDITOR_FACTORY)) {
						readEditorFactory(elements[j]);
					} else if (elements[j].getName().equals(ELMNT_HYPERLINK_DETECTOR)) {
						readHyperlinkDetector(elements[j]);
					}
				}
			}
			writer.setDelegateExternalizers(externalizers);
			extensionsRead = true;
		}
	}

	private static void readHyperlinkDetector(IConfigurationElement element) {
		try {
			Object hyperlinkDetector = element.createExecutableExtension(ATTR_CLASS);
			if (hyperlinkDetector instanceof IHyperlinkDetector) {
				TasksUiPlugin.getDefault().addTaskHyperlinkDetector((IHyperlinkDetector) hyperlinkDetector);
			} else {
				MylarStatusHandler.log("Could not load detector: " + hyperlinkDetector.getClass().getCanonicalName(),
						null);
			}
		} catch (CoreException e) {
			MylarStatusHandler.log(e, "Could not load tasklist hyperlink detector extension");
		}
	}

	private static void readEditorFactory(IConfigurationElement element) {
		try {
			Object editor = element.createExecutableExtension(ATTR_CLASS);
			if (editor instanceof ITaskEditorFactory) {
				TasksUiPlugin.getDefault().addContextEditor((ITaskEditorFactory) editor);
			} else {
				MylarStatusHandler.log("Could not load editor: " + editor.getClass().getCanonicalName()
						+ " must implement " + ITaskEditorFactory.class.getCanonicalName(), null);
			}
		} catch (CoreException e) {
			MylarStatusHandler.log(e, "Could not load tasklist listener extension");
		}
	}

	private static void readRepositoryConnector(IConfigurationElement element) {
		try {
			Object type = element.getAttribute(ELMNT_TYPE);
			Object repository = element.createExecutableExtension(ATTR_CLASS);
			if (repository instanceof AbstractRepositoryConnector && type != null) {
				TasksUiPlugin.getRepositoryManager().addRepositoryConnector((AbstractRepositoryConnector) repository);
			} else {
				MylarStatusHandler.log("could not not load extension: " + repository, null);
			}

		} catch (CoreException e) {
			MylarStatusHandler.log(e, "Could not load tasklist listener extension");
		}
	}

	private static void readRepositoryUi(IConfigurationElement element) {
		try {
			Object type = element.getAttribute(ELMNT_TYPE);
			Object repository = element.createExecutableExtension(ATTR_CLASS);
			if (repository instanceof AbstractRepositoryConnector && type != null) {
				TasksUiPlugin.getRepositoryManager().addRepositoryUi((AbstractRepositoryUi) repository);

				String iconPath = element.getAttribute(ATTR_BRANDING_ICON);
				if (iconPath != null) {
					ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(element.getContributor()
							.getName(), iconPath);
					if (descriptor != null) {
						TasksUiPlugin.getDefault().getBrandingIcons().put((AbstractRepositoryConnector) repository,
								TaskListImages.getImage(descriptor));
					}
				}
			} else {
				MylarStatusHandler.log("could not not load extension: " + repository, null);
			}

		} catch (CoreException e) {
			MylarStatusHandler.log(e, "Could not load tasklist listener extension");
		}
	}
	
	private static void readRepositoryTemplate(IConfigurationElement element) {

		boolean anonymous = false;
		boolean addAuto = false;

		String label = element.getAttribute(ELMNT_TMPL_LABEL);
		String serverUrl = element.getAttribute(ELMNT_TMPL_URLREPOSITORY);
		String repKind = element.getAttribute(ELMNT_TMPL_REPOSITORYKIND);
		String version = element.getAttribute(ELMNT_TMPL_VERSION);
		String newTaskUrl = element.getAttribute(ELMNT_TMPL_URLNEWTASK);
		String taskPrefix = element.getAttribute(ELMNT_TMPL_URLTASK);
		String taskQueryUrl = element.getAttribute(ELMNT_TMPL_URLTASKQUERY);
		String newAccountUrl = element.getAttribute(ELMNT_TMPL_NEWACCOUNTURL);
		addAuto = Boolean.parseBoolean(element.getAttribute(ELMNT_TMPL_ADDAUTO));
		anonymous = Boolean.parseBoolean(element.getAttribute(ELMNT_TMPL_ANONYMOUS));

		if (serverUrl != null && label != null && repKind != null
				&& TasksUiPlugin.getRepositoryManager().getRepositoryConnector(repKind) != null) {
			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager()
					.getRepositoryConnector(repKind);
			RepositoryTemplate template = new RepositoryTemplate(label, serverUrl, version, newTaskUrl, taskPrefix,
					taskQueryUrl, newAccountUrl, anonymous, addAuto);
			connector.addTemplate(template);
			
			for (IConfigurationElement configElement : element.getChildren()) {
				String name = configElement.getAttribute("name");
				String value = configElement.getAttribute("value");
				if(name != null && !name.equals("") && value != null) {
					template.addAttribute(name, value);
				}
			}
			
		} else {
			MylarStatusHandler.log("Could not load repository template extension " + element.getName(),
					TasksUiExtensionReader.class);
		}
	}

	private static void readDynamicPopupContributor(IConfigurationElement element) {
		try {
			Object dynamicPopupContributor = element.createExecutableExtension(ATTR_CLASS);
			if (dynamicPopupContributor instanceof IDynamicSubMenuContributor) {
				TasksUiPlugin.getDefault().addDynamicPopupContributor(
						(IDynamicSubMenuContributor) dynamicPopupContributor);
			} else {
				MylarStatusHandler.log("Could not load dyanmic popup menu: "
						+ dynamicPopupContributor.getClass().getCanonicalName() + " must implement "
						+ IDynamicSubMenuContributor.class.getCanonicalName(), null);
			}
		} catch (CoreException e) {
			MylarStatusHandler.log(e, "Could not load dynamic popup extension");
		}
	}

	private static void readExternalizer(IConfigurationElement element, List<ITaskListExternalizer> externalizers) {
		try {
			Object externalizerObject = element.createExecutableExtension(ATTR_CLASS);
			if (externalizerObject instanceof ITaskListExternalizer) {
				ITaskListExternalizer externalizer = (ITaskListExternalizer) externalizerObject;
				externalizers.add((ITaskListExternalizer) externalizer);
			} else {
				MylarStatusHandler.log("Could not load externalizer: "
						+ externalizerObject.getClass().getCanonicalName() + " must implement "
						+ ITaskListExternalizer.class.getCanonicalName(), null);
			}

			// Object taskHandler =
			// element.createExecutableExtension(ATTR_ACTION_CONTRIBUTOR_CLASS);
			// if (taskHandler instanceof ITaskHandler) {
			// MylarTaskListPlugin.getDefault().addTaskHandler((ITaskHandler)
			// taskHandler);
			// } else {
			// MylarStatusHandler.log("Could not load contributor: " +
			// taskHandler.getClass().getCanonicalName()
			// + " must implement " + ITaskHandler.class.getCanonicalName(),
			// null);
			// }
		} catch (CoreException e) {
			MylarStatusHandler.log(e, "Could not load task handler extension");
		}
	}
}
