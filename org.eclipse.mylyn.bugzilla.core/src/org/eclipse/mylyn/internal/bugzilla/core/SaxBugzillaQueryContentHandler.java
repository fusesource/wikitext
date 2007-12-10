/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.core;

import java.util.Locale;

import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.ITaskCollector;
import org.eclipse.mylyn.web.core.HtmlStreamTokenizer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for RDF bugzilla query results.
 * 
 * @author Rob Elves
 */
public class SaxBugzillaQueryContentHandler extends DefaultHandler {

	/** The bug id */
	private String id;

	/** The summary of the bug */
	private String description = "";

	/** The priority of the bug */
	private String priority = AbstractTask.PriorityLevel.getDefault().toString();

	private String owner = "";
	
	private StringBuffer characters;

	private ITaskCollector collector;

	private String repositoryUrl;

	public SaxBugzillaQueryContentHandler(String repositoryUrl, ITaskCollector collector) {
		this.repositoryUrl = repositoryUrl;
		this.collector = collector;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		characters.append(ch, start, length);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		characters = new StringBuffer();
		BugzillaReportElement tag = BugzillaReportElement.UNKNOWN;
		try {
			tag = BugzillaReportElement.valueOf(localName.trim().toUpperCase(Locale.ENGLISH));
			switch (tag) {
			case LI:
// hit = new BugzillaQueryHit();
// hit.setRepository(repositoryUrl);
// break;
			}
		} catch (RuntimeException e) {
			if (e instanceof IllegalArgumentException) {
				// ignore unrecognized tags
				return;
			}
			throw e;
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		String parsedText = HtmlStreamTokenizer.unescape(characters.toString());

		BugzillaReportElement tag = BugzillaReportElement.UNKNOWN;
		try {
			tag = BugzillaReportElement.valueOf(localName.trim().toUpperCase(Locale.ENGLISH));
			switch (tag) {
			case ID:
				id = parsedText;
				break;
			// case BUG_SEVERITY:
			// severity = parsedText;
			// break;
			case PRIORITY:
				priority = parsedText;
				break;
			// case REP_PLATFORM:
			// platform = parsedText;
			// break;
			case ASSIGNED_TO:
				//hit.setOwner(parsedText);
				owner = parsedText;
				break;
			case BUG_STATUS:
				//	state = parsedText;
				break;
			// case RESOLUTION:
			// resolution = parsedText;
			// break;
			case SHORT_DESC:
				description = parsedText;
				break;
			case SHORT_SHORT_DESC:
				description = parsedText;
				break;
			case LI:
				BugzillaTask task = new BugzillaTask(repositoryUrl, id, description);
				task.setPriority(priority);
				task.setOwner(owner);
				// TODO set state
				collector.accept(task);
			}
		} catch (RuntimeException e) {
			if (e instanceof IllegalArgumentException) {
				// ignore unrecognized tags
				return;
			}
			throw e;
		}

	}
}
