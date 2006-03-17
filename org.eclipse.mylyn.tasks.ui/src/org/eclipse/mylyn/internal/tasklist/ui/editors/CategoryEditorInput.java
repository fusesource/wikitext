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

package org.eclipse.mylar.internal.tasklist.ui.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylar.provisional.tasklist.AbstractTaskContainer;
import org.eclipse.mylar.provisional.tasklist.MylarTaskListPlugin;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * @author Ken Sueda
 */
public class CategoryEditorInput implements IEditorInput {

	private AbstractTaskContainer category;

	public CategoryEditorInput(AbstractTaskContainer cat) {
		this.category = cat;
	}

	public boolean exists() {
		return true;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return "Category Editor";
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return "Category Editor";
	}

	public Object getAdapter(Class adapter) {
		return null;
	}

	public String getCategoryName() {
		return category.getDescription();
	}

	public void setCategoryName(String description) {
		MylarTaskListPlugin.getTaskListManager().getTaskList().renameContainer(category, description);
//		category.setDescription(description);
	}
}
