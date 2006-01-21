/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.tasklist.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Mik Kersten
 */
public class EnterTaskHandleWizardPage extends WizardPage {

	private static final String TITLE = "Add Existing Task";

	private static final String DESCRIPTION = "Enter the identifier for the task, issue, or bug.";

	private Text taskIdText;

	public EnterTaskHandleWizardPage() {
		super(TITLE);
		setTitle(TITLE);
		setDescription(DESCRIPTION);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;

		GridData gd = new GridData();
		gd.widthHint = 50;

		Label label = new Label(container, SWT.NULL);
		label.setText("Enter ID: ");
		taskIdText = new Text(container, SWT.BORDER);
		taskIdText.setLayoutData(gd);
		taskIdText.setFocus();
		taskIdText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getWizard().getContainer().updateButtons();
//				try {
//					numDaysToReport = Integer.parseInt(taskId.getText());
//					setErrorMessage(null);
//				} catch (Exception ex) {
//					setErrorMessage("Must be integer");
//					numDaysToReport = 0;
//				}
			}
		});
		
		setControl(container);
	}

	public boolean isPageComplete() {
        return getTaskId() != null && !getTaskId().trim().equals("");
    }
	
	public String getTaskId() {
		return taskIdText.getText();
	}
}
