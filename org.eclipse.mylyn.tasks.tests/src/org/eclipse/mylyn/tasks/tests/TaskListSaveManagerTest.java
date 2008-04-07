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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import junit.framework.TestCase;

import org.eclipse.mylyn.internal.tasks.core.TaskActivityUtil;
import org.eclipse.mylyn.internal.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.internal.tasks.ui.TaskListBackupManager;
import org.eclipse.mylyn.internal.tasks.ui.util.TaskListSaveManager;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;

/**
 * @author Rob Elves
 */
public class TaskListSaveManagerTest extends TestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRemovalOfSnapshots() throws IOException {
		String backupPath = TasksUiPlugin.getDefault().getBackupFolderPath();
		File backupFolder = new File(backupPath);
		if (!backupFolder.exists()) {
			backupFolder.mkdir();
		}

		Calendar time = TaskActivityUtil.getCalendar();

		File thisHour = createFile(backupFolder, time);
		time.add(Calendar.MILLISECOND, -1000);
		File thisHourEarlier = createFile(backupFolder, time);

		TaskActivityUtil.snapStartOfHour(time);
		time.add(Calendar.HOUR_OF_DAY, -1);
		time.add(Calendar.MILLISECOND, -3000);
		File previousHour = createFile(backupFolder, time);

		time.add(Calendar.MILLISECOND, -3000);
		File previousHour2 = createFile(backupFolder, time);

		time.add(Calendar.DAY_OF_YEAR, -1);
		File previousDay = createFile(backupFolder, time);

		time.add(Calendar.HOUR_OF_DAY, -1);
		File previousDay2 = createFile(backupFolder, time);

		time.add(Calendar.DAY_OF_YEAR, -3);
		File previousDay3 = createFile(backupFolder, time);

		time.add(Calendar.HOUR_OF_DAY, -1);
		File previousDay4 = createFile(backupFolder, time);

		time.add(Calendar.MILLISECOND, -4000);
		File previousDay5 = createFile(backupFolder, time);

		TaskListBackupManager.removeOldBackups(backupFolder, TaskListSaveManager.SNAPSHOT_REGEXP, 0);
		assertTrue(thisHour.exists());
		assertFalse(thisHourEarlier.exists());
		assertTrue(previousHour.exists());
		assertFalse(previousHour2.exists());
		assertTrue(previousDay.exists());
		assertFalse(previousDay2.exists());
		assertTrue(previousDay3.exists());
		assertFalse(previousDay4.exists());
		assertFalse(previousDay5.exists());
	}

	private File createFile(File backupFolder, Calendar time) {
		SimpleDateFormat format = new SimpleDateFormat(TaskListBackupManager.TIMESTAMP_FORMAT, Locale.ENGLISH);
		File newFile = new File(backupFolder, ITasksUiConstants.PREFIX_TASKLIST + "-" + format.format(time.getTime())
				+ ITasksUiConstants.FILE_EXTENSION);
		try {
			newFile.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newFile;
	}
}
