/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.editors;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * @author Shawn Minto
 */
public class LocalTaskEditorContributionDescriptor {

	private final IConfigurationElement element;

	private final String id;

	public LocalTaskEditorContributionDescriptor(IConfigurationElement element) {
		this.element = element;
		this.id = element.getAttribute("id"); //$NON-NLS-1$
	}

	public AbstractLocalEditorPart createPart() {
		try {
			return (AbstractLocalEditorPart) element.createExecutableExtension("class"); //$NON-NLS-1$
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getId() {
		return id;
	}

}
