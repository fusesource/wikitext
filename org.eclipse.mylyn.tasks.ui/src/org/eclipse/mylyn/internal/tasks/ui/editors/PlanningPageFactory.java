/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.editors;

import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPageFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.FormPage;

public class PlanningPageFactory extends AbstractTaskEditorPageFactory {

	@Override
	public boolean canCreatePageFor(TaskEditorInput input) {
		return true;
	}

	@Override
	public FormPage createPage(TaskEditor parentEditor) {
		return new TaskPlanningEditor(parentEditor);
	}

	@Override
	public Image getPageImage() {
		return CommonImages.getImage(CommonImages.CALENDAR_SMALL);
	}

	@Override
	public String getPageText() {
		return "Planning";
	}

	@Override
	public int getPriority() {
		return PRIORITY_PLANNING;
	}

}
