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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylar.internal.web.WebResource;
import org.eclipse.mylar.tasks.ui.TasksUiUtil;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * @author Mik Kersten
 */
public class OpenWebResourceAction extends BaseSelectionListenerAction {

	protected OpenWebResourceAction(String text) {
		super(text);
	}
	
	@Override
	public void run() {
		IStructuredSelection selection = super.getStructuredSelection();
		Object selectedElement = selection.getFirstElement();
		if (selectedElement instanceof WebResource) {
			TasksUiUtil.openUrl(((WebResource)selectedElement).getUrl(), true);
		}
	}
}
