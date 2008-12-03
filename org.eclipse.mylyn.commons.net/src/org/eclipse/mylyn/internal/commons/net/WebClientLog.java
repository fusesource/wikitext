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

package org.eclipse.mylyn.internal.commons.net;

import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.commons.logging.impl.SimpleLog;

/**
 * @deprecated Do not use. This class is pending for removal: see bug 237552.
 */
@Deprecated
public class WebClientLog extends SimpleLog {

	private static final long serialVersionUID = -8631869110301753325L;

	private static OutputStream logOutputStream = System.err;

	private static boolean loggingEnabled = false;

	/**
	 * @since 2.0
	 */
	public WebClientLog(String name) {
		super(name);
		setLevel(LOG_LEVEL_ALL);
	}

	/**
	 * @since 3.0
	 */
	public static void initCommonsLoggingSettings() {
		// remove?
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog"); //$NON-NLS-1$ //$NON-NLS-2$
		System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "off"); //$NON-NLS-1$ //$NON-NLS-2$
		System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "off"); //$NON-NLS-1$ //$NON-NLS-2$
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "off"); //$NON-NLS-1$ //$NON-NLS-2$

		// FIXME this does not work with the commons logging Orbit bundle which does not see the WebClientLog class
		// Update our assigned logger to use custom WebClientLog
//		LogFactory logFactory = LogFactory.getFactory();
//		logFactory.setAttribute("org.apache.commons.logging.Log", "org.eclipse.mylyn.web.core.WebClientLog");
		// Note: level being set by Web
		// logFactory.setAttribute("org.apache.commons.logging.simplelog.showdatetime",
		// "true");
		// logFactory.setAttribute("org.apache.commons.logging.simplelog.log.httpclient.wire",
		// "debug");
		// logFactory.setAttribute("org.apache.commons.logging.simplelog.log.httpclient.wire.header",
		// "debug");
		// logFactory.setAttribute("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient",
		// "debug");
		// logFactory.setAttribute(
		// "org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient.HttpConnection",
		// "trace");
//		logFactory.release();
	}

	/**
	 * @since 2.0
	 */
	@Override
	protected void write(StringBuffer buffer) {
		if (WebClientLog.isLoggingEnabled()) {
			OutputStream out = WebClientLog.getLogStream();
			new PrintStream(out).println(buffer);
		}
	}

	/**
	 * @since 3.0
	 */
	public static OutputStream getLogStream() {
		return logOutputStream;
	}

	/**
	 * @since 3.0
	 */
	public static void setLogStream(OutputStream stream) {
		logOutputStream = stream;
	}

	/**
	 * @since 3.0
	 */
	public static void setLoggingEnabled(boolean enabled) {
		loggingEnabled = enabled;
	}

	/**
	 * @since 3.0
	 */
	public static boolean isLoggingEnabled() {
		return loggingEnabled;
	}
}
