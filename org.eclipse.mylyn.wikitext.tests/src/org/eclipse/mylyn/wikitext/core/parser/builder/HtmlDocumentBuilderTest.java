/*******************************************************************************
 * Copyright (c) 2007, 2010 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.wikitext.core.parser.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.eclipse.mylyn.wikitext.core.parser.Attributes;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder.BlockType;
import org.eclipse.mylyn.wikitext.core.parser.LinkAttributes;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder.Stylesheet;
import org.eclipse.mylyn.wikitext.tests.TestUtil;
import org.eclipse.mylyn.wikitext.textile.core.TextileLanguage;

/**
 * @author David Green
 */
public class HtmlDocumentBuilderTest extends TestCase {

	private MarkupParser parser;

	private StringWriter out;

	private HtmlDocumentBuilder builder;

	private final Map<File, URL> fileToUrl = new HashMap<File, URL>();

	@Override
	public void setUp() {
		parser = new MarkupParser();
		parser.setMarkupLanguage(new TextileLanguage());
		out = new StringWriter();
		builder = new HtmlDocumentBuilder(out) {
			@Override
			protected void checkFileReadable(File file) {
				if (!fileToUrl.containsKey(file)) {
					super.checkFileReadable(file);
				}
			}

			@Override
			protected Reader getReader(File inputFile) throws FileNotFoundException {
				URL url = fileToUrl.get(inputFile);
				if (url != null) {
					try {
						return new InputStreamReader(url.openStream());
					} catch (IOException e) {
						throw new FileNotFoundException(String.format("%s (%s)", inputFile, url));
					}
				}
				return super.getReader(inputFile);
			}
		};
		parser.setBuilder(builder);
	}

	public void testRelativeUrlWithBase() throws URISyntaxException {
		builder.setBase(new URI("http://www.foo.bar/baz"));
		parser.parse("\"An URL\":foo/bar.html");
		String html = out.toString();
		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<a href=\"http://www.foo.bar/baz/foo/bar.html\">An URL</a>"));
	}

	public void testAbsoluteUrlWithBase() throws URISyntaxException {
		builder.setBase(new URI("http://www.foo.bar/baz"));
		parser.parse("\"An URL\":http://www.baz.ca/foo/bar.html");
		String html = out.toString();
		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<a href=\"http://www.baz.ca/foo/bar.html\">An URL</a>"));
	}

	public void testRelativeUrlWithFileBase() throws URISyntaxException {
		final File file = new File("/base/2/with space/");
		builder.setBase(file.toURI());
		parser.parse("\"An URL\":foo/bar.html");
		String html = out.toString();
		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<a href=\"file:/base/2/with%20space/foo/bar.html\">An URL</a>")
				|| html.contains("<a href=\"file:/C:/base/2/with%20space/foo/bar.html\">An URL</a>"));
	}

	public void testNoGratuitousWhitespace() {
		builder.beginDocument();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		builder.characters("some para text");
		builder.lineBreak();
		builder.characters("more para text");
		builder.endBlock();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		builder.characters("second para");
		builder.endBlock();
		builder.endDocument();

		String html = out.toString();

		TestUtil.println(html);

		assertTrue(html.indexOf('\r') == -1);
		assertTrue(html.indexOf('\n') == -1);
		assertEquals(
				"<?xml version='1.0' encoding='utf-8' ?><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head><body><p>some para text<br/>more para text</p><p>second para</p></body></html>",
				html);
	}

	public void testCssStylesheetAsLink() {
		builder.addCssStylesheet(new Stylesheet("styles/test.css"));
		builder.beginDocument();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		builder.characters("some para text");
		builder.endBlock();
		builder.endDocument();

		String html = out.toString();
		TestUtil.println(html);

		assertTrue(html.contains("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><link type=\"text/css\" rel=\"stylesheet\" href=\"styles/test.css\"/></head>"));
	}

	public void testCssStylesheetEmbedded() throws Exception {
		URL cssResource = HtmlDocumentBuilderTest.class.getResource("resources/test.css");
		File cssFile = new File(cssResource.toURI().getPath());

		fileToUrl.put(cssFile, cssResource);

		TestUtil.println("loading css: " + cssFile);

		builder.addCssStylesheet(new Stylesheet(cssFile));
		builder.beginDocument();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		builder.characters("some para text");
		builder.endBlock();
		builder.endDocument();

		String html = out.toString();
		TestUtil.println(html);

		html = html.replace("&#xd;", "");

		assertTrue(Pattern.compile(
				"<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><style type=\"text/css\">\\s*body\\s+\\{\\s+background-image: test-content.png;\\s+\\}\\s*</style></head>",
				Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testDefaultTargetForExternalLinks() throws Exception {
		builder.setDefaultAbsoluteLinkTarget("_external");
		builder.beginDocument();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		builder.link("http://www.example.com", "test");
		builder.endBlock();
		builder.endDocument();

		String html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<a href=\"http://www.example.com\" target=\"_external\">test</a>"));
	}

	public void testDefaultTargetForExternalLinks2() throws Exception {
		builder.setBase(new URI("http://www.notexample.com"));
		builder.setDefaultAbsoluteLinkTarget("_external");
		builder.beginDocument();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		builder.link("http://www.example.com", "test");
		builder.endBlock();
		builder.endDocument();

		String html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<a href=\"http://www.example.com\" target=\"_external\">test</a>"));
	}

	public void testDefaultTargetForInternalLinks() throws Exception {
		builder.setDefaultAbsoluteLinkTarget("_external");
		builder.beginDocument();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		builder.link("foo", "test");
		builder.endBlock();
		builder.endDocument();

		String html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<a href=\"foo\">test</a>"));
	}

	public void testDefaultTargetForInternalLinks2() throws Exception {
		builder.setBase(new URI("http://www.example.com/"));
		builder.setDefaultAbsoluteLinkTarget("_external");
		builder.beginDocument();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		builder.link("http://www.example.com/foo.html", "test");
		builder.endBlock();
		builder.endDocument();

		String html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<a href=\"http://www.example.com/foo.html\">test</a>"));
	}

	public void testSuppressInlineStyles() throws Exception {
		StringWriter out = new StringWriter();
		HtmlDocumentBuilder builder = new HtmlDocumentBuilder(out);
		builder.setUseInlineStyles(false);
		builder.beginDocument();
		builder.beginBlock(BlockType.NOTE, new Attributes());
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		builder.characters("foo");
		builder.endBlock();
		builder.endBlock();
		builder.endDocument();

		String html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<body><div class=\"note\"><p>foo</p></div></body>"));
		assertTrue(html.contains("<style type=\"text/css\">"));
	}

	public void testSuppressBuiltInlineStyles() throws Exception {
		StringWriter out = new StringWriter();
		HtmlDocumentBuilder builder = new HtmlDocumentBuilder(out);
		builder.setSuppressBuiltInStyles(true);
		builder.beginDocument();
		builder.beginBlock(BlockType.NOTE, new Attributes());
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		builder.characters("foo");
		builder.endBlock();
		builder.endBlock();
		builder.endDocument();

		String html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<body><div class=\"note\"><p>foo</p></div></body>"));
		assertTrue(!html.contains("<style type=\"text/css\">"));
	}

	public void testLinkRel() throws Exception {
		// link-specific rel
		StringWriter out = new StringWriter();
		HtmlDocumentBuilder builder = new HtmlDocumentBuilder(out);
		builder.beginDocument();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		LinkAttributes attributes = new LinkAttributes();
		attributes.setRel("nofollow");
		builder.link(attributes, "http://www.foo.bar", "Foo Bar");
		builder.endBlock();
		builder.endDocument();

		String html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<a href=\"http://www.foo.bar\" rel=\"nofollow\">Foo Bar</a>"));

		// default link rel
		out = new StringWriter();
		builder = new HtmlDocumentBuilder(out);
		builder.setLinkRel("nofollow");
		builder.beginDocument();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		attributes = new LinkAttributes();
		builder.link(attributes, "http://www.foo.bar", "Foo Bar");
		builder.endBlock();
		builder.endDocument();

		html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<a href=\"http://www.foo.bar\" rel=\"nofollow\">Foo Bar</a>"));

		// both link-specific and default link ref
		out = new StringWriter();
		builder = new HtmlDocumentBuilder(out);
		builder.setLinkRel("nofollow");
		builder.beginDocument();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		attributes = new LinkAttributes();
		attributes.setRel("foobar");
		builder.link(attributes, "http://www.foo.bar", "Foo Bar");
		builder.endBlock();
		builder.endDocument();

		html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<a href=\"http://www.foo.bar\" rel=\"foobar nofollow\">Foo Bar</a>"));

		// no rel at all
		out = new StringWriter();
		builder = new HtmlDocumentBuilder(out);
		builder.beginDocument();
		builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
		attributes = new LinkAttributes();
		builder.link(attributes, "http://www.foo.bar", "Foo Bar");
		builder.endBlock();
		builder.endDocument();

		html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<a href=\"http://www.foo.bar\">Foo Bar</a>"));
	}

	public void testStylesheetWithAttributes() {
		Stylesheet stylesheet = new Stylesheet("a/test.css");
		stylesheet.getAttributes().put("foo", "bar");
		builder.addCssStylesheet(stylesheet);

		builder.beginDocument();
		builder.endDocument();

		String html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<link type=\"text/css\" rel=\"stylesheet\" href=\"a/test.css\" foo=\"bar\"/>"));
	}

	public void testStylesheetWithNoAttributes() {
		builder.addCssStylesheet(new Stylesheet("a/test.css"));

		builder.beginDocument();
		builder.endDocument();

		String html = out.toString();

		TestUtil.println(html);

		assertTrue(html.contains("<link type=\"text/css\" rel=\"stylesheet\" href=\"a/test.css\"/>"));
	}
}
