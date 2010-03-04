/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.tasks.tests.core;

import junit.framework.TestCase;

import org.eclipse.mylyn.tasks.core.TaskRepository;

public class TaskRepositoryTest extends TestCase {

	private TaskRepository taskRepository;

	@Override
	protected void setUp() throws Exception {
		taskRepository = new TaskRepository("kind", "url");
	}

	public void testSetTaskRepositoryProperty() {
		String key = "key";
		String value = "value";
		taskRepository.setProperty(key, value);
		assertEquals(value, taskRepository.getProperty(key));
	}

	public void testResetTaskRepositoryProperty() {
		String key = "key";
		String value = "value";
		taskRepository.setProperty(key, value);
		assertEquals(value, taskRepository.getProperty(key));
		value = "newValue";
		taskRepository.setProperty(key, value);
		assertEquals(value, taskRepository.getProperty(key));
	}

	public void testSetTaskRepositoryPropertyWithSpace() {
		String key = "key 1";
		String value = "value";
		boolean caughtException = false;
		try {
			taskRepository.setProperty(key, value);
		} catch (IllegalArgumentException e) {
			caughtException = true;
		}
		assertTrue(caughtException);
	}

	public void testSetTaskRepositoryPropertyWithTab() {
		String key = "key\t1";
		String value = "value";
		boolean caughtException = false;
		try {
			taskRepository.setProperty(key, value);
		} catch (IllegalArgumentException e) {
			caughtException = true;
		}
		assertTrue(caughtException);
	}

	public void testSetTaskRepositoryPropertyWithNewline() {
		String key = "key\n1";
		String value = "value";
		boolean caughtException = false;
		try {
			taskRepository.setProperty(key, value);
		} catch (IllegalArgumentException e) {
			caughtException = true;
		}
		assertTrue(caughtException);
	}
}
