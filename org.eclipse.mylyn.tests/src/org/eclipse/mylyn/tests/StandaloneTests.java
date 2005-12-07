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

package org.eclipse.mylar.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.mylar.core.tests.ContextTest;
import org.eclipse.mylar.core.tests.DegreeOfInterestTest;
import org.eclipse.mylar.tasklist.tests.TaskListStandaloneTest;

/**
 * @author Mik Kersten
 */
public class StandaloneTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests not requiring Eclipse Workbench");
		
		//$JUnit-BEGIN$
		suite.addTestSuite(DegreeOfInterestTest.class);
		suite.addTestSuite(ContextTest.class);
		suite.addTestSuite(TaskListStandaloneTest.class);
		//$JUnit-END$
		return suite;
	}
}
