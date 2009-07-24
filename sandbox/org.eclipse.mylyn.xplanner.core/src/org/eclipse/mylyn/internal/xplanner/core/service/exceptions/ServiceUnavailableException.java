/*******************************************************************************
 * Copyright (c) 2007, 2009 CodeGear and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.internal.xplanner.core.service.exceptions;

/**
 * @author Ravi Kumar
 * @author Helen Bershadskaya
 */
public class ServiceUnavailableException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ServiceUnavailableException(String message) {
		super(message);
	}
}
