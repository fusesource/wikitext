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

package org.eclipse.mylyn.internal.provisional.commons.ui;

import java.util.Date;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;

/**
 * @author Rob Elves
 * @author Mik Kersten
 */
public abstract class AbstractNotification implements Comparable<AbstractNotification>, IAdaptable {

	public abstract void open();

	public abstract String getDescription();

	public abstract String getLabel();

	public abstract Image getNotificationImage();

	public abstract Image getNotificationKindImage();

	public abstract Date getDate();

	public abstract void setDate(Date date);

	public Object getToken() {
		return null;
	}

}
