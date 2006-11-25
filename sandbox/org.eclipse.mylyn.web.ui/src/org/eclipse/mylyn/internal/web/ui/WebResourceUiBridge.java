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

package org.eclipse.mylar.internal.web.ui;

import java.util.List;

import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.mylar.context.core.ContextCorePlugin;
import org.eclipse.mylar.context.core.IMylarElement;
import org.eclipse.mylar.context.core.AbstractContextStructureBridge;
import org.eclipse.mylar.context.ui.AbstractContextUiBridge;
import org.eclipse.mylar.internal.web.WebPage;
import org.eclipse.mylar.internal.web.WebResource;
import org.eclipse.mylar.internal.web.WebResourceStructureBridge;
import org.eclipse.mylar.internal.web.WebSite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

/**
 * @author Mik Kersten
 */
public class WebResourceUiBridge extends AbstractContextUiBridge {

	@Override
	public void open(IMylarElement element) {
		AbstractContextStructureBridge bridge = ContextCorePlugin.getDefault().getStructureBridge(element.getContentType());
		if (bridge == null) {
			return;
		} else {
			WebResource webResource = (WebResource)bridge.getObjectForHandle(element.getHandleIdentifier());
			if (webResource instanceof WebPage || webResource instanceof WebSite) {
				WebUiUtil.openUrlInInternalBrowser(webResource);
			}
		}
	} 

	@Override
	public void restoreEditor(IMylarElement document) {
		open(document);
	}
	
	@Override
	public void close(IMylarElement node) {
		// ignore
	}

	@Override
	public boolean acceptsEditor(IEditorPart editorPart) {
		return false;
	}

	@Override
	public List<TreeViewer> getContentOutlineViewers(IEditorPart editorPart) {
		return null;
	}

	@Override
	public Object getObjectForTextSelection(TextSelection selection, IEditorPart editor) {
		return null;
	}
	
	@Override
	public IMylarElement getElement(IEditorInput input) {
		return null;
	}

	@Override
	public String getContentType() {
		return WebResourceStructureBridge.CONTENT_TYPE;
	}
}
