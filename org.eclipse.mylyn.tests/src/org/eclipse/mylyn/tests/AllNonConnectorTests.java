/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.mylyn.commons.tests.AllCommonsTests;
import org.eclipse.mylyn.context.tests.AllContextTests;
import org.eclipse.mylyn.discovery.tests.AllDiscoveryTests;
import org.eclipse.mylyn.ide.tests.AllIdeTests;
import org.eclipse.mylyn.java.tests.AllJavaTests;
import org.eclipse.mylyn.monitor.tests.AllMonitorTests;
import org.eclipse.mylyn.resources.tests.AllResourcesTests;
import org.eclipse.mylyn.tasks.tests.AllTasksTests;
import org.eclipse.mylyn.team.tests.AllTeamTests;
import org.eclipse.mylyn.tests.integration.AllIntegrationTests;
import org.eclipse.mylyn.tests.misc.AllMiscTests;

/**
 * @author Shawn Minto
 */
public class AllNonConnectorTests {

	public static Test suite() {
		// the order of these tests might still matter, but shouldn't
		TestSuite suite = new TestSuite("All Non-Connector Tests for org.eclipse.mylyn.tests");
		suite.addTest(AllCommonsTests.suite());
		suite.addTest(AllContextTests.suite());
		suite.addTest(AllDiscoveryTests.suite());
		suite.addTest(AllJavaTests.suite());
		suite.addTest(AllMonitorTests.suite());
		suite.addTest(AllIntegrationTests.suite());
		suite.addTest(AllIdeTests.suite());
		suite.addTest(AllTasksTests.suite());
		suite.addTest(AllResourcesTests.suite());
		suite.addTest(AllTeamTests.suite());
		suite.addTest(AllMiscTests.suite());
		suite.addTest(org.eclipse.mylyn.wikitext.tests.HeadlessTests.suite());
		return suite;
	}
}
