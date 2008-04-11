/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.java.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.mylyn.context.tests.support.TestUtil;
import org.eclipse.mylyn.internal.resources.ui.ResourcesUiBridgePlugin;
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
		TestSuite suite = new TestSuite("Tests for org.eclipse.mylyn.java.tests");

		// API-3.0 replace with context UI lazy start extension 
		// NOTE: used to trigger activation on start
		ResourcesUiBridgePlugin.getDefault();

		TestUtil.triggerContextUiLazyStart();

		// $JUnit-BEGIN$
		suite.addTestSuite(ContentSpecificContextTest.class);
		suite.addTestSuite(ResourceStructureMappingTest.class);
		// XXX: Put back
		suite.addTestSuite(InterestManipulationTest.class);
		suite.addTestSuite(EditorManagerTest.class);
		suite.addTestSuite(RefactoringTest.class);
		suite.addTestSuite(ContentOutlineRefreshTest.class);
		suite.addTestSuite(TypeHistoryManagerTest.class);
		suite.addTestSuite(PackageExplorerRefreshTest.class);
		// Enable?
		//suite.addTestSuite(ResultUpdaterTest.class);
		suite.addTestSuite(ProblemsListTest.class);
		suite.addTestSuite(InterestFilterTest.class);
		suite.addTestSuite(ContextManagerTest.class);
		suite.addTestSuite(JavaStructureTest.class);
		suite.addTestSuite(JavaImplementorsSearchPluginTest.class);
		suite.addTestSuite(JavaReadAccessSearchPluginTest.class);
		suite.addTestSuite(JavaReferencesSearchTest.class);
		suite.addTestSuite(JavaWriteAccessSearchPluginTest.class);
		suite.addTestSuite(JUnitReferencesSearchPluginTest.class);
		suite.addTestSuite(XmlSearchPluginTest.class);
		// $JUnit-END$

//		ResourcesUiBridgePlugin.getDefault().setResourceMonitoringEnabled(true);
		return suite;
	}
}
