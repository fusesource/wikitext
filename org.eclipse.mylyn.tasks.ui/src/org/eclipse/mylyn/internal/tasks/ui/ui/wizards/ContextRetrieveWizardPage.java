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

package org.eclipse.mylar.internal.tasks.ui.ui.wizards;

import java.util.Set;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.RepositoryAttachment;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Rob Elves
 * @author Mik Kersten
 */
public class ContextRetrieveWizardPage extends WizardPage {

	private static final String DESCRIPTION = "Loads context from repository task into the workspace";
		
	private static final String COLUMN_COMMENT = "Comment";

	private static final String COLUMN_AUTHOR = "Author";

	private static final String COLUMN_DATE = "Date";

	private TaskRepository repository;

	private AbstractRepositoryTask task;

	private RepositoryAttachment selectedContextAttachment = null;

	protected ContextRetrieveWizardPage(TaskRepository repository, AbstractRepositoryTask task) {
		super(ContextAttachWizard.WIZARD_TITLE);
		this.repository = repository;
		this.task = task;
		setDescription(DESCRIPTION);
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		
		new Label(composite, SWT.NONE).setText("Task: " + task.getDescription());
		new Label(composite, SWT.NONE).setText("Repository: " + repository.getUrl());
		new Label(composite, SWT.NONE).setText("Select context below:");

		final Table contextTable = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER);
		contextTable.setHeaderVisible(true);
		contextTable.setLinesVisible(true);
		contextTable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (contextTable.getSelectionIndex() > -1) {
					selectedContextAttachment = (RepositoryAttachment) contextTable.getItem(contextTable.getSelectionIndex())
							.getData();
					getWizard().getContainer().updateButtons();
				}
			}
		});

		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
				repository.getKind());

		Set<RepositoryAttachment> contextAttachments = connector.getContextAttachments(repository, task);
		TableColumn[] columns = new TableColumn[3];

		columns[0] = new TableColumn(contextTable, SWT.LEFT);
		columns[0].setText(COLUMN_DATE);

		columns[1] = new TableColumn(contextTable, SWT.LEFT);
		columns[1].setText(COLUMN_AUTHOR);

		columns[2] = new TableColumn(contextTable, SWT.CENTER);
		columns[2].setText(COLUMN_COMMENT);

		for (RepositoryAttachment attachment : contextAttachments) {
			TableItem item = new TableItem(contextTable, SWT.NONE);
			item.setText(0, attachment.getDateCreated());
			item.setText(1, attachment.getCreator());
			item.setText(2, attachment.getDescription());
			item.setData(attachment);
		}

	    for (int i = 0, n = columns.length; i < n; i++) {
	      columns[i].pack();
	    }
		
		contextTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		setControl(composite);
	}

	public RepositoryAttachment getSelectedContext() {
		return selectedContextAttachment;
	}

	@Override
	public boolean isPageComplete() {
		if(selectedContextAttachment == null) return false;
		return super.isPageComplete();
	}

}
