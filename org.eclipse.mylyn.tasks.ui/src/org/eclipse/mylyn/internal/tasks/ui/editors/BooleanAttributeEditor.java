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

package org.eclipse.mylyn.internal.tasks.ui.editors;

import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @author Steffen Pingel
 */
public class BooleanAttributeEditor extends AbstractAttributeEditor {

	private Button button;

	private boolean ignoreNotification;

	public BooleanAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
		super(manager, taskAttribute);
	}

	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		button = toolkit.createButton(parent, super.getLabel(), SWT.CHECK);
		button.setEnabled(!isReadOnly());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!ignoreNotification) {
					setValue(button.getSelection());
				}
			}
		});
		refresh();
		setControl(button);
	}

	@Override
	public String getLabel() {
		return ""; //$NON-NLS-1$
	}

	public boolean getValue() {
		return getAttributeMapper().getBooleanValue(getTaskAttribute());
	}

	public void setValue(boolean value) {
		getAttributeMapper().setBooleanValue(getTaskAttribute(), value);
		attributeChanged();
	}

	@Override
	public void refresh() {
		if (button == null) {
			return;
		}

		try {
			ignoreNotification = true;
			button.setSelection(getValue());
		} finally {
			ignoreNotification = false;
		}
	}
}
