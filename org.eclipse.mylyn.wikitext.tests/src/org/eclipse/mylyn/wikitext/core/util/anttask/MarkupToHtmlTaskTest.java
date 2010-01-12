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

package org.eclipse.mylyn.wikitext.core.util.anttask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.eclipse.mylyn.wikitext.tests.TestUtil;

public class MarkupToHtmlTaskTest extends AbstractTestAntTask {

	protected MarkupToHtmlTask task;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		task = createTask();
		task.setFormatOutput(true);
		task.setMarkupLanguage(languageName);
	}

	protected MarkupToHtmlTask createTask() {
		return new MarkupToHtmlTask();
	}

	public void testSimpleOutput() throws IOException {
		File markup = createSimpleTextileMarkup();
		task.setFile(markup);
		task.execute();

		listFiles();

		File htmlFile = new File(markup.getParentFile(), "markup.html");
		assertTrue(htmlFile.exists() && htmlFile.isFile());

		String content = getContent(htmlFile);
//		TestUtil.println(content);

		assertTrue(content.contains("<html"));
		assertTrue(content.contains("</html>"));
		assertTrue(content.contains("<title>markup</title>"));
		assertTrue(content.contains("<body>"));
		assertTrue(content.contains("</body>"));
	}

	public void testSimpleOutputStrictXHTML() throws IOException {
		File markup = createSimpleTextileMarkupWithImage();
		task.setFile(markup);
		task.setXhtmlStrict(true);
		task.execute();

		listFiles();

		File htmlFile = new File(markup.getParentFile(), "markup.html");
		assertTrue(htmlFile.exists() && htmlFile.isFile());

		String content = getContent(htmlFile);
		TestUtil.println(content);

		// verify that alt is present on img tag.
		assertTrue(Pattern.compile("<img.*?alt=\"\"").matcher(content).find());
	}

	public void testSimpleOutputAlternateTitle() throws IOException {
		File markup = createSimpleTextileMarkup();
		task.setFile(markup);
		task.setTitle("Alternate Title");
		task.execute();

		listFiles();

		File htmlFile = new File(markup.getParentFile(), "markup.html");
		assertTrue(htmlFile.exists() && htmlFile.isFile());

		String content = getContent(htmlFile);
//		TestUtil.println(content);

		assertTrue(content.contains("<html"));
		assertTrue(content.contains("</html>"));
		assertTrue(content.contains("<title>Alternate Title</title>"));
		assertTrue(content.contains("<body>"));
		assertTrue(content.contains("</body>"));
	}

	public void testMultipleFiles() throws IOException {
		File markup = createSimpleTextileMarkup();
		task.setFile(markup);
		task.setMultipleOutputFiles(true);
		task.execute();

		listFiles();

		File htmlFile = new File(markup.getParentFile(), "markup.html");
		assertTrue(htmlFile.exists() && htmlFile.isFile());

		String content = getContent(htmlFile);
//		TestUtil.println(content);

		assertTrue(content.contains("<html"));
		assertTrue(content.contains("</html>"));
		assertTrue(content.contains("<title>markup</title>"));
		assertTrue(content.contains("<body>"));
		assertTrue(content.contains("</body>"));
		assertTrue(content.contains("<a href=\"Second-Heading.html\" title=\"Second Heading\">Next</a>"));
		assertTrue(Pattern.compile("<td[^>]*>Second Heading</td>").matcher(content).find());

		File htmlFile2 = new File(markup.getParentFile(), "Second-Heading.html");
		assertTrue(htmlFile2.exists());

		String content2 = getContent(htmlFile2);
//		TestUtil.println(content2);

		assertTrue(content2.contains("<html"));
		assertTrue(content2.contains("</html>"));
		assertTrue(content2.contains("<title>markup - Second Heading</title>"));
		assertTrue(content2.contains("<body>"));
		assertTrue(content2.contains("</body>"));
		assertTrue(content2.contains("<a href=\"markup.html\" title=\"First Heading\">Previous</a>"));
		assertTrue(Pattern.compile("<td[^>]*>First Heading</td>").matcher(content2).find());
	}

	protected File createSimpleTextileMarkup() throws IOException {
		File markupFile = new File(tempFolder, "markup.textile");
		PrintWriter writer = new PrintWriter(new FileWriter(markupFile));
		try {
			writer.println("h1. First Heading");
			writer.println();
			writer.println("some content");
			writer.println();
			writer.println("h1. Second Heading");
			writer.println();
			writer.println("some more content");
		} finally {
			writer.close();
		}
		return markupFile;
	}

	protected File createSimpleTextileMarkupWithImage() throws IOException {
		File markupFile = new File(tempFolder, "markup.textile");
		PrintWriter writer = new PrintWriter(new FileWriter(markupFile));
		try {
			writer.println("some content with !image.png! an image");
		} finally {
			writer.close();
		}
		return markupFile;
	}

	public void testTaskdef() {
		ResourceBundle bundle = ResourceBundle.getBundle(MarkupToHtmlTask.class.getPackage().getName() + ".tasks");
		assertEquals(MarkupToHtmlTask.class.getName(), bundle.getString("wikitext-to-html"));
	}
}
