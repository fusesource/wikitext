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

package org.eclipse.mylar.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.mylar.bugzilla.tests.AllBugzillaTests;
import org.eclipse.mylar.context.tests.AllCoreTests;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.ide.tests.AllIdeTests;
import org.eclipse.mylar.java.tests.AllJavaTests;
import org.eclipse.mylar.jira.tests.AllJiraTests;
import org.eclipse.mylar.monitor.tests.AllMonitorTests;
import org.eclipse.mylar.resources.MylarResourcesPlugin;
import org.eclipse.mylar.resources.tests.AllResourcesTests;
import org.eclipse.mylar.tasks.tests.AllTasksTests;
import org.eclipse.mylar.tests.integration.AllIntegrationTests;
import org.eclipse.mylar.tests.integration.TestingStatusNotifier;
import org.eclipse.mylar.tests.misc.AllMiscTests;
import org.eclipse.mylar.trac.tests.AllTracTests;

/**
 * @author Mik Kersten
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.eclipse.mylar.tests");

		MylarStatusHandler.addStatusHandler(new TestingStatusNotifier());
		MylarResourcesPlugin.getDefault().setResourceMonitoringEnabled(false);

		// TODO: the order of these tests might still matter, but shouldn't
		
		// $JUnit-BEGIN$
		suite.addTest(AllMonitorTests.suite());
		suite.addTest(AllIntegrationTests.suite());
		suite.addTest(AllCoreTests.suite());
		suite.addTest(AllIdeTests.suite());
		suite.addTest(AllJavaTests.suite());
		suite.addTest(AllTasksTests.suite());
		suite.addTest(AllResourcesTests.suite());
		suite.addTest(AllBugzillaTests.suite());
		suite.addTest(AllMiscTests.suite());
		suite.addTest(AllJiraTests.suite());
		suite.addTest(AllTracTests.suite());
		suite.addTestSuite(WebClientUtilTest.class);
		suite.addTestSuite(SslProtocolSocketFactoryTest.class);
		// $JUnit-END$
		return suite;
	}
}
