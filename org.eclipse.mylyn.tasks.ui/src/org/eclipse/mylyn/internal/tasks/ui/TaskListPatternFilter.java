/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.internal.tasks.ui;

import org.eclipse.ui.dialogs.PatternFilter;

/**
 * @author Mik Kersten
 */
public class TaskListPatternFilter extends PatternFilter {

	@Override
	public void setPattern(String patternString) {
		if (patternString == null || patternString.startsWith("*")) {
			super.setPattern(patternString);
		} else {
			super.setPattern("*" + patternString);
		}
	}
}