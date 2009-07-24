/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.notifications;

import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.swt.graphics.Image;

/**
 * @author Rob Elves
 * @author Mik Kersten
 */
public class TaskListNotificationReminder extends TaskListNotification {

	public TaskListNotificationReminder(AbstractTask task) {
		super(task);
	}

	@Override
	public Image getNotificationKindImage() {
		return CommonImages.getImage(CommonImages.OVERLAY_DATE_DUE);
	}
}
