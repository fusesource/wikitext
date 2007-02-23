/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.trac.tests;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.mylar.internal.trac.core.ITracClient.Version;
import org.eclipse.mylar.internal.trac.core.model.TracSearch;
import org.eclipse.mylar.internal.trac.core.model.TracTicket;

/**
 * @author Steffen Pingel
 */
public class TracWebClientSearchTest extends AbstractTracClientSearchTest {

	public TracWebClientSearchTest() {
		super(Version.TRAC_0_9);
	}

	// TODO move this test to AbstracTracClientTest when bug 162094 is resolved
	public void testSearchMilestoneAmpersand010() throws Exception {
		connect010();
		searchMilestoneAmpersand();
	}

	public void testSearchMilestoneAmpersand011() throws Exception {
		connect011();
		searchMilestoneAmpersand();
	}

	private void searchMilestoneAmpersand() throws Exception {
		TracSearch search = new TracSearch();
		search.addFilter("milestone", "mile&stone");
		search.setOrderBy("id");
		List<TracTicket> result = new ArrayList<TracTicket>();
		repository.search(search, result);
		assertEquals(1, result.size());
		assertTicketEquals(tickets.get(7), result.get(0));
	}

}
