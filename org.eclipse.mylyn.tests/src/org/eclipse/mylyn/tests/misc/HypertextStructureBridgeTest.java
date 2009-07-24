/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tests.misc;

import junit.framework.TestCase;

import org.eclipse.mylyn.internal.web.ui.WebResourceStructureBridge;

/**
 * @author Mik Kersten
 */
public class HypertextStructureBridgeTest extends TestCase {

	private WebResourceStructureBridge bridge = new WebResourceStructureBridge();

	public void testParentHandle() {
		String site = "http://www.foo.bar";
		String page = "http://www.foo.bar/part/index.html";
		assertEquals(site, bridge.getParentHandle(page));
	}

}