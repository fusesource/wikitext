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
package org.eclipse.mylyn.internal.tasks.ui.util;

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
import org.eclipse.mylyn.internal.monitor.core.util.StatusManager;
import org.eclipse.mylyn.internal.tasks.ui.IDynamicSubMenuContributor;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractTaskListFactory;
import org.eclipse.mylyn.tasks.core.RepositoryTemplate;
import org.eclipse.mylyn.tasks.ui.AbstractDuplicateDetector;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.AbstractTaskRepositoryLinkProvider;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorFactory;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * @author Mik Kersten
 * @author Shawn Minto
 * @author Rob Elves
 */
public class TasksUiExtensionReader {

	public static final String EXTENSION_REPOSITORIES = "org.eclipse.mylyn.tasks.ui.repositories";

	public static final String EXTENSION_REPOSITORY_LINKS_PROVIDERS = "org.eclipse.mylyn.tasks.ui.projectLinkProviders";

	public static final String EXTENSION_TEMPLATES = "org.eclipse.mylyn.tasks.core.templates";

	public static final String EXTENSION_TMPL_REPOSITORY = "repository";

	public static final String ELMNT_TMPL_LABEL = "label";

	public static final String ELMNT_TMPL_URLREPOSITORY = "urlRepository";

	public static final String ELMNT_TMPL_REPOSITORYKIND = "repositoryKind";

	public static final String ELMNT_TMPL_CHARACTERENCODING = "characterEncoding";

	public static final String ELMNT_TMPL_ANONYMOUS = "anonymous";

	public static final String ELMNT_TMPL_VERSION = "version";

	public static final String ELMNT_TMPL_URLNEWTASK = "urlNewTask";

	public static final String ELMNT_TMPL_URLTASK = "urlTask";

	public static final String ELMNT_TMPL_URLTASKQUERY = "urlTaskQuery";

	public static final String ELMNT_TMPL_NEWACCOUNTURL = "urlNewAccount";

	public static final String ELMNT_TMPL_ADDAUTO = "addAutomatically";

	public static final String ELMNT_REPOSITORY_CONNECTOR = "connectorCore";

	public static final String ATTR_USER_MANAGED = "userManaged";

	public static final String ATTR_CUSTOM_NOTIFICATIONS = "customNotifications";

	public static final String ELMNT_REPOSITORY_LINK_PROVIDER = "linkProvider";

	public static final String ELMNT_REPOSITORY_UI = "connectorUi";

	public static final String ELMNT_EXTERNALIZER = "externalizer";

	public static final String ATTR_BRANDING_ICON = "brandingIcon";

	public static final String ATTR_OVERLAY_ICON = "overlayIcon";

	public static final String ELMNT_TYPE = "type";

	public static final String ELMNT_QUERY_PAGE = "queryPage";

	public static final String ELMNT_SETTINGS_PAGE = "settingsPage";

	public static final String EXTENSION_TASK_CONTRIBUTOR = "org.eclipse.mylyn.tasks.ui.actions";

	public static final String ATTR_ACTION_CONTRIBUTOR_CLASS = "taskHandlerClass";

	public static final String DYNAMIC_POPUP_ELEMENT = "dynamicPopupMenu";

	public static final String ATTR_CLASS = "class";

	public static final String ATTR_MENU_PATH = "menuPath";

	public static final String EXTENSION_EDITORS = "org.eclipse.mylyn.tasks.ui.editors";

	public static final String ELMNT_EDITOR_FACTORY = "editorFactory";

	public static final String ELMNT_HYPERLINK_LISTENER = "hyperlinkListener";

	public static final String ELMNT_HYPERLINK_DETECTOR = "hyperlinkDetector";

	public static final String EXTENSION_DUPLICATE_DETECTORS = "org.eclipse.mylyn.tasks.ui.duplicateDetectors";

	public static final String ELMNT_DUPLICATE_DETECTOR = "detector";

	public static final String ATTR_NAME = "name";

	public static final String ATTR_KIND = "kind";

	private static boolean coreExtensionsRead = false;

	public static void initStartupExtensions(TaskListWriter delegatingExternalizer) {
		List<AbstractTaskListFactory> externalizers = new ArrayList<AbstractTaskListFactory>();
		if (!coreExtensionsRead) {
			IExtensionRegistry registry = Platform.getExtensionRegistry();

			// HACK: has to be read first
			IExtensionPoint repositoriesExtensionPoint = registry.getExtensionPoint(EXTENSION_REPOSITORIES);
			IExtension[] repositoryExtensions = repositoriesExtensionPoint.getExtensions();
			for (int i = 0; i < repositoryExtensions.length; i++) {
				IConfigurationElement[] elements = repositoryExtensions[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					if (elements[j].getName().equals(ELMNT_REPOSITORY_CONNECTOR)) {
						readRepositoryConnectorCore(elements[j]);
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
			delegatingExternalizer.setDelegateExternalizers(externalizers);
			coreExtensionsRead = true;
		}
	}

	public static void initWorkbenchUiExtensions() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();

		IExtensionPoint repositoriesExtensionPoint = registry.getExtensionPoint(EXTENSION_REPOSITORIES);
		IExtension[] repositoryExtensions = repositoriesExtensionPoint.getExtensions();
		for (int i = 0; i < repositoryExtensions.length; i++) {
			IConfigurationElement[] elements = repositoryExtensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals(ELMNT_REPOSITORY_UI)) {
					readRepositoryConnectorUi(elements[j]);
				}
			}
		}

		IExtensionPoint linkProvidersExtensionPoint = registry.getExtensionPoint(EXTENSION_REPOSITORY_LINKS_PROVIDERS);
		IExtension[] linkProvidersExtensions = linkProvidersExtensionPoint.getExtensions();
		for (int i = 0; i < linkProvidersExtensions.length; i++) {
			IConfigurationElement[] elements = linkProvidersExtensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals(ELMNT_REPOSITORY_LINK_PROVIDER)) {
					readLinkProvider(elements[j]);
				}
			}
		}

		IExtensionPoint duplicateDetectorsExtensionPoint = registry.getExtensionPoint(EXTENSION_DUPLICATE_DETECTORS);
		IExtension[] dulicateDetectorsExtensions = duplicateDetectorsExtensionPoint.getExtensions();
		for (int i = 0; i < dulicateDetectorsExtensions.length; i++) {
			IConfigurationElement[] elements = dulicateDetectorsExtensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals(ELMNT_DUPLICATE_DETECTOR)) {
					readDuplicateDetector(elements[j]);
				}
			}
		}

	}

	private static void readDuplicateDetector(IConfigurationElement element) {
		try {
			Object obj = element.createExecutableExtension(ATTR_CLASS);
			if (obj instanceof AbstractDuplicateDetector) {
				AbstractDuplicateDetector duplicateDetector = (AbstractDuplicateDetector) obj;
				duplicateDetector.setName(element.getAttribute(ATTR_NAME));
				duplicateDetector.setKind(element.getAttribute(ATTR_KIND));
				TasksUiPlugin.getDefault().addDuplicateDetector((AbstractDuplicateDetector) duplicateDetector);
			} else {
				StatusManager.log("Could not load duplicate detector: " + obj.getClass().getCanonicalName(), null);
			}
		} catch (CoreException e) {
			StatusManager.log(e, "Could not load duplicate detector extension");
		}
	}

	private static void readLinkProvider(IConfigurationElement element) {
		try {
			Object repositoryLinkProvider = element.createExecutableExtension(ATTR_CLASS);
			if (repositoryLinkProvider instanceof AbstractTaskRepositoryLinkProvider) {
				TasksUiPlugin.getDefault().addRepositoryLinkProvider(
						(AbstractTaskRepositoryLinkProvider) repositoryLinkProvider);
			} else {
				StatusManager.log("Could not load repository link provider: "
						+ repositoryLinkProvider.getClass().getCanonicalName(), null);
			}
		} catch (CoreException e) {
			StatusManager.log(e, "Could not load repository link provider extension");
		}
	}

	private static void readHyperlinkDetector(IConfigurationElement element) {
		try {
			Object hyperlinkDetector = element.createExecutableExtension(ATTR_CLASS);
			if (hyperlinkDetector instanceof IHyperlinkDetector) {
				TasksUiPlugin.getDefault().addTaskHyperlinkDetector((IHyperlinkDetector) hyperlinkDetector);
			} else {
				StatusManager.log("Could not load detector: " + hyperlinkDetector.getClass().getCanonicalName(),
						null);
			}
		} catch (CoreException e) {
			StatusManager.log(e, "Could not load tasklist hyperlink detector extension");
		}
	}

	private static void readEditorFactory(IConfigurationElement element) {
		try {
			Object editor = element.createExecutableExtension(ATTR_CLASS);
			if (editor instanceof AbstractTaskEditorFactory) {
				TasksUiPlugin.getDefault().addContextEditor((AbstractTaskEditorFactory) editor);
			} else {
				StatusManager.log("Could not load editor: " + editor.getClass().getCanonicalName()
						+ " must implement " + AbstractTaskEditorFactory.class.getCanonicalName(), null);
			}
		} catch (CoreException e) {
			StatusManager.log(e, "Could not load tasklist listener extension");
		}
	}

	private static void readRepositoryConnectorCore(IConfigurationElement element) {
		try {
			Object type = element.getAttribute(ELMNT_TYPE);
			Object connectorCore = element.createExecutableExtension(ATTR_CLASS);
			if (connectorCore instanceof AbstractRepositoryConnector && type != null) {
				AbstractRepositoryConnector repositoryConnector = (AbstractRepositoryConnector) connectorCore;
				TasksUiPlugin.getRepositoryManager().addRepositoryConnector(repositoryConnector);

				String userManagedString = element.getAttribute(ATTR_USER_MANAGED);
				if (userManagedString != null) {
					boolean userManaged = Boolean.parseBoolean(userManagedString);
					repositoryConnector.setUserManaged(userManaged);
				}
			} else {
				StatusManager.log("could not not load connector core: " + connectorCore, null);
			}

		} catch (CoreException e) {
			StatusManager.log(e, "Could not load tasklist listener extension");
		}
	}

	private static void readRepositoryConnectorUi(IConfigurationElement element) {
		try {
			Object connectorUiObject = element.createExecutableExtension(ATTR_CLASS);
			if (connectorUiObject instanceof AbstractRepositoryConnectorUi) {
				AbstractRepositoryConnectorUi connectorUi = (AbstractRepositoryConnectorUi) connectorUiObject;
				TasksUiPlugin.addRepositoryConnectorUi((AbstractRepositoryConnectorUi) connectorUi);

				String customNotificationsString = element.getAttribute(ATTR_CUSTOM_NOTIFICATIONS);
				if (customNotificationsString != null) {
					boolean customNotifications = Boolean.parseBoolean(customNotificationsString);
					connectorUi.setCustomNotificationHandling(customNotifications);
				}

				String iconPath = element.getAttribute(ATTR_BRANDING_ICON);
				if (iconPath != null) {
					ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(element.getContributor()
							.getName(), iconPath);
					if (descriptor != null) {
						TasksUiPlugin.getDefault().addBrandingIcon(
								((AbstractRepositoryConnectorUi) connectorUi).getRepositoryType(),
								TasksUiImages.getImage(descriptor));
					}
				}
				String overlayIconPath = element.getAttribute(ATTR_OVERLAY_ICON);
				if (overlayIconPath != null) {
					ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(element.getContributor()
							.getName(), overlayIconPath);
					if (descriptor != null) {
						TasksUiPlugin.getDefault().addOverlayIcon(
								((AbstractRepositoryConnectorUi) connectorUi).getRepositoryType(), descriptor);
					}
				}
			} else {
				StatusManager.log("could not not load connector ui: " + connectorUiObject, null);
			}

		} catch (CoreException e) {
			StatusManager.log(e, "Could not load tasklist listener extension");
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
		String encoding = element.getAttribute(ELMNT_TMPL_CHARACTERENCODING);
		addAuto = Boolean.parseBoolean(element.getAttribute(ELMNT_TMPL_ADDAUTO));
		anonymous = Boolean.parseBoolean(element.getAttribute(ELMNT_TMPL_ANONYMOUS));

		if (serverUrl != null && label != null && repKind != null
				&& TasksUiPlugin.getRepositoryManager().getRepositoryConnector(repKind) != null) {
			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager()
					.getRepositoryConnector(repKind);
			RepositoryTemplate template = new RepositoryTemplate(label, serverUrl, encoding, version, newTaskUrl,
					taskPrefix, taskQueryUrl, newAccountUrl, anonymous, addAuto);
			connector.addTemplate(template);

			for (IConfigurationElement configElement : element.getChildren()) {
				String name = configElement.getAttribute("name");
				String value = configElement.getAttribute("value");
				if (name != null && !name.equals("") && value != null) {
					template.addAttribute(name, value);
				}
			}

		} else {
			StatusManager.log("Could not load repository template extension " + element.getName(),
					TasksUiExtensionReader.class);
		}
	}

	private static void readDynamicPopupContributor(IConfigurationElement element) {
		try {
			Object dynamicPopupContributor = element.createExecutableExtension(ATTR_CLASS);
			String menuPath = element.getAttribute(ATTR_MENU_PATH);
			if (dynamicPopupContributor instanceof IDynamicSubMenuContributor) {
				TasksUiPlugin.getDefault().addDynamicPopupContributor(menuPath,
						(IDynamicSubMenuContributor) dynamicPopupContributor);
			} else {
				StatusManager.log("Could not load dynamic popup menu: "
						+ dynamicPopupContributor.getClass().getCanonicalName() + " must implement "
						+ IDynamicSubMenuContributor.class.getCanonicalName(), null);
			}
		} catch (CoreException e) {
			StatusManager.log(e, "Could not load dynamic popup extension");
		}
	}

	private static void readExternalizer(IConfigurationElement element, List<AbstractTaskListFactory> externalizers) {
		try {
			Object externalizerObject = element.createExecutableExtension(ATTR_CLASS);
			if (externalizerObject instanceof AbstractTaskListFactory) {
				AbstractTaskListFactory externalizer = (AbstractTaskListFactory) externalizerObject;
				externalizers.add(externalizer);
			} else {
				StatusManager.log("Could not load externalizer: "
						+ externalizerObject.getClass().getCanonicalName() + " must implement "
						+ AbstractTaskListFactory.class.getCanonicalName(), null);
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
			StatusManager.log(e, "Could not load task handler extension");
		}
	}
}
