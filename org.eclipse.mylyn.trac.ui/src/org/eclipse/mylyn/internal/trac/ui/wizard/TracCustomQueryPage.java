/*******************************************************************************
 * Copyright (c) 2003 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylar.internal.trac.ui.wizard;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.internal.trac.core.ITracClient;
import org.eclipse.mylar.internal.trac.core.TracCorePlugin;
import org.eclipse.mylar.internal.trac.core.TracException;
import org.eclipse.mylar.internal.trac.core.TracRepositoryConnector;
import org.eclipse.mylar.internal.trac.core.TracRepositoryQuery;
import org.eclipse.mylar.internal.trac.core.model.TracSearch;
import org.eclipse.mylar.internal.trac.core.model.TracSearchFilter;
import org.eclipse.mylar.internal.trac.core.model.TracSearchFilter.CompareOperator;
import org.eclipse.mylar.internal.trac.ui.TracUiPlugin;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.core.TaskRepositoryManager;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.search.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * Trac search page. Provides a form similar to the one the Bugzilla connector
 * uses.
 * 
 * @author Steffen Pingel
 */
public class TracCustomQueryPage extends AbstractRepositoryQueryPage {

	private static final String TITLE = "Enter query parameters";

	private static final String DESCRIPTION = "If attributes are blank or stale press the Update button.";

	private static final String TITLE_QUERY_TITLE = "Query Title:";

	private static final String[] DEFAULT_STATUS_SELECTION = new String[] { "new", "assigned", "reopened", };

	private TracRepositoryQuery query;

	private Text titleText;

	private static final int PRODUCT_HEIGHT = 60;

	private static final int STATUS_HEIGHT = 40;

	protected final static String PAGE_NAME = "TracSearchPage"; //$NON-NLS-1$

	private static final String SEARCH_URL_ID = PAGE_NAME + ".SEARCHURL";

	protected Combo summaryText = null;

	protected Combo repositoryCombo = null;

	private TextSearchField summaryField;

	private TextSearchField descriptionField;

	private ListSearchField componentField;

	private ListSearchField versionField;

	private ListSearchField milestoneField;

	private ListSearchField priorityField;

	private ListSearchField typeField;

	private ListSearchField resolutionField;

	private ListSearchField statusField;

	private Button updateButton;

	private TextSearchField keywordsField;

	private Map<String, SearchField> searchFieldByName = new HashMap<String, SearchField>();

	private boolean firstTime = true;

	private UserSearchField ownerField;

	private UserSearchField reporterField;

	private UserSearchField ccField;

	public TracCustomQueryPage(TaskRepository repository, AbstractRepositoryQuery query) {
		super(TITLE);

		this.repository = repository;
		this.query = (TracRepositoryQuery) query;

		setTitle(TITLE);
		setDescription(DESCRIPTION);
	}

	public TracCustomQueryPage(TaskRepository repository) {
		this(repository, null);
	}

	@Override
	public void createControl(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);
		GridLayout layout = new GridLayout(4, false);
		if (isSearchPage()) {
			layout.marginWidth = 0;
			layout.marginHeight = 0;
		}
		control.setLayout(layout);

		createTitleGroup(control);

		summaryField = new TextSearchField("summary");
		summaryField.createControls(control, "Summary");

		descriptionField = new TextSearchField("description");
		descriptionField.createControls(control, "Description");

		keywordsField = new TextSearchField("keywords");
		keywordsField.createControls(control, "Keywords");

		createOptionsGroup(control);

		createUserGroup(control);

		if (query != null) {
			titleText.setText(query.getSummary());
			restoreWidgetValues(query.getTracSearch());
		}

		setControl(control);
	}

	@Override
	public boolean canFlipToNextPage() {
		return false;
	}

	private void restoreWidgetValues(TracSearch search) {
		java.util.List<TracSearchFilter> filters = search.getFilters();
		for (TracSearchFilter filter : filters) {
			SearchField field = searchFieldByName.get(filter.getFieldName());
			if (field != null) {
				field.setFilter(filter);
			} else {
				MylarStatusHandler.log("Ignoring invalid search filter: " + filter, this);
			}
		}
	}

	private void createTitleGroup(Composite control) {
		if (inSearchContainer()) {
			return;
		}

		Label titleLabel = new Label(control, SWT.NONE);
		titleLabel.setText(TITLE_QUERY_TITLE);

		titleText = new Text(control, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 3;
		titleText.setLayoutData(gd);
		titleText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				// ignore
			}

			public void keyReleased(KeyEvent e) {
				getContainer().updateButtons();
			}
		});
	}

	protected Control createOptionsGroup(Composite control) {
		Group group = new Group(control, SWT.NONE);
		// group.setText("Ticket Attributes");
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		group.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 4;
		group.setLayoutData(gd);

		createProductAttributes(group);
		createTicketAttributes(group);
		createUpdateButton(group);

		return group;
	}

	protected void createUserGroup(Composite control) {
		TextSearchField userField = new TextSearchField(null);
		userField.createControls(control, "User");

		Composite radioGroup = new Composite(control, SWT.NONE);
		radioGroup.setLayout(new GridLayout(3, false));

		ownerField = new UserSearchField("owner", userField);
		ownerField.createControls(radioGroup, "Owner");

		reporterField = new UserSearchField("reporter", userField);
		reporterField.createControls(radioGroup, "Reporter");

		ccField = new UserSearchField("cc", userField);
		ccField.createControls(radioGroup, "CC");

		// select first control by default
		setUserGroupSelection(ownerField);
	}

	/**
	 * Selects a user radio button and make sure only a single radio button is
	 * selected at a time.
	 */
	protected void setUserGroupSelection(UserSearchField searchField) {
		ownerField.getRadioButton().setSelection(searchField == ownerField);
		reporterField.getRadioButton().setSelection(searchField == reporterField);
		ccField.getRadioButton().setSelection(searchField == ccField);
	}
	
	/**
	 * Creates the area for selection on product attributes
	 * component/version/milestone.
	 */
	protected Control createProductAttributes(Composite control) {
		Composite group = new Composite(control, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		group.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;
		group.setLayoutData(gd);

		Label label = new Label(group, SWT.LEFT);
		label.setText("Component");

		label = new Label(group, SWT.LEFT);
		label.setText("Version");

		label = new Label(group, SWT.LEFT);
		label.setText("Milestone");

		componentField = new ListSearchField("component");
		componentField.createControls(group, PRODUCT_HEIGHT);

		versionField = new ListSearchField("version");
		versionField.createControls(group, PRODUCT_HEIGHT);

		milestoneField = new ListSearchField("milestone");
		milestoneField.createControls(group, PRODUCT_HEIGHT);

		return group;
	}

	/**
	 * Creates the area for selection of ticket attributes
	 * status/resolution/priority.
	 */
	protected Control createTicketAttributes(Composite control) {
		Composite group = new Composite(control, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		group.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;
		group.setLayoutData(gd);

		Label label = new Label(group, SWT.LEFT);
		label.setText("Status");

		label = new Label(group, SWT.LEFT);
		label.setText("Resolution");

		label = new Label(group, SWT.LEFT);
		label.setText("Type");

		label = new Label(group, SWT.LEFT);
		label.setText("Priority");

		statusField = new ListSearchField("status");
		statusField.createControls(group, STATUS_HEIGHT);

		resolutionField = new ListSearchField("resolution");
		resolutionField.createControls(group, STATUS_HEIGHT);

		typeField = new ListSearchField("type");
		typeField.createControls(group, STATUS_HEIGHT);

		priorityField = new ListSearchField("priority");
		priorityField.createControls(group, STATUS_HEIGHT);

		return group;
	}

	protected Control createUpdateButton(final Composite control) {
		Composite group = new Composite(control, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		updateButton = new Button(group, SWT.PUSH);
		updateButton.setText("Update Attributes from Repository");
		updateButton.setLayoutData(new GridData());
		updateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (repository != null) {
					updateAttributesFromRepository(true);
				} else {
					MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
							TracUiPlugin.TITLE_MESSAGE_DIALOG, TaskRepositoryManager.MESSAGE_NO_REPOSITORY);
				}
			}
		});

		return group;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (scontainer != null) {
			scontainer.setPerformActionEnabled(true);
		}

		if (visible && firstTime) {
			firstTime = false;
			if (!hasAttributes()) {
				// delay the execution so the dialog's progress bar is visible
				// when the attributes are updated
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						if (getControl() != null && !getControl().isDisposed()) {
							initializePage();
						}
					}

				});
			} else {
				// no remote connection is needed to get attributes therefore do
				// not use delayed execution to avoid flickering
				initializePage();
			}
		}
	}

	private void initializePage() {
		updateAttributesFromRepository(false);
		boolean restored = (query != null);
		if (isSearchPage()) {
			restored |= restoreWidgetValues();
		}
		// initialize with default values
		if (!restored) {
			statusField.selectItems(DEFAULT_STATUS_SELECTION);
		}
	}

	private boolean isSearchPage() {
		return scontainer != null;
	}

	private boolean hasAttributes() {
		TracRepositoryConnector connector = (TracRepositoryConnector) TasksUiPlugin.getRepositoryManager()
				.getRepositoryConnector(TracCorePlugin.REPOSITORY_KIND);
		try {
			ITracClient client = connector.getClientManager().getRepository(repository);
			return client.hasAttributes();
		} catch (MalformedURLException e) {
			return false;
		}
	}

	private void updateAttributesFromRepository(final boolean force) {
		TracRepositoryConnector connector = (TracRepositoryConnector) TasksUiPlugin.getRepositoryManager()
				.getRepositoryConnector(TracCorePlugin.REPOSITORY_KIND);
		final ITracClient client;
		try {
			client = connector.getClientManager().getRepository(repository);
		} catch (MalformedURLException e) {
			TracUiPlugin.handleTracException(e);
			return;
		}

		if (!client.hasAttributes() || force) {
			try {
				IRunnableWithProgress runnable = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							client.updateAttributes(monitor, force);
						} catch (TracException e) {
							throw new InvocationTargetException(e);
						}
					}
				};

				if (getContainer() != null) {
					getContainer().run(true, true, runnable);
				} else if (scontainer != null) {
					scontainer.getRunnableContext().run(true, true, runnable);
				} else {
					IProgressService service = PlatformUI.getWorkbench().getProgressService();
					service.run(true, true, runnable);
				}
			} catch (InvocationTargetException e) {
				TracUiPlugin.handleTracException(e.getCause());
				return;
			} catch (InterruptedException e) {
				return;
			}
		}

		statusField.setValues(client.getTicketStatus());
		resolutionField.setValues(client.getTicketResolutions());
		typeField.setValues(client.getTicketTypes());
		priorityField.setValues(client.getPriorities());

		componentField.setValues(client.getComponents());
		versionField.setValues(client.getVersions());
		milestoneField.setValues(client.getMilestones());
	}

	public TaskRepository getRepository() {
		return repository;
	}

	public void setRepository(TaskRepository repository) {
		this.repository = repository;
	}

	@Override
	public boolean isPageComplete() {
		if (titleText != null && titleText.getText().length() > 0) {
			return true;
		}
		return false;
	}

	public String getQueryUrl(String repsitoryUrl) {
		TracSearch search = getTracSearch();

		StringBuilder sb = new StringBuilder();
		sb.append(repsitoryUrl);
		sb.append(ITracClient.QUERY_URL);
		sb.append(search.toUrl());
		return sb.toString();
	}

	private TracSearch getTracSearch() {
		TracSearch search = new TracSearch();
		for (SearchField field : searchFieldByName.values()) {
			TracSearchFilter filter = field.getFilter();
			if (filter != null) {
				search.addFilter(filter);
			}
		}
		return search;
	}

	@Override
	public TracRepositoryQuery getQuery() {
		return new TracRepositoryQuery(repository.getUrl(), getQueryUrl(repository.getUrl()), getTitleText(),
				TasksUiPlugin.getTaskListManager().getTaskList());
	}

	private String getTitleText() {
		return (titleText != null) ? titleText.getText() : "<search>";
	}

	// public boolean performAction() {
	//
	// Proxy proxySettings = TasksUiPlugin.getDefault().getProxySettings();
	// SearchHitCollector collector = new
	// SearchHitCollector(TasksUiPlugin.getTaskListManager().getTaskList(),
	// repository, getQuery(), proxySettings);
	// NewSearchUI.runQueryInBackground(collector);
	//
	// return true;
	// }

	@Override
	public boolean performAction() {
		if (isSearchPage()) {
			saveWidgetValues();
		}

		return super.performAction();
	}

	public IDialogSettings getDialogSettings() {
		IDialogSettings settings = TracUiPlugin.getDefault().getDialogSettings();
		IDialogSettings dialogSettings = settings.getSection(PAGE_NAME);
		if (dialogSettings == null) {
			dialogSettings = settings.addNewSection(PAGE_NAME);
		}
		return dialogSettings;
	}

	private boolean restoreWidgetValues() {
		IDialogSettings settings = getDialogSettings();
		String repoId = "." + repository.getUrl();

		String searchUrl = settings.get(SEARCH_URL_ID + repoId);
		if (searchUrl == null) {
			return false;
		}

		restoreWidgetValues(new TracSearch(searchUrl));
		return true;
	}

	public void saveWidgetValues() {
		String repoId = "." + repository.getUrl();
		IDialogSettings settings = getDialogSettings();
		settings.put(SEARCH_URL_ID + repoId, getTracSearch().toUrl());
	}

	private abstract class SearchField {

		protected String fieldName;

		public SearchField(String fieldName) {
			this.fieldName = fieldName;

			if (fieldName != null) {
				assert !searchFieldByName.containsKey(fieldName);
				searchFieldByName.put(fieldName, this);
			}
		}

		public String getFieldName() {
			return fieldName;
		}

		public abstract TracSearchFilter getFilter();

		public abstract void setFilter(TracSearchFilter filter);

	}

	private class TextSearchField extends SearchField {

		private Combo conditionCombo;

		protected Text searchText;

		private Label label;

		private CompareOperator[] compareOperators = { CompareOperator.CONTAINS, CompareOperator.CONTAINS_NOT,
				CompareOperator.BEGINS_WITH, CompareOperator.ENDS_WITH, CompareOperator.IS, CompareOperator.IS_NOT, };

		public TextSearchField(String fieldName) {
			super(fieldName);
		}

		public void createControls(Composite parent, String labelText) {
			label = new Label(parent, SWT.LEFT);
			label.setText(labelText);

			conditionCombo = new Combo(parent, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
			for (CompareOperator op : compareOperators) {
				conditionCombo.add(op.toString());
			}
			conditionCombo.setText(compareOperators[0].toString());

			searchText = new Text(parent, SWT.BORDER);
			GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			// the user search field has additional controls and no fieldName
			if (fieldName != null) {
				gd.horizontalSpan = 2;
			}
			searchText.setLayoutData(gd);
		}

		public CompareOperator getCondition() {
			return compareOperators[conditionCombo.getSelectionIndex()];
		}

		public String getSearchText() {
			return searchText.getText();
		}

		public boolean setCondition(CompareOperator operator) {
			if (conditionCombo != null) {
				int i = conditionCombo.indexOf(operator.toString());
				if (i != -1) {
					conditionCombo.select(i);
					return true;
				}
			}
			return false;
		}

		public void setSearchText(String text) {
			searchText.setText(text);
		}

		@Override
		public TracSearchFilter getFilter() {
			String text = getSearchText();
			if (text.length() == 0) {
				return null;
			}

			TracSearchFilter newFilter = new TracSearchFilter(getFieldName());
			newFilter.setOperator(getCondition());
			newFilter.addValue(getSearchText());
			return newFilter;
		}

		@Override
		public void setFilter(TracSearchFilter filter) {
			setCondition(filter.getOperator());
			java.util.List<String> values = filter.getValues();
			setSearchText(values.get(0));
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

	}

	private class ListSearchField extends SearchField {

		private List list;

		public ListSearchField(String fieldName) {
			super(fieldName);
		}

		public void setValues(Object[] items) {
			// preserve selected values
			TracSearchFilter filter = getFilter();

			list.removeAll();
			if (items != null) {
				list.setEnabled(true);
				for (Object item : items) {
					list.add(item.toString());
				}

				// restore selected values
				if (filter != null) {
					setFilter(filter);
				}
			} else {
				list.setEnabled(false);
			}
		}

		public void createControls(Composite parent, int height) {
			list = new List(parent, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint = height;
			list.setLayoutData(gd);
		}

		@Override
		public TracSearchFilter getFilter() {
			int[] indicies = list.getSelectionIndices();
			if (indicies.length == 0) {
				return null;
			}

			TracSearchFilter newFilter = new TracSearchFilter(getFieldName());
			newFilter.setOperator(CompareOperator.IS);
			for (int i : indicies) {
				newFilter.addValue(list.getItem(i));
			}
			return newFilter;
		}

		@Override
		public void setFilter(TracSearchFilter filter) {
			list.deselectAll();
			java.util.List<String> values = filter.getValues();
			for (String item : values) {
				int i = list.indexOf(item);
				if (i != -1) {
					list.select(i);
				} else {
					list.add(item, 0);
					list.select(0);
				}
			}
		}

		public void selectItems(String[] items) {
			list.deselectAll();
			for (String item : items) {
				int i = list.indexOf(item);
				if (i != -1) {
					list.select(i);
				}
			}
		}

	}

	private class UserSearchField extends SearchField {

		private TextSearchField textField;

		private Button radioButton;

		public UserSearchField(String fieldName, TextSearchField textField) {
			super(fieldName);

			this.textField = textField;
		}

		public Button getRadioButton() {
			return radioButton;
		}
		
		public void createControls(Composite parent, String labelText) {
			radioButton = new Button(parent, SWT.RADIO);
			radioButton.setText(labelText);
		}

		@Override
		public TracSearchFilter getFilter() {
			if (radioButton.getSelection()) {
				textField.setFieldName(fieldName);
				return textField.getFilter();
			}
			return null;
		}

		@Override
		public void setFilter(TracSearchFilter filter) {
			textField.setFieldName(fieldName);
			textField.setFilter(filter);
			setUserGroupSelection(this);
		}

	}

}
