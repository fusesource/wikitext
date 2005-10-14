/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.monitor.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.mylar.core.InteractionEvent;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.java.MylarJavaPlugin;
import org.eclipse.mylar.java.ui.actions.ApplyMylarToPackageExplorerAction;
import org.eclipse.mylar.monitor.InteractionEventLogger;
import org.eclipse.mylar.monitor.MylarMonitorPlugin;
import org.eclipse.mylar.monitor.reports.IStatsCollector;
import org.eclipse.mylar.monitor.reports.ReportGenerator;
import org.eclipse.mylar.monitor.reports.internal.MylarUserAnalysisCollector;
import org.eclipse.mylar.monitor.reports.internal.ViewUsageCollector;
import org.eclipse.mylar.monitor.reports.ui.views.UsageStatisticsSummary;
import org.eclipse.mylar.tasklist.ui.actions.TaskActivateAction;

/**
 * @author Mik Kersten
 */
public class StatisticsReportingTest extends TestCase {

	private InteractionEventLogger logger;
	private ViewUsageCollector viewCollector = new ViewUsageCollector();
	private MylarUserAnalysisCollector editRatioCollector = new MylarUserAnalysisCollector();;
	private ReportGenerator report;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		assertNotNull(MylarPlugin.getDefault());
		assertNotNull(MylarJavaPlugin.getDefault());
		assertNotNull(PackageExplorerPart.openInActivePerspective());
		
		logger = MylarMonitorPlugin.getDefault().getInteractionLogger();
		logger.stop();
		String path = logger.getOutputFile().getAbsolutePath();
		logger.getOutputFile().delete();
		logger.setOutputFile(new File(path));
		logger.start();
		
		List<IStatsCollector> collectors = new ArrayList<IStatsCollector>();
		collectors.add(viewCollector);
		collectors.add(editRatioCollector);
		report = new ReportGenerator(logger, collectors);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	protected void mockExplorerSelection(String handle) {
		MylarPlugin.getDefault().notifyInteractionObserved(
				new InteractionEvent(InteractionEvent.Kind.SELECTION, "java", handle, JavaUI.ID_PACKAGES)
    	);
	}

	protected void mockEdit(String handle) {
		MylarPlugin.getDefault().notifyInteractionObserved(
				new InteractionEvent(InteractionEvent.Kind.EDIT, "java", handle, JavaUI.ID_PACKAGES)
    	);
	}
	
	protected void mockTypesSelection(String handle) {
		MylarPlugin.getDefault().notifyInteractionObserved(
				new InteractionEvent(InteractionEvent.Kind.SELECTION, "java", handle, JavaUI.ID_TYPES_VIEW)
    	);
	}
	
	public void testEditRatio() {
		logger.stop();
		PackageExplorerPart part = PackageExplorerPart.openInActivePerspective();
		assertNotNull(part.getTreeViewer());
		assertNotNull(MylarJavaPlugin.getDefault());
		part.setFocus();
		
		logger.start();		
		mockExplorerSelection("A.java");
		mockExplorerSelection("A.java");
		mockEdit("A.java");		
		
		MylarPlugin.getDefault().notifyInteractionObserved(
				InteractionEvent.makeCommand(TaskActivateAction.ID, "")
    	);

		mockExplorerSelection("A.java");
		mockEdit("A.java");		
		mockEdit("A.java");		
		
		logger.stop();
		report.getStatisticsFromInteractionHistory(logger.getOutputFile());
		
//		System.err.println(">>> " + editRatioCollector.baselineEdits);
//		System.err.println(">>> " + editRatioCollector.baselineSelections);
		
		// TODO: these are off from expected when test run alone, due to unknown element selections
		assertEquals(0.5f, editRatioCollector.getBaselineRatio(-1));
		assertEquals(2f, editRatioCollector.getMylarRatio(-1));
	}
	
	public void testSimpleSelection() {
		mockExplorerSelection("A.java");
		UsageStatisticsSummary summary = report.getStatisticsFromInteractionHistory(logger.getOutputFile());
		assertTrue(summary.getSingleSummaries().size() > 0);
	}
	
	public void testFilteredModeDetection() {
		mockExplorerSelection("A.java");
		mockExplorerSelection("A.java");
		mockTypesSelection("A.java");
		
		MylarPlugin.getDefault().getPreferenceStore().setValue(ApplyMylarToPackageExplorerAction.getDefault().getPrefId(), true); 
		
		mockExplorerSelection("A.java");
		mockExplorerSelection("A.java");
		mockTypesSelection("A.java");
		
		MylarPlugin.getDefault().getPreferenceStore().setValue(ApplyMylarToPackageExplorerAction.getDefault().getPrefId(), false);
		
		mockExplorerSelection("A.java");
		
		logger.stop();
		report.getStatisticsFromInteractionHistory(logger.getOutputFile());
		
		int normal = viewCollector.getNormalViewSelections().get(JavaUI.ID_PACKAGES);
		int filtered = viewCollector.getFilteredViewSelections().get(JavaUI.ID_PACKAGES);
		
		assertEquals(5, normal);
		assertEquals(2, filtered);
	}
}
