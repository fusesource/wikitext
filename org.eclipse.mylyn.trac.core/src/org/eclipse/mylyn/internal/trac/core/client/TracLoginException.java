/*******************************************************************************
* Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.trac.core.client;

/**
 * Indicates an authentication error during login.
 * 
 * @author Steffen Pingel
 */
public class TracLoginException extends TracException {

	private static final long serialVersionUID = -6128773690643367414L;

	public TracLoginException() {
	}

	public TracLoginException(String message) {
		super(message);
	}

}
