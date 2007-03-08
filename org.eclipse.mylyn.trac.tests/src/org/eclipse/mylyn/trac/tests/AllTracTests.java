/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.trac.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.mylar.internal.trac.ui.TracHyperlinkDetectorTest;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class AllTracTests {

	public static Test suite() {
//		UrlConnectionUtil.initCommonsLoggingSettings();
		
		TestSuite suite = new TestSuite("Test for org.eclipse.mylar.trac.tests");
		// $JUnit-BEGIN$
		// suite.addTestSuite(TracXmlRpcTest.class);
		suite.addTestSuite(TracSearchTest.class);
		suite.addTestSuite(TracTicketTest.class);
		suite.addTestSuite(TracXmlRpcClientTest.class);
		suite.addTestSuite(TracXmlRpcClientSearchTest.class);
		suite.addTestSuite(TracWebClientTest.class);
		suite.addTestSuite(TracWebClientSearchTest.class);
		suite.addTestSuite(TracClientFactoryTest.class);
		suite.addTestSuite(TracRepositoryConnectorTest.class);
		suite.addTestSuite(TracQueryTest.class);
		suite.addTestSuite(TracRepositoryQueryTest.class);
		suite.addTestSuite(TracClientManagerTest.class);
		suite.addTestSuite(TracAttachmentHandlerTest.class);
		suite.addTestSuite(RepositorySearchQueryTest.class);
		suite.addTestSuite(TracTaskDataHandlerTest.class);
		suite.addTestSuite(TracTaskTest.class);
		suite.addTestSuite(TracQueryHitTest.class);
		suite.addTestSuite(TracRepositorySettingsPageTest.class);
		suite.addTestSuite(TracClientProxyTest.class);
		suite.addTestSuite(TracHyperlinkDetectorTest.class);
		// $JUnit-END$
		return suite;
	}

}