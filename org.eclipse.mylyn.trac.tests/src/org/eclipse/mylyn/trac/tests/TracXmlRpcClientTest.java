/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.trac.tests;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.internal.trac.core.TracPermissionDeniedException;
import org.eclipse.mylyn.internal.trac.core.TracRemoteException;
import org.eclipse.mylyn.internal.trac.core.TracXmlRpcClient;
import org.eclipse.mylyn.internal.trac.core.ITracClient.Version;
import org.eclipse.mylyn.internal.trac.core.model.TracVersion;
import org.eclipse.mylyn.internal.trac.core.model.TracWikiPage;
import org.eclipse.mylyn.internal.trac.core.model.TracWikiPageInfo;

/**
 * @author Steffen Pingel
 * @author Xiaoyang Guan
 */
public class TracXmlRpcClientTest extends AbstractTracClientRepositoryTest {

	public TracXmlRpcClientTest() {
		super(Version.XML_RPC);
	}

	@Override
	public void testValidate011() throws Exception {
		validate(TracTestConstants.TEST_TRAC_011_URL);
	}

	public void testValidateFailNoAuth() throws Exception {
		connect(TracTestConstants.TEST_TRAC_010_URL, "", "");
		try {
			repository.validate();
			fail("Expected TracLoginException");
		} catch (TracPermissionDeniedException e) {
		}
	}

	public void testMulticallExceptions() throws Exception {
		connect010();
		try {
			((TracXmlRpcClient) repository).getTickets(new int[] { 1, Integer.MAX_VALUE });
			fail("Expected TracRemoteException");
		} catch (TracRemoteException e) {
		}
	}

	public void testUpdateAttributes010() throws Exception {
		connect010();
		updateAttributes();
	}

	public void testUpdateAttributes011() throws Exception {
		connect011();
		updateAttributes();
	}

	public void updateAttributes() throws Exception {
		assertNull(repository.getMilestones());
		repository.updateAttributes(new NullProgressMonitor(), true);
		TracVersion[] versions = repository.getVersions();
		assertEquals(2, versions.length);
		Arrays.sort(versions, new Comparator<TracVersion>() {
			public int compare(TracVersion o1, TracVersion o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		assertEquals("1.0", versions[0].getName());
		assertEquals("", versions[0].getDescription());
		assertEquals(new Date(0), versions[0].getTime());
		assertEquals("2.0", versions[1].getName());
		assertEquals("", versions[1].getDescription());
		assertEquals(new Date(0), versions[1].getTime());
	}

	public void testWikiToHtml010() throws Exception {
		connect010();
		wikiToHtml(TracTestConstants.TEST_TRAC_010_URL);
	}

	public void testWikiToHtml011() throws Exception {
		connect011();
		wikiToHtml(TracTestConstants.TEST_TRAC_011_URL);
	}

	public void wikiToHtml(String tracUrl) throws Exception {
		String html = ((TracXmlRpcClient) repository).wikiToHtml("");
		assertEquals("", html);

		html = ((TracXmlRpcClient) repository).wikiToHtml("A simple line of text.");
		assertEquals("<p>\nA simple line of text.\n</p>\n", html);

		String source = "= WikiFormattingTesting =\n" + " * '''bold''', '''!''' can be bold too''', and '''! '''\n"
				+ " * ''italic''\n" + " * '''''bold italic'''''\n" + " * __underline__\n"
				+ " * {{{monospace}}} or `monospace`\n" + " * ~~strike-through~~\n" + " * ^superscript^ \n"
				+ " * ,,subscript,,\n" + "= Heading =\n" + "== Subheading ==\n";

		String expectedHtml = "<h1 id=\"WikiFormattingTesting\"><a class=\"missing wiki\" href=\""
				+ tracUrl
				+ "/wiki/WikiFormattingTesting\" rel=\"nofollow\">WikiFormattingTesting?</a></h1>\n<ul><li><strong>bold</strong>, <strong>\'\'\' can be bold too</strong>, and <strong>! </strong>\n</li><li><i>italic</i>\n</li><li><strong><i>bold italic</i></strong>\n</li><li><span class=\"underline\">underline</span>\n</li><li><tt>monospace</tt> or <tt>monospace</tt>\n</li><li><del>strike-through</del>\n</li><li><sup>superscript</sup> \n</li><li><sub>subscript</sub>\n</li></ul><h1 id=\"Heading\">Heading</h1>\n<h2 id=\"Subheading\">Subheading</h2>\n";

		html = ((TracXmlRpcClient) repository).wikiToHtml(source);
		assertEquals(expectedHtml, html);
	}

	public void testValidateWikiAPI010() throws Exception {
		connect010();
		((TracXmlRpcClient) repository).validateWikiRpcApi();
	}

	public void testValidateWikiAPI011() throws Exception {
		connect011();
		((TracXmlRpcClient) repository).validateWikiRpcApi();
	}

	public void testGetAllWikiPageNames010() throws Exception {
		connect010();
		getAllWikiPageNames();
	}

	public void testGetAllWikiPageNames011() throws Exception {
		connect011();
		getAllWikiPageNames();
	}

	private void getAllWikiPageNames() throws Exception {
		String[] names = ((TracXmlRpcClient) repository).getAllWikiPageNames();
		List<String> all = Arrays.asList(names);
		assertTrue(all.contains("Test"));
	}

	public void testGetWikiPage010() throws Exception {
		connect010();
		getWikiPage();
	}

	public void testGetWikiPage011() throws Exception {
		connect011();
		getWikiPage();
	}

	private void getWikiPage() throws Exception {
		String pageName = "Test";
		String author = "admin@mylar.eclipse.org";
		int version = 1;

		TracWikiPage page = ((TracXmlRpcClient) repository).getWikiPage(pageName);
		assertEquals(pageName, page.getPageInfo().getPageName());
		assertEquals(author, page.getPageInfo().getAuthor());
		assertEquals(version, page.getPageInfo().getVersion());
		// XXX: The Date returned from Wiki API seems to have a problem with the Time Zone
		//String date = "Sat Nov 11 18:10:56 EST 2006";
		//assertEquals(date, page.getPageVersion().getLastModified().toString());
		assertTrue(page.getContent() != null);
		assertTrue(page.getPageHTML().startsWith("<html>"));

		pageName = "WikiStart";
		author = "trac";
		version = 1;

		page = ((TracXmlRpcClient) repository).getWikiPage("WikiStart", 1);
		assertEquals(pageName, page.getPageInfo().getPageName());
		assertEquals(author, page.getPageInfo().getAuthor());
		assertEquals(version, page.getPageInfo().getVersion());
		assertTrue(page.getContent() != null);
		assertTrue(page.getPageHTML().startsWith("<html>"));
	}

	public void testGetWikiPageInvalid010() throws Exception {
		connect010();
		getWikiPageInvalid();
	}

	public void testGetWikiPageInvalid011() throws Exception {
		connect011();
		getWikiPageInvalid();
	}

	private void getWikiPageInvalid() throws Exception {
		// get info -- non-existing version
		try {
			((TracXmlRpcClient) repository).getWikiPageInfo("Test", 10);
			fail("Expected TracRemoteException");
		} catch (TracRemoteException e) {
		}

		// get info -- non-existing page name
		try {
			((TracXmlRpcClient) repository).getWikiPageInfo("NoSuchPage");
			fail("Expected TracRemoteException");
		} catch (TracRemoteException e) {
		}

		// get info -- null parameter
		try {
			((TracXmlRpcClient) repository).getWikiPageInfo(null);
			fail("Expected RuntimeException");
		} catch (IllegalArgumentException e) {
		}

		// get content -- non-existing version
		try {
			((TracXmlRpcClient) repository).getWikiPageContent("Test", 10);
			fail("Expected TracRemoteException");
		} catch (TracRemoteException e) {
		}

		// get content -- non-existing page name
		try {
			((TracXmlRpcClient) repository).getWikiPageContent("NoSuchPage");
			fail("Expected TracRemoteException");
		} catch (TracRemoteException e) {
		}

		// get content -- null parameter
		try {
			((TracXmlRpcClient) repository).getWikiPageContent(null);
			fail("Expected RuntimeException");
		} catch (IllegalArgumentException e) {
		}

		// get HTML -- non-existing version
		try {
			((TracXmlRpcClient) repository).getWikiPageHtml("Test", 10);
			fail("Expected TracRemoteException");
		} catch (TracRemoteException e) {
		}

		// get HTML -- non-existing page name
		try {
			((TracXmlRpcClient) repository).getWikiPageHtml("NoSuchPage");
			fail("Expected TracRemoteException");
		} catch (TracRemoteException e) {
		}

		// get HTML -- null parameter
		try {
			((TracXmlRpcClient) repository).getWikiPageHtml(null);
			fail("Expected RuntimeException");
		} catch (IllegalArgumentException e) {
		}

		// get a page -- non-existing version
		try {
			((TracXmlRpcClient) repository).getWikiPage("Test", 10);
			fail("Expected TracRemoteException");
		} catch (TracRemoteException e) {
		}

		// get a page -- non-existing page name
		try {
			((TracXmlRpcClient) repository).getWikiPage("NoSuchPage");
			fail("Expected TracRemoteException");
		} catch (TracRemoteException e) {
		}

		// get a page -- null parameter
		try {
			((TracXmlRpcClient) repository).getWikiPage(null);
			fail("Expected RuntimeException");
		} catch (IllegalArgumentException e) {
		}

		// get all versions of a page -- non-existing page name
		try {
			((TracXmlRpcClient) repository).getWikiPageInfoAllVersions("NoSuchPage");
			fail("Expected TracRemoteException");
		} catch (TracRemoteException e) {
		}

		// get all versions of a page -- null parameter
		try {
			((TracXmlRpcClient) repository).getWikiPageInfoAllVersions(null);
			fail("Expected RuntimeException");
		} catch (IllegalArgumentException e) {
		}
	}

	public void testGetWikiPageInfoAllVersions010() throws Exception {
		connect010();
		getWikiPageInfoAllVersions();
	}

	public void testGetWikiPageInfoAllVersions011() throws Exception {
		connect011();
		getWikiPageInfoAllVersions();
	}

	private void getWikiPageInfoAllVersions() throws Exception {
		String pageName = "Test";

		TracWikiPageInfo[] versions = ((TracXmlRpcClient) repository).getWikiPageInfoAllVersions(pageName);
		assertTrue(versions.length >= 1);
		int counter = 1;
		for (TracWikiPageInfo version : versions) {
			assertTrue(version.getPageName().equals(pageName));
			assertTrue(version.getVersion() == counter++); // assuming versions are ordered increasingly
		}
	}

	public void testGetRecentWikiChanges010() throws Exception {
		connect010();
		getRecentWikiChanges();
	}

	public void testGetRecentWikiChanges011() throws Exception {
		connect011();
		getRecentWikiChanges();
	}

	private void getRecentWikiChanges() throws Exception {
		TracWikiPageInfo[] changes = ((TracXmlRpcClient) repository).getRecentWikiChanges(new Date(0));
		TracWikiPageInfo testPage = null;
		for (TracWikiPageInfo item : changes) {
			assertTrue(item.getPageName() != null);
			if (item.getPageName().equals("Test")) {
				testPage = item;
			}
		}
		assertTrue(testPage != null);
	}

	public void testPutWikiPage010() throws Exception {
		connect010();
		putWikiPage();
	}

	public void testPutWikiPage011() throws Exception {
		connect011();
		putWikiPage();
	}

	private void putWikiPage() throws Exception {
		// TODO: testing wiki.putPage()
	}
}
