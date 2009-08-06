/*******************************************************************************
 * Copyright (c) 2007, 2009 CodeGear and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.internal.xplanner.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylyn.commons.core.CoreUtil;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Ravi Kumar
 * @author Helen Bershadskaya
 */
public class XPlannerUiPlugin extends AbstractUIPlugin {

	private static XPlannerUiPlugin INSTANCE;

	public static final String ID_PLUGIN = "org.eclipse.mylyn.xplanner.ui"; //$NON-NLS-1$

	public final static String XPLANNER_CLIENT_LABEL = Messages.XPlannerPlugin_CLIENT_LABEL;

	public final static String TITLE_MESSAGE_DIALOG = Messages.XPlannerPlugin_CLIENT_DIALOG_TITLE;

	public static final String DELIM_URL_PREFIX = "/do/view/"; //$NON-NLS-1$

	public final static String DELIM_URL_SUFFIX = "?oid="; //$NON-NLS-1$

	public final static String TASK_URL_PREFIX = DELIM_URL_PREFIX + "task" + DELIM_URL_SUFFIX; //$NON-NLS-1$

	public final static String USER_STORY_URL_PREFIX = DELIM_URL_PREFIX + "userstory" + DELIM_URL_SUFFIX; //$NON-NLS-1$

	public final static String ITERATION_URL_PREFIX = DELIM_URL_PREFIX + "iteration" + DELIM_URL_SUFFIX; //$NON-NLS-1$

	// Preference setting names
	public final static String USE_AUTO_TIME_TRACKING_PREFERENCE_NAME = "UseAutoTimeTracking"; //$NON-NLS-1$

	public final static String ROUND_AUTO_TIME_TRACKING_TO_HALF_HOUR_PREFERENCE_NAME = "RoundAutoTimeTrackingToHalfHour"; //$NON-NLS-1$

	public final static String ADD_AUTO_TRACKED_TIME_TO_REPOSITORY_VALUE_PREFERENCE_NAME = "AddAutoTrackedTimeToRepositoryValue"; //$NON-NLS-1$

	@Deprecated
	public static final String REPOSITORY_KIND = "xplanner"; //$NON-NLS-1$

	public XPlannerUiPlugin() {
		INSTANCE = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		setPreferenceDefaults();
	}

	private void setPreferenceDefaults() {
		getPreferenceStore().setDefault(USE_AUTO_TIME_TRACKING_PREFERENCE_NAME, false);
		getPreferenceStore().setDefault(ROUND_AUTO_TIME_TRACKING_TO_HALF_HOUR_PREFERENCE_NAME, true);
		getPreferenceStore().setDefault(ADD_AUTO_TRACKED_TIME_TO_REPOSITORY_VALUE_PREFERENCE_NAME, true);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		INSTANCE = null;
		if (XPlannerClientFacade.isInitialized()) { // avoid initialization in XPlannerClientFacade.getDefault()
			XPlannerClientFacade.getDefault().logOutFromAll();
		}
	}

	public static void log(final Throwable e, final String message, boolean informUser) {
		if (Platform.isRunning() && informUser) {
			try {
				if (!CoreUtil.TEST_MODE) {
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						public void run() {
							Shell shell = null;
							if (PlatformUI.getWorkbench() != null
									&& PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
								shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
							}
							String displayMessage = message == null ? e.getMessage() : message + "\n" + e.getMessage(); //$NON-NLS-1$
							MessageDialog.openError(shell, Messages.XPlannerPlugin_XPLANNER_ERROR_TITLE, displayMessage);
						}
					});
				} else {
					System.err.println("XPlanner error: " + message); //$NON-NLS-1$
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		} else {
			StatusHandler.log(new Status(IStatus.ERROR, XPlannerUiPlugin.ID_PLUGIN, message == null
					|| message.length() == 0 ? Messages.XPlannerPlugin_XPLANNER_ERROR_TITLE : message, e));
		}
	}

	public static XPlannerUiPlugin getDefault() {
		return INSTANCE;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path.
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(ID_PLUGIN, path);
	}

	public static void setBooleanPreference(String name, boolean value) {
		getDefault().getPreferenceStore().setValue(name, value);
	}

	public static boolean getBooleanPreference(String name) {
		return getDefault().getPreferenceStore().getBoolean(name);
	}
}