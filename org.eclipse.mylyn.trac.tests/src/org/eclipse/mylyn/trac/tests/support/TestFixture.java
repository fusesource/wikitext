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

package org.eclipse.mylar.trac.tests.support;

import org.eclipse.mylar.core.core.tests.support.MylarTestUtils;
import org.eclipse.mylar.core.core.tests.support.MylarTestUtils.Credentials;
import org.eclipse.mylar.core.core.tests.support.MylarTestUtils.PrivilegeLevel;
import org.eclipse.mylar.trac.tests.Constants;

/**
 * Initializes Trac repositories to a defined state. This is done once per test
 * run, since cleaning and initializing the repository for each test method
 * would take too long.
 * 
 * @author Steffen Pingel
 */
public class TestFixture {

	public static XmlRpcServer.TestData data010;

	/**
	 * Adds the existing repository content to the test data of
	 * <code>server</code>.
	 */
	private static void initializeTestData(XmlRpcServer server) throws Exception {
		server.ticketMilestone("milestone1").itemCreated();
		server.ticketMilestone("milestone2").itemCreated();
		server.ticketMilestone("milestone3").itemCreated();
		server.ticketMilestone("milestone4").itemCreated();

		server.ticketVersion("1.0").itemCreated();
		server.ticketVersion("2.0").itemCreated();

		server.ticket(1).itemCreated();
		server.ticket(2).itemCreated();
		server.ticket(3).itemCreated();
		server.ticket(4).itemCreated();
		server.ticket(5).itemCreated();
		server.ticket(6).itemCreated();
		server.ticket(7).itemCreated();
	}

//	private static void initializeRepository(XmlRpcServer server) throws Exception {
//		server.ticketVersion(null).deleteAll();
//		server.ticketVersion("1.0").create(0, "");
//		server.ticketVersion("2.0").create(0, "");
//
//		server.ticketMilestone(null).deleteAll();
//		server.ticketMilestone("milestone1").create();
//		server.ticketMilestone("milestone2").create();
//		server.ticketMilestone("milestone3").create();
//		server.ticketMilestone("milestone4").create();
//
//		server.ticket().deleteAll();
//		Ticket ticket = server.ticket().create("summary1", "description1");
//		ticket.update("comment", "milestone", "milestone1");
//		ticket = server.ticket().create("summary2", "description2");
//		ticket.update("comment", "milestone", "milestone2");
//		ticket = server.ticket().create("summary3", "description3");
//		ticket.update("comment", "milestone", "milestone2");
//		ticket = server.ticket().create("summary4", "description4");
//	
//	    ticket = server.ticket().create("attachment test", "");
//	    // create attachment named attachment.txt that contains the text "Mylar"
//	    ticket = server.ticket().create("test html entities: ���", "���\n\nmulti\nline\n\n'''bold'''\n");
// 	    ticket = server.ticket().create("offline handler test", "");
//	}

	public static XmlRpcServer.TestData init010() throws Exception {
		if (data010 == null) {
			Credentials credentials = MylarTestUtils.readCredentials(PrivilegeLevel.USER);
			XmlRpcServer server = new XmlRpcServer(Constants.TEST_TRAC_010_URL, credentials.username,
					credentials.password);

			initializeTestData(server);
			data010 = server.getData();
		}
		return data010;
	}

	public static void cleanup010() throws Exception {
		if (data010 != null) {
			// data010.cleanup();
			data010 = null;
		}
	}

}
