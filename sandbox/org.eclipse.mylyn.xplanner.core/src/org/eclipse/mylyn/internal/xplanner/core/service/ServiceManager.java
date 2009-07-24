/*******************************************************************************
 * Copyright (c) 2007, 2009 CodeGear and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.internal.xplanner.core.service;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.mylyn.internal.xplanner.core.IXPlannerCoreExtensionConstants;
import org.eclipse.mylyn.internal.xplanner.core.XPlannerCorePlugin;

/**
 * TODO This is eclipse specific at the moment. Need to find a way to load a XPlannerService instance based off some use
 * preference. TODO mention that this should really only be used by internal classes and tests
 * 
 * @author Ravi Kumar
 * @author Helen Bershadskaya
 */
public class ServiceManager {

	private static XPlannerServiceFactory factory;

	public static synchronized XPlannerService getXPlannerService(XPlannerClient client) {
		if (factory == null) {
			factory = loadServiceProviderFactories();
		}
		return factory.createService(client);
	}

	public static XPlannerServiceFactory loadServiceProviderFactories() {
		XPlannerCorePlugin plugin = XPlannerCorePlugin.getDefault();
		if (plugin != null) {
			IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(XPlannerCorePlugin.ID,
					IXPlannerCoreExtensionConstants.SERVICE_PROVIDER_FACTORY);
			if (extension != null) {
				IExtension[] extensions = extension.getExtensions();
				for (IExtension extension2 : extensions) {
					IConfigurationElement[] configElements = extension2.getConfigurationElements();
					for (IConfigurationElement configElement : configElements) {
						try {
							return (XPlannerServiceFactory) configElement.createExecutableExtension("class"); //$NON-NLS-1$
						} catch (ClassCastException e) {
							XPlannerCorePlugin.log(IStatus.ERROR, "Must implement the correct class", e); //$NON-NLS-1$
						} catch (Throwable e) {
							XPlannerCorePlugin.log(IStatus.ERROR, "Failed to load XPlanner service", e); //$NON-NLS-1$
						}
						return null;
					}
				}
			}
		}
		return null;
	}
}
