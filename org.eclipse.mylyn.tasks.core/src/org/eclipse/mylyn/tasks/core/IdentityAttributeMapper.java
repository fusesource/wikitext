/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core;

import org.eclipse.mylyn.tasks.core.data.AbstractAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

/**
 * @since 3.0
 * @author Steffen Pingel
 */
public class IdentityAttributeMapper extends AbstractAttributeMapper {

	private static final IdentityAttributeMapper INSTANCE = new IdentityAttributeMapper();

	public static AbstractAttributeMapper getInstance() {
		return INSTANCE;
	}

	private IdentityAttributeMapper() {
	}

	@Override
	public String getType(TaskAttribute taskAttribute) {
		return TaskAttribute.TYPE_SHORT_TEXT;
	}

}
