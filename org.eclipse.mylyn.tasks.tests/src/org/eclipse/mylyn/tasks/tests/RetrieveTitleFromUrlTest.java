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

package org.eclipse.mylar.tasks.tests;

import java.lang.reflect.InvocationTargetException;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylar.internal.tasks.ui.RetrieveTitleFromUrlJob;

/**
 * @author Mik Kersten
 */
public class RetrieveTitleFromUrlTest extends TestCase {

	// XXX broken due to hang that causes the scheduled job to never complete
	public void testRetrieve() throws InterruptedException, InvocationTargetException {
		final String url = "http://eclipse.org/mylar/index.php";
		final String knownTitle = "Mylar Technology Project";

		RetrieveTitleFromUrlJob job = new RetrieveTitleFromUrlJob(url) {

			@Override
			public void setTitle(String title) {
				assertEquals(knownTitle, title);
			}
		};
		job.run(new NullProgressMonitor());

		assertTrue(job.isTitleRetrieved());
		assertEquals(knownTitle, job.getPageTitle());
	}
}
