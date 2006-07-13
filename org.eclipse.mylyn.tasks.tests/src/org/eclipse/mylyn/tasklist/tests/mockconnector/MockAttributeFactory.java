/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.tasklist.tests.mockconnector;

import org.eclipse.mylar.tasks.core.AbstractAttributeFactory;



/**
 * @author Rob Elves
 */
public class MockAttributeFactory extends AbstractAttributeFactory {

	private static final long serialVersionUID = 7713746838934802731L;

	@Override
	public boolean getIsHidden(String key) {
		// ignore
		return false;
	}

	@Override
	public String getName(String key) {
		// ignore
		return key;
	}

	@Override
	public boolean isReadOnly(String key) {
		// ignore
		return false;
	}

	@Override
	public String mapCommonAttributeKey(String key) {
		return key;
	}

}
