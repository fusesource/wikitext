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

package org.eclipse.mylar.internal.bugzilla.ui.actions;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaPlugin;
import org.eclipse.swt.widgets.Shell;

/**
 * Class that contains shared functions for the favorites actions
 */
public class AbstractFavoritesAction extends Action {

	/**
	 * Set the actions icon
	 * 
	 * @param filename
	 *            The icons file
	 */
	protected void setIcon(String filename) {
		URL url = null;
		try {
			// try to change the default icon
			url = new URL(BugzillaPlugin.getDefault().getBundle().getEntry("/"), filename);
			setImageDescriptor(ImageDescriptor.createFromURL(url));
		} catch (MalformedURLException e) {
			// Simply don't change the default icon
		}
	}

	/**
	 * Convienience method for getting the current shell
	 * 
	 * @return The current shell
	 */
	protected Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

}
