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
package org.eclipse.mylar.internal.hypertext;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylar.internal.core.util.MylarStatusHandler;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Mik Kersten
 */
public class MylarHypertextPlugin extends AbstractUIPlugin {

	protected static final String ID = "org.eclipse.mylar.hypertext";

	private static MylarHypertextPlugin plugin;

	private WebResourceManager webResourceManager;
	
	private BrowserTracker browserTracker;

	public MylarHypertextPlugin() {
		plugin = this;
	}

	public static WebResourceManager getWebResourceManager() {
		return plugin.webResourceManager;
	}
	
	public void start(BundleContext context) throws Exception {
		super.start(context);
		webResourceManager = new WebResourceManager();
		final IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().asyncExec(new Runnable() {
			public void run() {
				try {
					browserTracker = new BrowserTracker();
					workbench.addWindowListener(browserTracker);
					IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
					for (int i = 0; i < windows.length; i++) {
						windows[i].addPageListener(browserTracker);
						IWorkbenchPage[] pages = windows[i].getPages();
						for (int j = 0; j < pages.length; j++) {
							pages[j].addPartListener(browserTracker);
						}
					}
				} catch (Exception e) {
					MylarStatusHandler.fail(e, "Mylar Hypertext initialization failed", false);
				}
			}
		});
	}

	public void stop(BundleContext context) throws Exception {
		webResourceManager.dispose();
		super.stop(context);
		plugin = null;
	}

	public static MylarHypertextPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path.
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("org.eclipse.mylar.internal.hypertext", path);
	}

}
