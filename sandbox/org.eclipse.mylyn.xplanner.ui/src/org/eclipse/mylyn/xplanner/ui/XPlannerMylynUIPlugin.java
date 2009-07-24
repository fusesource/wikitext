/*******************************************************************************
 * Copyright (c) 2007 - 2007 CodeGear and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.xplanner.ui;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Ravi Kumar 
 * @author Helen Bershadskaya 
 */
public class XPlannerMylynUIPlugin extends AbstractUIPlugin {

	private static XPlannerMylynUIPlugin INSTANCE;

	public static final String PLUGIN_ID = "org.eclipse.mylyn.xplanner.ui"; //$NON-NLS-1$

	public final static String REPOSITORY_KIND = "xplanner"; //$NON-NLS-1$

	public final static String XPLANNER_CLIENT_LABEL = Messages.MylynXPlannerPlugin_CLIENT_LABEL;

	public final static String TITLE_MESSAGE_DIALOG = Messages.MylynXPlannerPlugin_CLIENT_DIALOG_TITLE;

	public static final String DELIM_URL_PREFIX = "/do/view/"; //$NON-NLS-1$

	public final static String DELIM_URL_SUFFIX = "?oid="; //$NON-NLS-1$

	public final static String TASK_URL_PREFIX = DELIM_URL_PREFIX + "task" + DELIM_URL_SUFFIX; //$NON-NLS-1$

	public final static String USER_STORY_URL_PREFIX = DELIM_URL_PREFIX + "userstory" + DELIM_URL_SUFFIX; //$NON-NLS-1$

	public final static String ITERATION_URL_PREFIX = DELIM_URL_PREFIX + "iteration" + DELIM_URL_SUFFIX; //$NON-NLS-1$

	public final static IStatus NO_LICENSE_STATUS = new Status(IStatus.INFO, XPlannerMylynUIPlugin.PLUGIN_ID,
			0, Messages.MylynXPlannerPlugin_NOT_AVAILABLE_IN_SKU, null);

	public XPlannerMylynUIPlugin() {
		INSTANCE = this;
	}

	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		INSTANCE = null;
		XPlannerClientFacade.getDefault().logOutFromAll();
	}

	public static void log(final Throwable e, final String message, boolean informUser) {
		if (Platform.isRunning() && informUser) {
			try {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						Shell shell = null;
						if (PlatformUI.getWorkbench() != null
								&& PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
							shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
						}
						String displayMessage = message == null ? e.getMessage() : message + "\n" + e.getMessage(); //$NON-NLS-1$
						MessageDialog.openError(shell, Messages.MylynXPlannerPlugin_XPLANNER_ERROR_TITLE, displayMessage); 
					}
				});
			} 
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
		else {
			StatusHandler.log(e, message == null ? Messages.MylynXPlannerPlugin_XPLANNER_ERROR_TITLE : message);
		}
	}

	public static XPlannerMylynUIPlugin getDefault() {
		return INSTANCE;
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
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}