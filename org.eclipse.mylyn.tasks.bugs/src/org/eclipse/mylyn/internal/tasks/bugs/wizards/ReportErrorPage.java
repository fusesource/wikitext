/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.bugs.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.internal.tasks.bugs.AttributeTaskMapper;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Steffen Pingel
 */
public class ReportErrorPage extends WizardPage {

	private final IStatus status;

	private final AttributeTaskMapper mapper;

	protected TaskRepository taskRepository;

	public ReportErrorPage(AttributeTaskMapper mapper, IStatus status) {
		super("reportError");
		this.mapper = mapper;
		this.status = status;
		setTitle("Report as Bug");
		setMessage("An unexpected error has occured in plug-in " + status.getPlugin() + "");
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));

//		Group errorGroup = new Group(composite, SWT.NONE);
//		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(errorGroup);
//		errorGroup.setText("Details");
//		errorGroup.setLayout(new GridLayout(1, true));

		Label label = new Label(composite, SWT.NONE);
		label.setText("Details:");

		Text text = new Text(composite, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER);
		text.setText(status.getMessage());
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(text);

		// space
		new Label(composite, SWT.NONE);

		if (mapper.isMappingComplete()) {
			final Button defaultRepositoryButton = new Button(composite, SWT.RADIO);
			defaultRepositoryButton.setText("Report to: " + mapper.getTaskRepository().getRepositoryLabel());
			defaultRepositoryButton.setSelection(true);

			final Button selectRepositoryButton = new Button(composite, SWT.RADIO);
			selectRepositoryButton.setText("Select repository");

			defaultRepositoryButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					if (defaultRepositoryButton.getSelection()) {
						selectRepositoryButton.setSelection(false);
					}
					taskRepository = mapper.getTaskRepository();
					getContainer().updateButtons();
				}
			});

			selectRepositoryButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					if (selectRepositoryButton.getSelection()) {
						defaultRepositoryButton.setSelection(false);
					}
					taskRepository = null;
					getContainer().updateButtons();
				}
			});

			taskRepository = mapper.getTaskRepository();
		} else {
			taskRepository = null;
		}
		setControl(composite);
	}

	@Override
	public boolean canFlipToNextPage() {
		return taskRepository == null;
	}

	public TaskRepository getTaskRepository() {
		return taskRepository;
	}

	@Override
	public boolean isPageComplete() {
		return true;
	}

}