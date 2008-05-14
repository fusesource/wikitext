/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.core;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.mylyn.commons.net.WebClientUtil;

/**
 * Use <code>GzipGetMethod</code> instead of {@link GetMethod} to make Mylyn well-behaved when accessing repositories
 * that can supply gzipped responses.<br /> <br>
 * References:
 * <ul>
 * <li><a href="http://www.schroepl.net/projekte/mod_gzip/index.htm">Gzip home</a></li>
 * <li><a href="http://www.oreilly.com/catalog/9780596529307/chapter/ch04.pdf">Gzip site comparison</a></li>
 * </ul>
 * 
 * @see GzipPostMethod, GetMethod
 * 
 * @author Maarten Meijer
 */
public class GzipGetMethod extends GetMethod {
	private final boolean gzipWanted;

	/**
	 * @param requestPath
	 * 		the URI to request
	 * @param gzipWanted
	 * 		is compression desired (for debugging or optionalizing)
	 */
	public GzipGetMethod(String requestPath, boolean gzipWanted) {
		super(requestPath);
		this.gzipWanted = gzipWanted;
	}

	/**
	 * @return true if payload is zipped in any way. Two situations possible:<br />
	 * 	<ul>
	 * 	<li>content-encoding:gzip can be set by a dedicated perl script or mod_gzip</li>
	 * 	<li>content-type: application/x-gzip can be set by any apache after 302 redirect, based on .gz suffix</li>
	 * 	</ul>
	 */
	public boolean isZippedReply() {
		// content-encoding:gzip can be set by a dedicated perl script or mod_gzip
		boolean zipped = (null != this.getResponseHeader("Content-encoding") && this.getResponseHeader(
				"Content-encoding").getValue().equals(WebClientUtil.CONTENT_ENCODING_GZIP))
				||
				// content-type: application/x-gzip can be set by any apache after 302 redirect, based on .gz suffix
				(null != this.getResponseHeader("Content-Type") && this.getResponseHeader("Content-Type")
						.getValue()
						.equals("application/x-gzip"));
		return zipped;
	}

	@Override
	public int execute(HttpState state, HttpConnection conn) throws HttpException, IOException {
		// Insert accept-encoding header
		if (gzipWanted) {
			//this.setRequestHeader("Accept-encoding", WebClientUtil.CONTENT_ENCODING_GZIP);
		}
		int result = super.execute(state, conn);
		return result;
	}

	/**
	 * getResponseBodyNoop is meant for clearing the response body in case of error. The result is never used so no need
	 * to unzip it first.
	 * 
	 * @throws IOException
	 */
	public void getResponseBodyNoop() throws IOException {
		// result is ignored
		super.getResponseBody();
	}

	/**
	 * getResponseBodyAsUnzippedStream checks a usable (decoded if necessary) stream. It checks the headers the headers
	 * and decides accordingly.
	 * 
	 * @return a decoded stream to be used as plain stream.
	 * @throws IOException
	 */
	public InputStream getResponseBodyAsUnzippedStream() throws IOException {
		InputStream input = super.getResponseBodyAsStream();
		if (isZippedReply()) {
			return new java.util.zip.GZIPInputStream(input);
		}
		return input;
	}
}
