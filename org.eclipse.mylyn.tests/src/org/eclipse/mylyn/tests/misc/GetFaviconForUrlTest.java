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

package org.eclipse.mylar.tests.misc;

import java.net.MalformedURLException;

import org.eclipse.mylar.core.net.WebClientUtil;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import junit.framework.TestCase;

/**
 * @author Leo Dos Santos
 */
public class GetFaviconForUrlTest extends TestCase {
	
	public void testMalformedUrl() {
		boolean exceptionThrown = false;
		try {
			WebClientUtil.getFaviconForUrl("www.eclipse.org");
		} catch (MalformedURLException e) {
			exceptionThrown = true;
		} 
		assertTrue(exceptionThrown);
	}
	
	public void testEclipseDotOrg() {
		Image img = null;
		try {
			img = WebClientUtil.getFaviconForUrl("http://www.eclipse.org");
		} catch (MalformedURLException e) {
			fail();
		}
		assertNotNull(img);
		ImageData data = img.getImageData();
		assertEquals(data.height, 16);
		assertEquals(data.width, 16);
	}
	
	public void testNoFavicon() {
		Image img = null;
		try {
			img = WebClientUtil.getFaviconForUrl("http://help.eclipse.org/help32/index.jsp");
		} catch (MalformedURLException e) {
			fail();
		}
		assertNull(img);
	}
	
}
