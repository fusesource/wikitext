/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.editors;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * @author Steffen Pingel
 */
public class TaskEditorPeoplePart extends AbstractTaskEditorPart {

	private static final int COLUMN_MARGIN = 5;

	public TaskEditorPeoplePart() {
		setPartName("People");
	}

	private void addAttribute(Composite composite, FormToolkit toolkit, TaskAttribute attribute) {
		AbstractAttributeEditor editor = createAttributeEditor(attribute);
		if (editor != null) {
			editor.createLabelControl(composite, toolkit);
			GridDataFactory.defaultsFor(editor.getLabelControl()).indent(COLUMN_MARGIN, 0).applyTo(
					editor.getLabelControl());
			editor.createControl(composite, toolkit);
			getTaskEditorPage().getAttributeEditorToolkit().adapt(editor);
		}
	}

	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		Section section = createSection(parent, toolkit, true);

		Composite peopleComposite = toolkit.createComposite(section);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 5;
		peopleComposite.setLayout(layout);

		addAttribute(peopleComposite, toolkit, getTaskData().getRoot().getAttribute(TaskAttribute.USER_ASSIGNED));
		addAttribute(peopleComposite, toolkit, getTaskData().getRoot().getAttribute(TaskAttribute.USER_REPORTER));
		addAttribute(peopleComposite, toolkit, getTaskData().getRoot().getAttribute(TaskAttribute.ADD_SELF_CC));
		addAttribute(peopleComposite, toolkit, getTaskData().getRoot().getAttribute(TaskAttribute.NEW_CC));

		// TODO EDITOR cc list

		toolkit.paintBordersFor(peopleComposite);
		section.setClient(peopleComposite);
		setSection(toolkit, section);
	}

}
