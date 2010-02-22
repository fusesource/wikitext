/*******************************************************************************
 * Copyright (c) 2004, 2010 Tasktop Technologies and others.
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

import org.eclipse.mylyn.bugzilla.tests.support.BugzillaFixture;
import org.eclipse.mylyn.bugzilla.tests.ui.BugzillaRepositorySettingsPageTest;
import org.eclipse.mylyn.bugzilla.tests.ui.BugzillaSearchPageTest;
import org.eclipse.mylyn.bugzilla.tests.ui.BugzillaTaskHyperlinkDetectorTest;
import org.eclipse.mylyn.bugzilla.tests.ui.TaskEditorTest;

/**
 * @author Mik Kersten
 */
public class AllBugzillaTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for org.eclipse.mylyn.bugzilla.tests");
		suite.addTest(AllBugzillaHeadlessStandaloneTests.suite());

		for (BugzillaFixture fixture : BugzillaFixture.ALL) {
			fixture.createSuite(suite);
			// only run certain tests against head to avoid spurious failures 
			if (fixture != BugzillaFixture.BUGS_HEAD) {
				fixture.add(BugzillaTaskDataHandlerTest.class);
				fixture.add(TaskEditorTest.class);
				fixture.add(BugzillaRepositorySettingsPageTest.class);
				fixture.add(RepositoryReportFactoryTest.class);
				fixture.add(BugzillaTaskHyperlinkDetectorTest.class);
				fixture.add(BugzillaSearchTest.class);
				fixture.add(BugzillaRepositoryConnectorTest.class);
				fixture.add(BugzillaAttachmentHandlerTest.class);
				fixture.add(EncodingTest.class);
				fixture.add(BugzillaSearchPageTest.class);
				fixture.add(BugzillaRepository32Test.class);
				fixture.add(BugzillaDateTimeTests.class);
			}
			fixture.done();
		}

		return suite;
	}

}
