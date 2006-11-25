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

package org.eclipse.mylar.internal.web;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.mylar.context.core.AbstractRelationProvider;
import org.eclipse.mylar.context.core.IDegreeOfSeparation;
import org.eclipse.mylar.context.core.AbstractContextStructureBridge;
import org.eclipse.swt.browser.LocationEvent;

/**
 * TODO: there is schitzophrenia between content types and protocols
 * 
 * @author Mik Kersten
 */
public class WebResourceStructureBridge extends AbstractContextStructureBridge {

	private static final String DELIM_PROTOCOL = "//";
	
	public static final String CONTENT_TYPE = "http"; 

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

	@Override
	public String getContentType(String elementHandle) {
		return CONTENT_TYPE;
	}

	@Override
	public boolean acceptsObject(Object object) {
		if (object instanceof LocationEvent || object instanceof WebResource || object instanceof URL) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String getHandleIdentifier(Object object) {
		if (object instanceof LocationEvent) {
			return ((LocationEvent) object).location;
		} else if (object instanceof WebResource){
			return ((WebResource)object).getUrl();
		} else if (object instanceof URL) {
			return ((URL)object).toExternalForm();
		} else {
			return null;
		}
	}

	@Override
	public Object getObjectForHandle(String handle) {
		return MylarWebPlugin.getWebResourceManager().find(handle);
	}

	@Override
	public String getParentHandle(String handle) {
		if (handle == null || "".equals(handle)) {
			return null;
		}
		String site = getSite(handle);
		if (site == null) {
			return WebRoot.HANDLE_ROOT;
		}
		return site;
	}

	public String getSite(String url) {
		String site = null;
		int protocolEnd = url.indexOf(DELIM_PROTOCOL) + 2;
		if (protocolEnd != -1) {
			String withoutProtocol = url.substring(protocolEnd);
			int siteEnd = withoutProtocol.indexOf("/");
			if (siteEnd != -1) {
				site = url.substring(0, protocolEnd + siteEnd);
			}
		}
		return site;
	}

	@Override
	public String getName(Object object) {
		return null;
	}

	@Override
	public boolean canBeLandmark(String handle) {
		return getSite(handle) != null;
	}

	@Override
	public boolean canFilter(Object element) {
		return element instanceof WebResource;
	}

	@Override
	public boolean isDocument(String handle) {
		return true;
	}

	@Override
	public String getHandleForOffsetInObject(Object resource, int offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<AbstractRelationProvider> getRelationshipProviders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IDegreeOfSeparation> getDegreesOfSeparation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getChildHandles(String handle) {
		return Collections.emptyList();
	}

}
