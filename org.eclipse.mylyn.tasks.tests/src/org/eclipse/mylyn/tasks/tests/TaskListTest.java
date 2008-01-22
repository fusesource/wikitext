/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.tests;

import junit.framework.TestCase;

import org.eclipse.mylyn.internal.tasks.core.TaskCategory;
import org.eclipse.mylyn.tasks.core.TaskList;

/**
 * @author Mik Kersten
 * @author Rob Elves
 */
public class TaskListTest extends TestCase {

	public void testGetCategories() {
		TaskList taskList = new TaskList();
		taskList.addCategory(new TaskCategory("a"));
		assertEquals(2, taskList.getCategories().size());
	}

}
