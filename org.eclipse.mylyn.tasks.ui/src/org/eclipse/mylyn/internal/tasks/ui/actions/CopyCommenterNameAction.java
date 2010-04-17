/*******************************************************************************
 * Copyright (c) 2010 Frank Becker and others. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.tasks.ui.util.ClipboardCopier;
import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.ITaskComment;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * @author Frank Becker
 */
public class CopyCommenterNameAction extends BaseSelectionListenerAction {

	public CopyCommenterNameAction() {
		super(Messages.CopyCommenterNameAction_Copy_User_Name);
		setToolTipText(Messages.CopyCommenterNameAction_Copy_User_Name_Tooltip);
		setImageDescriptor(CommonImages.COPY);
	}

	@Override
	public void run() {
		ClipboardCopier.getDefault().copy(getStructuredSelection(), new ClipboardCopier.TextProvider() {
			public String getTextForElement(Object element) {
				if (element instanceof ITaskComment) {
					ITaskComment comment = (ITaskComment) element;
					IRepositoryPerson author = comment.getAuthor();
					if (author != null) {
						return author.getName();
					}
				}
				return null;
			}
		});
	}

}
