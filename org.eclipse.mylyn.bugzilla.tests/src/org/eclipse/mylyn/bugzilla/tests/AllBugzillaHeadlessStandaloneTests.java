/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.bugzilla.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.mylyn.bugzilla.tests.core.BugzillaClientTest;
import org.eclipse.mylyn.bugzilla.tests.core.BugzillaConfigurationTest;
import org.eclipse.mylyn.bugzilla.tests.core.BugzillaRepositoryConnectorConfigurationTest;
import org.eclipse.mylyn.bugzilla.tests.core.BugzillaRepositoryConnectorStandaloneTest;
import org.eclipse.mylyn.bugzilla.tests.core.BugzillaVersionTest;
import org.eclipse.mylyn.bugzilla.tests.support.BugzillaFixture;

/**
 * @author Steffen Pingel
 * @author Thomas Ehrnhoefer
 */
public class AllBugzillaHeadlessStandaloneTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Headless Standalone Tests for org.eclipse.mylyn.bugzilla.tests");
		suite.addTestSuite(BugzillaConfigurationTest.class);
		suite.addTestSuite(BugzillaVersionTest.class);
		suite.addTestSuite(BugzillaTaskCompletionTest.class);
		for (BugzillaFixture fixture : BugzillaFixture.ALL) {
			fixture.createSuite(suite);
			// only run certain tests against head to avoid spurious failures 
			if (fixture != BugzillaFixture.BUGS_HEAD) {
				fixture.add(BugzillaClientTest.class);
				// XXX: re-enable when webservice is used for retrieval of history
				// fixture.add(fixtureSuite, BugzillaTaskHistoryTest.class); 
				fixture.add(BugzillaRepositoryConnectorStandaloneTest.class);
			}
			fixture.add(BugzillaRepositoryConnectorConfigurationTest.class);
			fixture.done();
		}
		return suite;
	}

}
