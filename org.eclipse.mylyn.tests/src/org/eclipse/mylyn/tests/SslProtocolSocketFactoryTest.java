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

import java.net.InetAddress;
import java.net.Socket;

import junit.framework.TestCase;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.eclipse.mylar.core.net.SslProtocolSocketFactory;

public class SslProtocolSocketFactoryTest extends TestCase {

	public void testTrustAllSslProtocolSocketFactory() throws Exception {
		SslProtocolSocketFactory factory = SslProtocolSocketFactory.getInstance();
		Socket s;

		s = factory.createSocket("mylar.eclipse.org", 80);
		assertNotNull(s);
		assertTrue(s.isConnected());
		s.close();

		InetAddress anyHost = new Socket().getLocalAddress();

		s = factory.createSocket("mylar.eclipse.org", 80, anyHost, 0);
		assertNotNull(s);
		assertTrue(s.isConnected());
		s.close();

		HttpConnectionParams params = new HttpConnectionParams();
		s = factory.createSocket("mylar.eclipse.org", 80, anyHost, 0, params);
		assertNotNull(s);
		assertTrue(s.isConnected());
		s.close();

		params.setConnectionTimeout(1000);
		s = factory.createSocket("mylar.eclipse.org", 80, anyHost, 0, params);
		assertNotNull(s);
		assertTrue(s.isConnected());
		s.close();
	}

}
