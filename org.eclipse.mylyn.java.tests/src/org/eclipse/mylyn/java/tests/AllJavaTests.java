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

package org.eclipse.mylyn.java.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.mylyn.context.tests.support.ContextTestUtil;
import org.eclipse.mylyn.java.tests.search.JUnitReferencesSearchPluginTest;
import org.eclipse.mylyn.java.tests.search.JavaImplementorsSearchPluginTest;
import org.eclipse.mylyn.java.tests.search.JavaReadAccessSearchPluginTest;
import org.eclipse.mylyn.java.tests.search.JavaReferencesSearchTest;
import org.eclipse.mylyn.java.tests.search.JavaWriteAccessSearchPluginTest;
import org.eclipse.mylyn.java.tests.xml.XmlSearchPluginTest;

/**
 * @author Mik Kersten
 */
public class AllJavaTests {

	public static Test suite() {
		ContextTestUtil.triggerContextUiLazyStart();

		TestSuite suite = new TestSuite("Tests for org.eclipse.mylyn.java.tests");
		suite.addTestSuite(ContentSpecificContextTest.class);
		suite.addTestSuite(ResourceStructureMappingTest.class);
		suite.addTestSuite(InterestManipulationTest.class);
		suite.addTestSuite(EditorManagerTest.class);
		suite.addTestSuite(RefactoringTest.class);
		suite.addTestSuite(ContentOutlineRefreshTest.class);
		suite.addTestSuite(TypeHistoryManagerTest.class);
		suite.addTestSuite(PackageExplorerRefreshTest.class);
		// XXX 3.5 re-enable test case?
		//suite.addTestSuite(ResultUpdaterTest.class);
		suite.addTestSuite(ProblemsListTest.class);
		suite.addTestSuite(InterestFilterTest.class);
		suite.addTestSuite(InteractionContextManagerTest.class);
		suite.addTestSuite(JavaStructureTest.class);
		suite.addTestSuite(JavaImplementorsSearchPluginTest.class);
		suite.addTestSuite(JavaReadAccessSearchPluginTest.class);
		suite.addTestSuite(JavaReferencesSearchTest.class);
		suite.addTestSuite(JavaWriteAccessSearchPluginTest.class);
		suite.addTestSuite(JUnitReferencesSearchPluginTest.class);
		suite.addTestSuite(XmlSearchPluginTest.class);
		suite.addTestSuite(JavaEditingMonitorTest.class);
		return suite;
	}
}
