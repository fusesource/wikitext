/*******************************************************************************
 * Copyright (c) 2007, 2009 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *     Fintan Bolton - modified for use in Confdoc plugin
 *******************************************************************************/
package org.eclipse.mylyn.internal.wikitext.confluence.core.block;

import org.eclipse.mylyn.wikitext.core.parser.Attributes;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder.BlockType;

/**
 * @author David Green
 */
public class CodeBlock extends AbstractConfluenceDelimitedBlock {

	private String title;

	private String language;

	public CodeBlock() {
		super("code"); //$NON-NLS-1$
	}

	@Override
	protected void beginBlock() {
		if (title != null) {
			Attributes attributes = new Attributes();
			attributes.setTitle(title);
			builder.beginBlock(BlockType.PANEL, attributes);
		}
		Attributes attributes = new Attributes();
		//Attributes preAttributes = new Attributes();
		if (language != null) {
			// chili-style class and atlassian-style class
			attributes.setCssClass(language + " code-" + language); //$NON-NLS-1$
		}
		// fbolton - Backed out code for enclosing CODE in the PREFORMATTED style.
		// This does not seem to make sense. In DocBook, it would result in a code
		// block always being enclosed in a 'literallayout' element. Why is this useful?
		//builder.beginBlock(BlockType.PREFORMATTED, preAttributes);
		builder.beginBlock(BlockType.CODE, attributes);
		
		// Set a flag that signals to nesting-capable blocks (e.g. TableBlock)
		// that they must not try to parse inside this literal layout block.
		getMarkupLanguage().setCurrentBlockHasLiteralLayout(true);
	}

	@Override
	protected void handleBlockContent(String content) {
		builder.characters(content);
		builder.characters("\n"); //$NON-NLS-1$
	}

	@Override
	protected void endBlock() {
        getMarkupLanguage().setCurrentBlockHasLiteralLayout(false);
        
		if (title != null) {
			builder.endBlock(); // panel	
		}
		builder.endBlock(); // code
		//builder.endBlock(); // pre
	}

	@Override
	protected void resetState() {
		super.resetState();
		title = null;
	}

	@Override
	protected void setOption(String key, String value) {
		if (key.equals("title")) { //$NON-NLS-1$
			title = value;
		}
	}

	@Override
	protected void setOption(String option) {
		language = option.toLowerCase();
	}
}
