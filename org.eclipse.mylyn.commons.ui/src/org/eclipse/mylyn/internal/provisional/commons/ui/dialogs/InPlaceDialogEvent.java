/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.provisional.commons.ui.dialogs;

/**
 * Event sent with an {@link IInPlaceDialogListener} that contains information about the close event that occurred
 * 
 * @author Shawn Minto
 * @since 3.3
 */
public class InPlaceDialogEvent {

	private final int returnCode;

	private final boolean isClosing;

	public InPlaceDialogEvent(int returnCode, boolean isClosing) {
		this.returnCode = returnCode;
		this.isClosing = isClosing;
	}

	public boolean isClosing() {
		return isClosing;
	}

	public int getReturnCode() {
		return returnCode;
	}
}