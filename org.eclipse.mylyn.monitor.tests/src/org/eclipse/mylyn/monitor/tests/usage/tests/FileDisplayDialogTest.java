/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.monitor.tests.usage.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Calendar;

import org.eclipse.core.runtime.Path;
import org.eclipse.mylar.context.tests.support.FileTool;
import org.eclipse.mylar.internal.monitor.usage.ui.FileDisplayDialog;
import org.eclipse.mylar.monitor.tests.MylarMonitorTestsPlugin;

import junit.framework.TestCase;


/**
 * @author Meghan Allen
 */
public class FileDisplayDialogTest extends TestCase {
	
	private static final long TWO_SECONDS = 2* 1000;
		
	File monitorFile;
	
	@Override
	protected void setUp() throws Exception {
		monitorFile = FileTool.getFileInPlugin(MylarMonitorTestsPlugin.getDefault(), new Path(
		"testdata/monitor-history.xml"));
	}

	@Override
	protected void tearDown() throws Exception {

	}
	
	public void testGetContents() throws FileNotFoundException {
		long startTime = Calendar.getInstance().getTimeInMillis();
		FileDisplayDialog.getContents(monitorFile);
		long endTime = Calendar.getInstance().getTimeInMillis();
				
		assertTrue( endTime - startTime <= TWO_SECONDS);
	}
	
}
