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

package org.eclipse.mylar.monitor.reports.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Path;
import org.eclipse.mylar.internal.monitor.InteractionEventLogger;
import org.eclipse.mylar.internal.monitor.MylarMonitorPlugin;
import org.eclipse.mylar.internal.monitor.reports.ReportGenerator;
import org.eclipse.mylar.internal.monitor.reports.collectors.DataOverviewCollector;
import org.eclipse.mylar.monitor.reports.IUsageCollector;
import org.eclipse.mylar.monitor.tests.MylarMonitorTestsPlugin;

/**
 * 
 * @author Gail Murphy
 */
public class DataOverviewCollectorTest extends TestCase {

	private DataOverviewCollector dataOverviewCollector = null;

	public void testNumberOfUsers() {
		assertTrue(dataOverviewCollector.getNumberOfUsers() == 2);
	}

	public void testActiveUse() {
		long activeUse = dataOverviewCollector.getActiveUseOfUser(1);
		assertTrue("User 1 Use", getHoursOfDuration(activeUse) == 0);
		activeUse = dataOverviewCollector.getActiveUseOfUser(2);
		assertTrue("User 2 Use", getHoursOfDuration(activeUse) == 0);

	}

	public void testTimePeriodOfUse() {
		long durationOfUse = dataOverviewCollector.getDurationUseOfUser(1);
		assertTrue("User 1 duration", getHoursOfDuration(durationOfUse) == 24);
		durationOfUse = dataOverviewCollector.getDurationUseOfUser(2);
		assertTrue("User 2 duration", getHoursOfDuration(durationOfUse) == 24);
	}

	public void testSizeOfHistory() {
		int size = dataOverviewCollector.getSizeOfHistory(1);
		assertTrue("User 1 size", size == 21);
		size = dataOverviewCollector.getSizeOfHistory(2);
		assertTrue("User 2 size", size == 21);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		List<File> interactionHistoryFiles = new ArrayList<File>();

		// Access two interaction history files that are copies of each other
		File firstInteractionHistoryFile = FileTool.getFileInPlugin(MylarMonitorTestsPlugin.getDefault(), new Path(
				"testdata/USAGE-1.1.1-usage-1-2005-12-05-1-1-1.zip"));
		interactionHistoryFiles.add(firstInteractionHistoryFile);
		File secondInteractionHistoryFile = FileTool.getFileInPlugin(MylarMonitorTestsPlugin.getDefault(), new Path(
				"testdata/USAGE-1.1.1-usage-2-2005-12-05-1-1-1.zip"));
		interactionHistoryFiles.add(secondInteractionHistoryFile);

		// Initialize fake logger
		File logFile = new File("test-log.xml");
		logFile.delete();
		InteractionEventLogger logger = new InteractionEventLogger(logFile);
		logger.startObserving();

		// Prepare collectors
		List<IUsageCollector> collectors = new ArrayList<IUsageCollector>();
		dataOverviewCollector = new DataOverviewCollector("test-");
		collectors.add(dataOverviewCollector);

		ReportGenerator generator = new ReportGenerator(MylarMonitorPlugin.getDefault().getInteractionLogger(),
				collectors);
		generator.getStatisticsFromInteractionHistories(interactionHistoryFiles);

		// cleanup
		logFile.delete();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private long getHoursOfDuration(long duration) {
		long timeInSeconds = duration / 1000;
		long hours = timeInSeconds / 3600;
		return hours;
	}

}
