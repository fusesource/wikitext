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
package org.eclipse.mylyn.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.mylyn.context.core.AbstractContextStructureBridge;
import org.eclipse.mylyn.context.core.ContextCorePlugin;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.context.core.IInteractionElement;
import org.eclipse.mylyn.internal.monitor.core.util.StatusManager;
import org.eclipse.mylyn.internal.resources.ui.ContextEditorManager;
import org.eclipse.mylyn.internal.resources.ui.EditorInteractionMonitor;
import org.eclipse.mylyn.internal.resources.ui.ResourceChangeMonitor;
import org.eclipse.mylyn.internal.resources.ui.ResourceInteractionMonitor;
import org.eclipse.mylyn.internal.resources.ui.ResourceInterestUpdater;
import org.eclipse.mylyn.monitor.ui.MonitorUiPlugin;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Main entry point for the Resource Structure Bridge.
 * 
 * @author Mik Kersten
 * @since	2.0
 */
public class ResourcesUiBridgePlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.mylyn.resources.ui";
	
	private static ResourcesUiBridgePlugin INSTANCE;

	private ResourceChangeMonitor resourceChangeMonitor = new ResourceChangeMonitor();
	
	private ContextEditorManager editorManager = new ContextEditorManager();

	private ResourceInteractionMonitor resourceInteractionMonitor;

	private EditorInteractionMonitor interestEditorTracker = new EditorInteractionMonitor();

	private ResourceInterestUpdater interestUpdater = new ResourceInterestUpdater();
	
	private ResourceBundle resourceBundle;
	
	private static final String PREF_STORE_DELIM = ", ";

	public static final String PREF_RESOURCES_IGNORED = "org.eclipse.mylyn.ide.resources.ignored.pattern";

	public static final String PREF_VAL_DEFAULT_RESOURCES_IGNORED = ".*" + PREF_STORE_DELIM;
	
	public ResourcesUiBridgePlugin() {
		super();
		INSTANCE = this;
		resourceInteractionMonitor = new ResourceInteractionMonitor();
	}

	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		initPreferenceDefaults();

		ContextCorePlugin.getContextManager().addListener(editorManager);
		MonitorUiPlugin.getDefault().getSelectionMonitors().add(resourceInteractionMonitor);

		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeMonitor,
				IResourceChangeEvent.POST_CHANGE);
		
		interestEditorTracker.install(PlatformUI.getWorkbench());
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			super.stop(context);
			INSTANCE = null;
			resourceBundle = null;
			
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeMonitor);
			ContextCorePlugin.getContextManager().removeListener(editorManager);
			MonitorUiPlugin.getDefault().getSelectionMonitors().remove(resourceInteractionMonitor);
		} catch (Exception e) {
			StatusManager.fail(e, "Mylar XML stop failed", false);
		}
	}
	
	private void initPreferenceDefaults() {
		getPreferenceStore().setDefault(PREF_RESOURCES_IGNORED, PREF_VAL_DEFAULT_RESOURCES_IGNORED);
	}

	public List<IResource> getInterestingResources(IInteractionContext context) {
		List<IResource> interestingResources = new ArrayList<IResource>();
		Collection<IInteractionElement> resourceElements = ContextCorePlugin.getContextManager().getInterestingDocuments(context);
		for (IInteractionElement element : resourceElements) {
			IResource resource = getResourceForElement(element, false);
			if (resource != null) {
				interestingResources.add(resource);
			}
		}
		return interestingResources;
	}

	public void setExcludedResourcePatterns(Set<String> patterns) {
		StringBuilder store = new StringBuilder();
		for (String string : patterns) {
			store.append(string);
			store.append(PREF_STORE_DELIM);
		}
		getPreferenceStore().setValue(PREF_RESOURCES_IGNORED, store.toString());
	}

	public Set<String> getExcludedResourcePatterns() {
		Set<String> ignored = new HashSet<String>();
		String read = getPreferenceStore().getString(PREF_RESOURCES_IGNORED);
		if (read != null) {
 			StringTokenizer st = new StringTokenizer(read, PREF_STORE_DELIM);
			while (st.hasMoreTokens()) {
				ignored.add(st.nextToken());
			}
		}
		return ignored;
	}

	public static ResourceInterestUpdater getInterestUpdater() {
		return INSTANCE.interestUpdater;
	}
	
	public IResource getResourceForElement(IInteractionElement element, boolean findContainingResource) {
		if (element == null)
			return null;
		AbstractContextStructureBridge bridge = ContextCorePlugin.getDefault().getStructureBridge(element.getContentType());
		Object object = bridge.getObjectForHandle(element.getHandleIdentifier());
		if (object instanceof IResource) {
			return (IResource) object;
		} else if (object instanceof IAdaptable) {
			Object adapted = ((IAdaptable) object).getAdapter(IResource.class);
			if (adapted instanceof IResource) {
				return (IResource) adapted;
			}
		}
		if (findContainingResource) { // recurse if not found
			String parentHandle = bridge.getParentHandle(element.getHandleIdentifier());
			if (element.getHandleIdentifier().equals(parentHandle)) {
				return null;
			} else {
				return getResourceForElement(ContextCorePlugin.getContextManager().getElement(parentHandle), true);
			}
		} else {
			return null;
		}
	}
	
	public void setResourceMonitoringEnabled(boolean enabled) {
		resourceChangeMonitor.setEnabled(enabled);
	}
	
	public static ContextEditorManager getEditorManager() {
		return INSTANCE.editorManager;
	}
	
	public static ResourcesUiBridgePlugin getDefault() {
		return INSTANCE;
	}

	public ResourceBundle getResourceBundle() {
		try {
			if (resourceBundle == null)
				resourceBundle = ResourceBundle.getBundle("org.eclipse.mylyn.xml.XmlPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}
}
