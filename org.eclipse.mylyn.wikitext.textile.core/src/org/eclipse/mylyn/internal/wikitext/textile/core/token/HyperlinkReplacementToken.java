/*******************************************************************************
 * Copyright (c) 2007, 2008 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.internal.wikitext.textile.core.token;

import org.eclipse.mylyn.internal.wikitext.textile.core.TextileContentState;
import org.eclipse.mylyn.wikitext.core.parser.ImageAttributes;
import org.eclipse.mylyn.wikitext.core.parser.markup.PatternBasedElement;
import org.eclipse.mylyn.wikitext.core.parser.markup.PatternBasedElementProcessor;

/**
 * 
 * 
 * @author David Green
 */
public class HyperlinkReplacementToken extends PatternBasedElement {

	@Override
	protected String getPattern(int groupOffset) {
		return "(?:(\"|\\!)([^\"\\!]+)\\" + (1 + groupOffset) + ":([^\\s]*[^\\s!.)(,:;]))"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected int getPatternGroupCount() {
		return 3;
	}

	@Override
	protected PatternBasedElementProcessor newProcessor() {
		return new HyperlinkReplacementTokenProcessor();
	}

	private static class HyperlinkReplacementTokenProcessor extends PatternBasedElementProcessor {
		@Override
		public void emit() {
			String hyperlinkBoundaryText = group(1);
			String hyperlinkSrc = group(2);
			String href = group(3);
			String namedLinkUrl = ((TextileContentState) getState()).getNamedLinkUrl(href);
			if (namedLinkUrl != null) {
				href = namedLinkUrl;
			}

			if (hyperlinkBoundaryText.equals("\"")) { //$NON-NLS-1$
				builder.link(href, hyperlinkSrc);
			} else {
				builder.imageLink(new ImageAttributes(), href, hyperlinkSrc);
			}
		}
	}

}
