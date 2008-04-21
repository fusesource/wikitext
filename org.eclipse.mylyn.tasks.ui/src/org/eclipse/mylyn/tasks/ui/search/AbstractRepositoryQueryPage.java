/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.ui.search;

import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryManager;
import org.eclipse.mylyn.internal.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTaskCategory;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractEditQueryWizard;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Extend to provide repository-specific query page to the Workbench search dialog.
 * 
 * @author Rob Elves
 * @since 2.0
 */
public abstract class AbstractRepositoryQueryPage extends WizardPage implements ISearchPage {

	private static final String TITLE_QUERY_TITLE = "&Query Title:";

	private static final String TITLE = "Enter query parameters";

	private static final String DESCRIPTION = "If attributes are blank or stale press the Update button.";

	private final String titleString;

	protected Text title;

	protected ISearchPageContainer scontainer = null;

	protected TaskRepository repository;

	public AbstractRepositoryQueryPage(String wizardTitle) {
		this(wizardTitle, null);
		setTitle(TITLE);
		setDescription(DESCRIPTION);
		setImageDescriptor(TasksUiImages.BANNER_REPOSITORY);
		setPageComplete(false);
	}

	public AbstractRepositoryQueryPage(String wizardTitle, String queryTitle) {
		super(wizardTitle);
		titleString = queryTitle == null ? "" : queryTitle;
	}

	public void createControl(Composite parent) {
		if (scontainer == null) {
			createTitleGroup(parent);
			title.setFocus();
		}
	}

	private void createTitleGroup(Composite parent) {
		Composite group = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout(2, false);
		group.setLayout(layout);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		group.setLayoutData(gd);

		Label label = new Label(group, SWT.NONE);
		label.setText(TITLE_QUERY_TITLE);

		title = new Text(group, SWT.BORDER);
		title.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		title.setText(titleString);

		title.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(isPageComplete());
			}
		});
	}

	@Override
	public boolean isPageComplete() {
		Set<AbstractRepositoryQuery> queries = TasksUi.getTaskListManager().getTaskList().getQueries();
		Set<AbstractTaskCategory> categories = TasksUi.getTaskListManager().getTaskList().getCategories();

		if (title == null || title.getText().equals("")) {
			setErrorMessage("Please specify a title for the query.");
			return false;
		} else {
			if (getWizard() instanceof AbstractEditQueryWizard) {
				String oldSummary = ((AbstractEditQueryWizard) getWizard()).getQuerySummary();
				if (oldSummary != null && title.getText().equals(oldSummary)) {
					setErrorMessage(null);
					return true;
				}
			}
			for (AbstractTaskCategory category : categories) {
				if (title.getText().equals(category.getSummary())) {
					setErrorMessage("A category with this name already exists, please choose another name.");
					return false;
				}
			}
			for (AbstractRepositoryQuery query : queries) {
				if (title.getText().equals(query.getSummary())) {
					setErrorMessage("A query with this name already exists, please choose another name.");
					return false;
				}
			}
		}
		setErrorMessage(null);
		return true;
	}

	public String getQueryTitle() {
		return title != null ? title.getText() : "";
	}

	public abstract AbstractRepositoryQuery getQuery();

	public void saveState() {
		// empty
	}

	public void setContainer(ISearchPageContainer container) {
		scontainer = container;
	}

	public boolean inSearchContainer() {
		return scontainer != null;
	}

	public boolean performAction() {
		if (repository == null) {
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), ITasksUiConstants.TITLE_DIALOG,
					TaskRepositoryManager.MESSAGE_NO_REPOSITORY);
			return false;
		}

		NewSearchUI.activateSearchResultView();

		AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
				repository.getConnectorKind());
		if (connector != null) {
			SearchHitCollector collector = new SearchHitCollector(TasksUi.getTaskListManager().getTaskList(),
					repository, getQuery());
			NewSearchUI.runQueryInBackground(collector);
		}
		return true;
	}

	/**
	 * @since 2.1
	 */
	public void setControlsEnabled(boolean enabled) {
		setControlsEnabled(getControl(), enabled);
	}

	// TODO: make reusable or find better API, task editor has similar functionality
	private void setControlsEnabled(Control control, boolean enabled) {
		if (control instanceof Composite) {
			for (Control childControl : ((Composite) control).getChildren()) {
				childControl.setEnabled(enabled);
				setControlsEnabled(childControl, enabled);
			}
		}
		setPageComplete(isPageComplete());
	}

}
