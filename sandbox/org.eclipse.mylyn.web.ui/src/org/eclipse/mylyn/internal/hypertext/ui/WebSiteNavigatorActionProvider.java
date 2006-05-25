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

package org.eclipse.mylar.internal.hypertext.ui;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylar.internal.hypertext.WebSiteResource;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

/**
 * @author Mik Kersten
 */
public class WebSiteNavigatorActionProvider extends CommonActionProvider {

	// private ICommonViewerWorkbenchSite viewSite = null;

	private OpenWebResourceAction openAction = new OpenWebResourceAction("Open");

	public WebSiteNavigatorActionProvider() {
		super();
	}

	public void init(ICommonActionExtensionSite extensionSite) {
		 super.init(extensionSite);
		// createActions();
		if (extensionSite.getViewSite() instanceof ICommonViewerWorkbenchSite) {
			// viewSite = (ICommonViewerWorkbenchSite) aConfig.getViewSite();
			// openAction = new OpenFileAction();
			// contribute = true;
			// viewSite.getActionBars().
		}

	}

	public void fillContextMenu(IMenuManager menuManager) {
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();

		openAction.selectionChanged(selection);
		if (openAction.isEnabled()) {
			menuManager.insertAfter(ICommonMenuConstants.GROUP_OPEN, openAction);
		}
		menuManager.add(new Separator(ICommonMenuConstants.GROUP_ADDITIONS));
		// addOpenWithMenu(menuManager);
	}

	public void fillActionBars(IActionBars actionBars) {
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		if (selection.size() == 1 && selection.getFirstElement() instanceof WebSiteResource) {
			openAction.selectionChanged(selection);
			actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
		}
	}

	// private void addOpenWithMenu(IMenuManager aMenu) {
	// IStructuredSelection ss = (IStructuredSelection)
	// getContext().getSelection();
	//
	// if (ss == null || ss.size() != 1) {
	// return;
	// }
	//
	// Object o = ss.getFirstElement();
	//
	// // first try IResource
	// IAdaptable openable = (IAdaptable) AdaptabilityUtility.getAdapter(o,
	// IResource.class);
	// // otherwise try ResourceMapping
	// if (openable == null) {
	// openable = (IAdaptable) AdaptabilityUtility.getAdapter(o,
	// ResourceMapping.class);
	// } else if (((IResource) openable).getType() != IResource.FILE) {
	// openable = null;
	// }
	//
	// if (openable != null) {
	// // Create a menu flyout.
	// IMenuManager submenu = new
	// MenuManager(WorkbenchNavigatorMessages.OpenActionProvider_OpenWithMenu_label,
	// ICommonMenuConstants.GROUP_OPEN_WITH);
	// submenu.add(new GroupMarker(ICommonMenuConstants.GROUP_TOP));
	// submenu.add(new OpenWithMenu(viewSite.getPage(), openable));
	// submenu.add(new GroupMarker(ICommonMenuConstants.GROUP_ADDITIONS));
	//
	// // Add the submenu.
	// if (submenu.getItems().length > 2 && submenu.isEnabled()) {
	// aMenu.appendToGroup(ICommonMenuConstants.GROUP_OPEN_WITH, submenu);
	// }
	// }
	// }

}
