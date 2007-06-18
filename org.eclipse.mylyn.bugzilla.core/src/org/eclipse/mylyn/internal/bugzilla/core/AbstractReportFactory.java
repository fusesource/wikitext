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

package org.eclipse.mylyn.internal.bugzilla.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.security.GeneralSecurityException;

import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Rob Elves
 */
public class AbstractReportFactory {

	public static final int RETURN_ALL_HITS = -1;

	private InputStream inStream;

	private String characterEncoding;

	public AbstractReportFactory(InputStream inStream, String encoding) {
		this.inStream = inStream;
		this.characterEncoding = encoding;
	}

	/**
	 * expects rdf returned from repository (ctype=rdf in url)
	 * 
	 * @throws GeneralSecurityException
	 */
	protected void collectResults(DefaultHandler contentHandler, boolean clean) throws IOException {

		if (inStream == null) {
			return;
		}

		final BufferedInputStream is = new BufferedInputStream(inStream, 1024);

		// filtered upon tasklist and offline taskdata externalization 
//		InputStream iis = new InputStream() {
//			public int read() throws IOException {
//				int c;
//				while ((c = is.read()) != -1) {
//					if (!Character.isISOControl(c) || c == '\n' || c == '\r') {
//						return c;
//					}
//				}
//				return -1;
//			}
//		};

		Reader in;
		if (characterEncoding != null) {
			in = new InputStreamReader(is, characterEncoding);
		} else {
			in = new InputStreamReader(is);
		}

		if (in != null && clean) {
			StringBuffer result = XmlCleaner.clean(in);
			StringReader strReader = new StringReader(result.toString());
			in = new BufferedReader(strReader);
		}

		try {
			final XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(contentHandler);

			EntityResolver resolver = new EntityResolver() {

				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					// The default resolver will try to resolve the dtd via
					// URLConnection. Since we
					// don't have need of entity resolving
					// currently, we just supply a dummy (empty) resource for
					// each request...
					InputSource source = new InputSource();
					source.setCharacterStream(new StringReader(""));
					return source;
				}
			};

			reader.setEntityResolver(resolver);
			reader.setErrorHandler(new ErrorHandler() {

				public void error(SAXParseException exception) throws SAXException {
					throw exception;
				}

				public void fatalError(SAXParseException exception) throws SAXException {
					throw exception;
				}

				public void warning(SAXParseException exception) throws SAXException {
					throw exception;
				}
			});
			reader.parse(new InputSource(in));
		} catch (SAXException e) {
			// if
			// (e.getMessage().equals(IBugzillaConstants.ERROR_INVALID_USERNAME_OR_PASSWORD))
			// {
			// throw new LoginException(e.getMessage());
			// } else {
			throw new IOException(e.getMessage());
			// }
		}
	}

}
