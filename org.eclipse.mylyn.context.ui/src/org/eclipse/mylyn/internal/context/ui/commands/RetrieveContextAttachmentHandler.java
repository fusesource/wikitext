/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.context.ui.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.mylyn.tasks.core.ITaskAttachment;

/**
 * @author Steffen Pingel
 */
public class RetrieveContextAttachmentHandler extends AbstractTaskAttachmentHandler {

	@Override
	protected void execute(ExecutionEvent event, ITaskAttachment attachment) {
		// FIXME implement
//		ContextUiUtil.downloadContext(attachment.getTask(), attachment, PlatformUI.getWorkbench()
//				.getProgressService());
	}

}
