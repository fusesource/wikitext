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
package org.eclipse.mylyn.internal.wikitext.core.util;

import java.util.TreeSet;

import org.eclipse.mylyn.wikitext.core.WikiTextPlugin;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.core.util.ServiceLocator;

/**
 * A service locator that uses the {@link WikiTextPlugin} to resolve markup languages
 *
 * @author David Green
 */
public class EclipseServiceLocator extends ServiceLocator {

	public EclipseServiceLocator(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	public MarkupLanguage getMarkupLanguage(String languageName) throws IllegalArgumentException {
		if (languageName == null) {
			throw new IllegalArgumentException();
		}
		MarkupLanguage markupLanguage = WikiTextPlugin.getDefault().getMarkupLanguage(languageName);
		if (markupLanguage == null) {
			try {
				// dispatch to super in case we've been given a fully qualified class name
				markupLanguage = super.getMarkupLanguage(languageName);
			} catch (IllegalArgumentException e) {
				// specified language not found.
				// create a useful error message
				StringBuilder buf = new StringBuilder();
				for (String name: new TreeSet<String>(WikiTextPlugin.getDefault().getMarkupLanguageNames())) {
					if (buf.length() != 0) {
						buf.append(", ");
					}
					buf.append('\'');
					buf.append(name);
					buf.append('\'');
				}
				throw new IllegalArgumentException(String.format("No parser available for markup language '%s'. %s",languageName,buf.length()==0?"There are no parsers available.  Check your eclipse configuration.":"Known markup languages are "+buf));
			}
		}
		return markupLanguage;
	}

}