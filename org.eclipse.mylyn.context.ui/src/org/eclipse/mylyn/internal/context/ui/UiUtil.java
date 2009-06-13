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

package org.eclipse.mylyn.internal.context.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;

/**
 * @author Mik Kersten
 */
// TODO 3.3 move methods to other class
public class UiUtil {

	public static void initializeViewerSelection(IWorkbenchPart part) {
		ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
		if (selectionProvider != null) {
			ISelection selection = selectionProvider.getSelection();
			try {
				if (selection != null) {
					selectionProvider.setSelection(selection);
				} else {
					selectionProvider.setSelection(StructuredSelection.EMPTY);
				}
			} catch (UnsupportedOperationException e) {
				// ignore if the selection does not support setting a selection, see bug 217634
			}
		}
	}

	public static void displayInterestManipulationFailure() {
		MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
				Messages.UiUtil_Mylyn_Interest_Manipulation, Messages.UiUtil_Not_a_valid_landmark);
	}
}
