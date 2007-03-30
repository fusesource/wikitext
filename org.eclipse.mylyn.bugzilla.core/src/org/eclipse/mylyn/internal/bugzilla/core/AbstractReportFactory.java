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

package org.eclipse.mylar.internal.bugzilla.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		copyAndCleanByteStream(inStream, byteStream);

		BufferedReader in;
		if (characterEncoding != null) {
			// in = new BufferedReader(new InputStreamReader(inStream,
			// characterEncoding));
			in = new BufferedReader(new StringReader(byteStream.toString(characterEncoding)));
		} else {
			// in = new BufferedReader(new InputStreamReader(inStream));
			in = new BufferedReader(new StringReader(byteStream.toString()));
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

	// Copy and remove control characters other than \n and \r
	private void copyAndCleanByteStream(InputStream in, OutputStream out) throws IOException {
		try {
			if (in != null && out != null) {
				BufferedInputStream inBuffered = new BufferedInputStream(in);

				int bufferSize = 1000;
				byte[] buffer = new byte[bufferSize];

				int readCount;

				BufferedOutputStream outStream = new BufferedOutputStream(out);

				while ((readCount = inBuffered.read(buffer)) != -1) {
					for (int x = 0; x < readCount; x++) {

						if (!Character.isISOControl(buffer[x])) {
							outStream.write(buffer[x]);
						} else if (buffer[x] == '\n' || buffer[x] == '\r') {
							outStream.write(buffer[x]);
						}
					}
					// if (readCount < bufferSize) {
					// outStream.write(buffer, 0, readCount);
					// } else {
					// outStream.write(buffer);
					// }
				}
				outStream.flush();
				outStream.close();

			}
		} finally {
			in.close();
		}

	}

}
