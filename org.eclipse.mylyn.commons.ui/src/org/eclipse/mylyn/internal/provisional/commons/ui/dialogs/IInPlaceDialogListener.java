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
 * Listener interface for close events from an {@link AbstractInPlaceDialog}
 * 
 * @author Shawn Minto
 * @since 3.3
 */
public interface IInPlaceDialogListener {

	public void buttonPressed(InPlaceDialogEvent event);

}