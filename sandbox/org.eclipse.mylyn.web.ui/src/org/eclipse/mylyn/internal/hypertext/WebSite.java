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

package org.eclipse.mylar.internal.hypertext;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylar.internal.hypertext.ui.HypertextImages;

/**
 * @author Mik Kersten
 */
public class WebSite extends WebResource {

	private WebRoot project;
	
	private HashMap<String, WebResource> pages = new HashMap<String, WebResource>();
	
	public WebSite(String url, WebRoot project) {
		super(url);
	}

	public void addPage(WebPage page) {
		pages.put(page.getUrl(), page);
	}
	
	public void removePage(WebPage page) {
		pages.remove(page);
	}
	
	@Override
	public WebResource getParent() {
		return project;
	}

	@Override
	public Collection<WebResource> getChildren() {
		return pages.values();
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return HypertextImages.WEB_SITE;
	}

	public WebPage getPage(String url) {
		return (WebPage)pages.get(url);
	}

}
