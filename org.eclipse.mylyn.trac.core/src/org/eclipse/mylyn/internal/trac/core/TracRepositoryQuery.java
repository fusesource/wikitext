/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.trac.core;

import org.eclipse.mylyn.internal.trac.core.model.TracSearch;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;

/**
 * @author Steffen Pingel
 */
public class TracRepositoryQuery extends AbstractRepositoryQuery {

	public TracRepositoryQuery(String repositoryUrl, String queryUrl, String description) {
		super(description);

		assert queryUrl.startsWith(repositoryUrl + ITracClient.QUERY_URL);

		setRepositoryUrl(repositoryUrl);
		setUrl(queryUrl);
	}

	@Override
	public String getConnectorKind() {
		return TracCorePlugin.REPOSITORY_KIND;
	}

	public String getQueryParameter() {
		String url = getUrl();
		int i = url.indexOf(ITracClient.QUERY_URL);
		if (i == -1) {
			return null;
		}
		return url.substring(i + ITracClient.QUERY_URL.length());
	}

	/**
	 * Creates a <code>TracSearch</code> object from this query.
	 */
	public TracSearch getTracSearch() {
		TracSearch search = new TracSearch();
		String url = getQueryParameter();
		if (url != null) {
			search.fromUrl(url);
		}
		return search;
	}

}
