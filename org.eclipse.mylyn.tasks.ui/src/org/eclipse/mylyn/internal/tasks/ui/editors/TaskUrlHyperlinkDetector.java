/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.editors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;

/**
 * Source from {@link org.eclipse.jface.text.hyperlink.URLHyperlinkDetector}. Returns hyperlinks that use
 * {@link TasksUiUtil} to open urls.
 * 
 * @author Rob Elves
 */
public class TaskUrlHyperlinkDetector extends AbstractHyperlinkDetector {

	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		if (region == null || textViewer == null) {
			return null;
		}

		IDocument document = textViewer.getDocument();

		int offset = region.getOffset();
		if (document == null) {
			return null;
		}

		IRegion lineInfo;
		String line;
		try {
			lineInfo = document.getLineInformationOfOffset(offset);
			line = document.get(lineInfo.getOffset(), lineInfo.getLength());
		} catch (BadLocationException ex) {
			return null;
		}

		int offsetInLine = offset - lineInfo.getOffset();

		return findHyperlinks(line, offsetInLine, lineInfo.getOffset());
	}

	public IHyperlink[] findHyperlinks(String line, int offsetInLine, int offset) {
		char doubleChar = ' ';

		String urlString = null;
		boolean startDoubleQuote = false;
		int urlOffsetInLine = 0;
		int urlLength = 0;

		int urlSeparatorOffset = line.indexOf("://"); //$NON-NLS-1$
		while (urlSeparatorOffset >= 0) {

			// URL protocol (left to "://")
			urlOffsetInLine = urlSeparatorOffset;
			char ch;
			do {
				urlOffsetInLine--;
				ch = ' ';
				if (urlOffsetInLine > -1) {
					ch = line.charAt(urlOffsetInLine);
				}
			} while (Character.isUnicodeIdentifierStart(ch));
			urlOffsetInLine++;

			switch (ch) {
			case '"':
				doubleChar = '"';
				break;
			case '\'':
				doubleChar = '\'';
				break;
			case '[':
				doubleChar = ']';
				break;
			case '(':
				doubleChar = ')';
				break;
			case '{':
				doubleChar = '}';
				break;

			default:
				doubleChar = ' ';
				break;
			}
			startDoubleQuote = doubleChar != ' ';

			// Right to "://"
			StringTokenizer tokenizer = new StringTokenizer(line.substring(urlSeparatorOffset + 3),
					" \t\n\r\f<>", false); //$NON-NLS-1$
			if (!tokenizer.hasMoreTokens()) {
				return null;
			}

			urlLength = tokenizer.nextToken().length() + 3 + urlSeparatorOffset - urlOffsetInLine;
			if (offsetInLine >= urlOffsetInLine && offsetInLine <= urlOffsetInLine + urlLength) {
				break;
			}

			urlSeparatorOffset = line.indexOf("://", urlSeparatorOffset + 1); //$NON-NLS-1$
		}

		if (urlSeparatorOffset < 0) {
			return null;
		}

		if (startDoubleQuote) {
			int endOffset = -1;
			int nextDoubleQuote = line.indexOf(doubleChar, urlOffsetInLine);
			int nextWhitespace = line.indexOf(' ', urlOffsetInLine);
			if (nextDoubleQuote != -1 && nextWhitespace != -1) {
				endOffset = Math.min(nextDoubleQuote, nextWhitespace);
			} else if (nextDoubleQuote != -1) {
				endOffset = nextDoubleQuote;
			} else if (nextWhitespace != -1) {
				endOffset = nextWhitespace;
			}
			if (endOffset != -1) {
				urlLength = endOffset - urlOffsetInLine;
			}
		}

		// Set and validate URL string
		try {
			char lastChar = line.charAt(urlOffsetInLine + urlLength - 1);
			if (lastChar == ',' || lastChar == '.') {
				urlLength--;
			}
			urlString = line.substring(urlOffsetInLine, urlOffsetInLine + urlLength);
			new URL(urlString);
		} catch (MalformedURLException ex) {
			urlString = null;
			return null;
		}

		IRegion urlRegion = new Region(offset + urlOffsetInLine, urlLength);
		return new IHyperlink[] { new TaskUrlHyperlink(urlRegion, urlString) };
	}

}
