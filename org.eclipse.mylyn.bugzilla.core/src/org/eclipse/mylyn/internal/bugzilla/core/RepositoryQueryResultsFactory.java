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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import org.eclipse.mylyn.tasks.core.ITaskCollector;

/**
 * @author Rob Elves
 */
public class RepositoryQueryResultsFactory extends AbstractReportFactory {

	public RepositoryQueryResultsFactory(InputStream inStream, String encoding) {
		super(inStream, encoding);
	}

	/**
	 * expects rdf returned from repository (ctype=rdf in url)
	 * 
	 * @throws GeneralSecurityException
	 */
	public void performQuery(String repositoryUrl, ITaskCollector collector, int maxHits) throws IOException {
		SaxBugzillaQueryContentHandler contentHandler = new SaxBugzillaQueryContentHandler(repositoryUrl, collector);
		collectResults(contentHandler, false);
	}
}
