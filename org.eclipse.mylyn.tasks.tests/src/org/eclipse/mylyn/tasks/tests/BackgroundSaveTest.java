/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.tasks.tests;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import junit.framework.TestCase;

import org.eclipse.mylyn.internal.tasks.core.LocalTask;
import org.eclipse.mylyn.internal.tasks.core.externalization.TaskListExternalizationParticipant;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;

/**
 * Tests the mechanism for saving the task data periodically.
 * 
 * @author Wesley Coelho
 * @author Mik Kersten (rewrite)
 */
public class BackgroundSaveTest extends TestCase {

	public void testBackgroundSave() throws InterruptedException, IOException {
		if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("linux")) {
			System.out.println("> BackgroundSaveTest.testBackgroundSave() not run on Linux due to IO concurrency");
		} else {
			LocalTask task = new LocalTask("1", "summary");
			File file = TaskListExternalizationParticipant.getTaskListFile(TasksUiPlugin.getDefault()
					.getDataDirectory());
			long previouslyModified = file.lastModified();
			TasksUiPlugin.getTaskList().addTask(task);
			TasksUiPlugin.getExternalizationManager().requestSave();
			Thread.sleep(5000);
			assertTrue(file.lastModified() > previouslyModified);
			TasksUiPlugin.getTaskList().deleteTask(task);
		}
	}
}
