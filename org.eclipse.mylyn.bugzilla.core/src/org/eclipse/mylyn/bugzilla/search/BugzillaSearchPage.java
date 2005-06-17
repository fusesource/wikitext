/*******************************************************************************
 * Copyright (c) 2003 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylar.bugzilla.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.security.auth.login.LoginException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.mylar.bugzilla.BugzillaPlugin;
import org.eclipse.mylar.bugzilla.BugzillaPreferences;
import org.eclipse.mylar.bugzilla.IBugzillaConstants;
import org.eclipse.mylar.bugzilla.saveQuery.GetQueryDialog;
import org.eclipse.mylar.bugzilla.saveQuery.SaveQueryDialog;
import org.eclipse.mylar.bugzilla.saveQuery.SavedQueryFile;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.help.WorkbenchHelpSystem;


/**
 * Bugzilla search page
 */
public class BugzillaSearchPage extends DialogPage implements ISearchPage {
	private Combo summaryPattern = null;
	private static ArrayList<BugzillaSearchData> previousSummaryPatterns = new ArrayList<BugzillaSearchData>(20);
	private static ArrayList<BugzillaSearchData> previousEmailPatterns = new ArrayList<BugzillaSearchData>(20);
	private static ArrayList<BugzillaSearchData> previousCommentPatterns = new ArrayList<BugzillaSearchData>(20);
	private ISearchPageContainer scontainer = null;
	private boolean firstTime = true;

	private IDialogSettings fDialogSettings;

	private static final String [] patternOperationText = {"all words", "any word", "regexp"};
	private static final String [] patternOperationValues = {"allwordssubstr", "anywordssubstr", "regexp"};
	private static final String [] emailOperationText = {"substring", "exact", "regexp"};
	private static final String [] emailOperationValues = {"substring", "exact", "regexp"};
	private static final String [] emailRoleValues = {"emailassigned_to1", "emailreporter1", "emailcc1", "emaillongdesc1"};

	IPreferenceStore prefs = BugzillaPlugin.getDefault().getPreferenceStore();
	private String [] statusValues = BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.STATUS_VALUES));
	private String [] preselectedStatusValues = BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.PRESELECTED_STATUS_VALUES));
	private String [] resolutionValues = BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.RESOLUTION_VALUES));
	private String [] severityValues = BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.SEVERITY_VALUES));
	private String [] priorityValues = BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.PRIORITY_VALUES));
	private String [] hardwareValues = BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.HARDWARE_VALUES));
	private String [] osValues = BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.OS_VALUES));
	
	private String [] productValues = BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.PRODUCT_VALUES));
	private String [] componentValues = BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.COMPONENT_VALUES));
	private String [] versionValues = BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.VERSION_VALUES));
	private String [] targetValues = BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.TARGET_VALUES));
	
	private static class BugzillaSearchData {
		/** Pattern to match on */
		String pattern;
		/** Pattern matching criterion */
		int operation;
		
		BugzillaSearchData(String pattern, int operation) {
			this.pattern = pattern;
			this.operation = operation;
		}
	}
	
	/**
	 * The constructor.
	 */
	public BugzillaSearchPage() {
		super();
	}

	/**
	 * Insert the method's description here.
	 * @see DialogPage#createControl
	 */
	public void createControl(Composite parent) {
		readConfiguration();

		Composite control = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		control.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);

		createTextSearchComposite(control);
		createComment(control);
		createProductAttributes(control);
		createLists(control);
		createLastDays(control);
		createEmail(control);
		createSaveQuery(control);
		input = new SavedQueryFile(BugzillaPlugin.getDefault().getStateLocation().toString(), "/queries");
		createUpdate(control);
		

		setControl(control);
		WorkbenchHelpSystem.getInstance().setHelp(control, IBugzillaConstants.SEARCH_PAGE_CONTEXT);
	}

	private Control createTextSearchComposite(Composite control) {
		GridData gd;
		Label label;

		Composite group = new Composite(control, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		gd = new GridData(GridData.BEGINNING | GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 2;
		group.setLayoutData(gd);

		// Info text
		label = new Label(group, SWT.LEFT);
		label.setText("Bug id or summary search terms");
		gd = new GridData(GridData.BEGINNING);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		// Pattern combo
		summaryPattern = new Combo(group, SWT.SINGLE | SWT.BORDER);
		summaryPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				scontainer.setPerformActionEnabled(canQuery());
			}
		});
		summaryPattern.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected(summaryPattern, summaryOperation, previousSummaryPatterns);
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		summaryPattern.setLayoutData(gd);
		
		summaryOperation = new Combo(group, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
		summaryOperation.setItems(patternOperationText);
		summaryOperation.setText(patternOperationText[0]);
		summaryOperation.select(0);
		
		return group;
	}
	
	
	private Control createComment(Composite control) {
		GridData gd;
		Label label;

		Composite group = new Composite(control, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		gd = new GridData(GridData.BEGINNING | GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 2;
		group.setLayoutData(gd);

		// Info text
		label = new Label(group, SWT.LEFT);
		label.setText("Comment contains: ");
		gd = new GridData(GridData.BEGINNING);
		label.setLayoutData(gd);

		commentOperation = new Combo(group, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
		commentOperation.setItems(patternOperationText);
		commentOperation.setText(patternOperationText[0]);
		commentOperation.select(0);
		
		// Comment pattern combo
		commentPattern = new Combo(group, SWT.SINGLE | SWT.BORDER);
		commentPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				scontainer.setPerformActionEnabled(canQuery());
			}
		});
		commentPattern.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected(commentPattern, commentOperation, previousCommentPatterns);
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		commentPattern.setLayoutData(gd);
		
		return group;
	}
	
	/**
	 * Creates the area for selection on product/component/version.
	 */
	private Control createProductAttributes(Composite control) {
		GridData gd;
		GridLayout layout;
		
		// Search expression
		Group group = new Group(control, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 4;
		group.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 5;
		group.setLayoutData(gd);
		
		// Labels
		Label label = new Label(group, SWT.LEFT);
		label.setText("Product");
		
		label = new Label(group, SWT.LEFT);
		label.setText("Component");
		
		label = new Label(group, SWT.LEFT);
		label.setText("Version");
		
		label = new Label(group, SWT.LEFT);
		label.setText("Milestone");
		
		// Lists
		product = new List(group, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		product.setLayoutData(gd);
		
		component = new List(group, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		component.setLayoutData(gd);
		
		version = new List(group, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		version.setLayoutData(gd);
		
		target = new List(group, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		target.setLayoutData(gd);
		
		return group;
	}
	
	/**
	 * Creates the area for selection of bug attributes (status, etc.)
	 */
	private Control createLists(Composite control) {
		GridData gd;
		GridLayout layout;
		
		// Search expression
		Group group = new Group(control, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 6;
		group.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 5;
		group.setLayoutData(gd);
	
		// Labels
		Label label = new Label(group, SWT.LEFT);
		label.setText("Status");
		
		label = new Label(group, SWT.LEFT);
		label.setText("Resolution");
		
		label = new Label(group, SWT.LEFT);
		label.setText("Severity");
		
		label = new Label(group, SWT.LEFT);
		label.setText("Priority");
		
		label = new Label(group, SWT.LEFT);
		label.setText("Hardware");

		label = new Label(group, SWT.LEFT);
		label.setText("OS");
		
		// Lists
		status = new List(group, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		status.setLayoutData(gd);
		
		resolution = new List(group, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		resolution.setLayoutData(gd);
		
		severity = new List(group, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		severity.setLayoutData(gd);
		
		priority = new List(group, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		priority.setLayoutData(gd);
		
		hardware = new List(group, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		hardware.setLayoutData(gd);
		
		os = new List(group, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		os.setLayoutData(gd);
			
		return group;
	}
	
	private Text daysText;
	
	private Control createLastDays(Composite control)
	{
		GridLayout layout;
		GridData gd;

		Group group = new Group(control, SWT.NONE);
		layout = new GridLayout(3, false);
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		gd = new GridData(GridData.BEGINNING | GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 2;
		group.setLayoutData(gd);

		Label label = new Label(group, SWT.LEFT);
		label.setText("Only bugs changed in the last ");

		// operation combo
		daysText = new Text(group, SWT.BORDER);
		daysText.setTextLimit(5);
		daysText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String days = daysText.getText();
				if (days.length() == 0)
					return;
				for (int i = days.length() - 1; i >= 0; i--) {
					try {
						if (days.equals("") || Integer.parseInt(days) > -1) {
							if (i == days.length() - 1)
								return;
							else
								break;
						}
					} catch (NumberFormatException ex) {
						days = days.substring(0, i);
					}
				}
				daysText.setText(days);
			}
		});
		label = new Label(group, SWT.LEFT);
		label.setText(" Days.");


		return group;
	}
	
	private static final String [] emailText = {"bug owner", "reporter", "CC list", "commenter"};
	private Control createEmail(Composite control) {
		GridLayout layout;
		GridData gd;

		Group group = new Group(control, SWT.NONE);
		layout = new GridLayout(3, false);
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		gd = new GridData(GridData.BEGINNING | GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 2;
		group.setLayoutData(gd);
		
		Composite buttons = new Composite(group, SWT.NONE);
		layout = new GridLayout(4, false);
		buttons.setLayout(layout);
		buttons.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		gd = new GridData(GridData.BEGINNING);
		gd.horizontalSpan = 3;
		buttons.setLayoutData(gd);
		
		emailButton = new Button[emailText.length];
		for (int i = 0; i < emailButton.length; i++) {
			Button button = new Button(buttons, SWT.CHECK);
			button.setText(emailText[i]);
			emailButton[i] = button;
		}
		
		Label label = new Label(group, SWT.LEFT);
		label.setText("Email contains: ");

		// operation combo
		emailOperation = new Combo(group, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
		emailOperation.setItems(emailOperationText);
		emailOperation.setText(emailOperationText[0]);
		emailOperation.select(0);
		
		// pattern combo
		emailPattern = new Combo(group, SWT.SINGLE | SWT.BORDER);
		emailPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				scontainer.setPerformActionEnabled(canQuery());
			}
		});
		emailPattern.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected(emailPattern, emailOperation, previousEmailPatterns);
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		emailPattern.setLayoutData(gd);
		
		return group;
	}

	/**
	 * Creates the buttons for remembering a query and accessing previously
	 * saved queries.
	 */
	private Control createSaveQuery(Composite control) {
		GridLayout layout;
		GridData gd;

		Group group = new Group(control, SWT.NONE);
		layout = new GridLayout(3, false);
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		gd = new GridData(GridData.BEGINNING | GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 2;
		group.setLayoutData(gd);
		
		loadButton = new Button(group, SWT.PUSH | SWT.LEFT);
		loadButton.setText("Saved Queries...");
		final BugzillaSearchPage bsp = this;
		loadButton.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent event) {
				GetQueryDialog qd = new GetQueryDialog(getShell(),
						"Saved Queries", input);
				if (qd.open() == InputDialog.OK) {
					selIndex = qd.getSelected();
					if (selIndex != -1) {
						rememberedQuery = true;
						performAction();
						bsp.getShell().close();
					}
				}
			}
		});
		loadButton.setEnabled(true);
		loadButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		
		saveButton = new Button(group, SWT.PUSH | SWT.LEFT);
		saveButton.setText("Remember...");
		saveButton.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent event) {
				SaveQueryDialog qd = new SaveQueryDialog(getShell(),
						"Remember Query");
				if (qd.open() == InputDialog.OK) {
					String qName = qd.getText();
					if (qName != null && qName.compareTo("") != 0) {
						try {
							input.add(getQueryParameters().toString(), qName, summaryPattern.getText());
						}
						catch (UnsupportedEncodingException e) {
							/*
							 * Do nothing. Every implementation of the Java platform is required
							 * to support the standard charset "UTF-8"
							 */
						}
					}
				}
			}
		});
		saveButton.setEnabled(true);
		saveButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		
		return group;
	}
	
	public static SavedQueryFile getInput() {
		return input;
	}
			
	private Control createUpdate(final Composite control) {
		GridData gd;
		Label label;

		Composite group = new Composite(control, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		gd = new GridData(GridData.BEGINNING);
		gd.horizontalSpan = 2;
		group.setLayoutData(gd);

		//	Info text		
		label = new Label(group, SWT.LEFT);
		label.setText("Update search options from server (may take several seconds):");
		gd = new GridData(GridData.BEGINNING);
		label.setLayoutData(gd);
		
		updateButton = new Button(group, SWT.LEFT | SWT.PUSH);
		updateButton.setText("Update");
			
		updateButton.setLayoutData(new GridData());
		
		updateButton.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				
				monitorDialog.open();
				IProgressMonitor monitor = monitorDialog.getProgressMonitor();
				monitor.beginTask("Updating search options...", 55);

				try {
					BugzillaPreferences.updateQueryOptions(monitor);
					
					product.setItems(BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.PRODUCT_VALUES)));
					monitor.worked(1);
					component.setItems(BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.COMPONENT_VALUES)));
					monitor.worked(1);
					version.setItems(BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.VERSION_VALUES)));
					monitor.worked(1);
					target.setItems(BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.TARGET_VALUES)));
					monitor.worked(1);
					status.setItems(BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.STATUS_VALUES)));
					monitor.worked(1);
					status.setSelection(BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.PRESELECTED_STATUS_VALUES)));
					monitor.worked(1);
					resolution.setItems(BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.RESOLUTION_VALUES)));
					monitor.worked(1);
					severity.setItems(BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.SEVERITY_VALUES)));
					monitor.worked(1);
					priority.setItems(BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.PRIORITY_VALUES)));
					monitor.worked(1);
					hardware.setItems(BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.HARDWARE_VALUES)));
					monitor.worked(1);
					os.setItems(BugzillaPreferences.queryOptionsToArray(prefs.getString(IBugzillaConstants.OS_VALUES)));
					monitor.worked(1);
				}
				catch (LoginException exception) {
					// we had a problem that seems to have been caused from bad login info
					MessageDialog.openError(null, "Login Error", "Bugzilla could not log you in to get the information you requested since login name or password is incorrect.\nPlease check your settings in the bugzilla preferences. ");
					BugzillaPlugin.log(exception);
				}
				finally {
					monitor.done();
					monitorDialog.close();
				}
			}
		});
		
		return group;
	}
	
	private void handleWidgetSelected(Combo widget, Combo operation, ArrayList<BugzillaSearchData> history) {
		if (widget.getSelectionIndex() < 0)
			return;
		int index = history.size() - 1 - widget.getSelectionIndex();
		BugzillaSearchData patternData= history.get(index);
		if (patternData == null  || !widget.getText().equals(patternData.pattern))
			return;
		widget.setText(patternData.pattern);
		operation.setText(operation.getItem(patternData.operation));
	}

	/**
	 * @see ISearchPage#performAction()
	 */
	public boolean performAction() {
		getPatternData(summaryPattern, summaryOperation, previousSummaryPatterns);
		getPatternData(commentPattern, commentOperation, previousCommentPatterns);
		getPatternData(this.emailPattern, emailOperation, previousEmailPatterns);
		
		String summaryText;
		String url;
		if (rememberedQuery == true) {
			url = getQueryURL(new StringBuffer(input.getQueryParameters(selIndex)));
			summaryText = input.getSummaryText(selIndex);
		}
		else {
			try {
				StringBuffer params = getQueryParameters();
				url = getQueryURL(params);
				summaryText = summaryPattern.getText();
			}
			catch (UnsupportedEncodingException e) {
				/*
				 * These statements should never be executed. Every implementation of
				 * the Java platform is required to support the standard charset
				 * "UTF-8"
				 */
				url = "";
				summaryText = "";
			}
		}

		try {
			// if the summary contains a single bug id, open the bug directly
			int id = Integer.parseInt(summaryText);
			return BugzillaSearchHit.show(id);
		} catch (NumberFormatException ignored) {
			// ignore this since this means that the text is not a bug id
		}
		
		// Don't activate the search result view until it is known that the 
		// user is not opening a bug directly -- there is no need to open
		// the view if no searching is going to take place.
		NewSearchUI.activateSearchResultView();
		
		BugzillaPlugin.getDefault().getPreferenceStore().setValue(IBugzillaConstants.MOST_RECENT_QUERY, summaryText);

		IBugzillaSearchResultCollector collector= new BugzillaSearchResultCollector();
		
		IBugzillaSearchOperation op= new BugzillaSearchOperation(
			url, collector);
			
		BugzillaSearchQuery searchQuery = new BugzillaSearchQuery(op);
		NewSearchUI.runQueryInBackground(searchQuery);

		return true;
	}

	/**
	 * @see ISearchPage#setContainer(ISearchPageContainer)
	 */
	public void setContainer(ISearchPageContainer container) {
		scontainer = container;
	}
	
	@Override
	public void setVisible(boolean visible) {
		if (visible && summaryPattern != null) {
			if (firstTime) {
				firstTime = false;
				// Set item and text here to prevent page from resizing
				summaryPattern.setItems(getPreviousPatterns(previousSummaryPatterns));
				commentPattern.setItems(getPreviousPatterns(previousCommentPatterns));
				emailPattern.setItems(getPreviousPatterns(previousEmailPatterns));

				product.setItems(productValues);
				component.setItems(componentValues);
				version.setItems(versionValues);
				target.setItems(targetValues);

				status.setItems(statusValues);
				status.setSelection(preselectedStatusValues);
				resolution.setItems(resolutionValues);
				severity.setItems(severityValues);
				priority.setItems(priorityValues);
				hardware.setItems(hardwareValues);
				os.setItems(osValues);
			}
			summaryPattern.setFocus();
			scontainer.setPerformActionEnabled(canQuery());
		}
		super.setVisible(visible);
	}

	/**
	 * Returns <code>true</code> if at least some parameter is given to query on.
	 */
	private boolean canQuery() {
		return product.getSelectionCount() > 0 ||
			component.getSelectionCount() > 0 ||
			version.getSelectionCount() > 0 ||
			target.getSelectionCount() > 0 ||
			status.getSelectionCount() > 0 ||
			resolution.getSelectionCount() > 0 ||
			severity.getSelectionCount() > 0 ||
			priority.getSelectionCount() > 0 ||
			hardware.getSelectionCount() > 0 ||
			os.getSelectionCount() > 0 ||
			summaryPattern.getText().length() > 0 ||
			commentPattern.getText().length() > 0 ||
			emailPattern.getText().length() > 0;
	}

	/**
	 * Return search pattern data and update search history list.
	 * An existing entry will be updated or a new one created.
	 */
	private BugzillaSearchData getPatternData(Combo widget, Combo operation, ArrayList<BugzillaSearchData> previousSearchQueryData) {
		String pattern =  widget.getText();
		if (pattern == null || pattern.trim().equals("")) {
			return null;
		}
		BugzillaSearchData match = null;
		int i = previousSearchQueryData.size() - 1;
		while (i >= 0) {
			match = previousSearchQueryData.get(i);
			if (pattern.equals(match.pattern)) {
				break;
			}
			i--;
		}
		if (i >= 0) {
			match.operation = operation.getSelectionIndex();
			// remove - will be added last (see below)
			previousSearchQueryData.remove(match);
		} else {
			match= new BugzillaSearchData(widget.getText(), operation.getSelectionIndex());
		}
		previousSearchQueryData.add(match);
		return match;
	}

	/**
	 * Returns an array of previous summary patterns
	 */
	private String [] getPreviousPatterns(ArrayList<BugzillaSearchData> patternHistory) {
		int size = patternHistory.size();
		String [] patterns = new String[size];
		for (int i = 0; i < size; i++)
			patterns[i]= (patternHistory.get(size - 1 - i)).pattern;
		return patterns;
	}
	
	
	private String getQueryURL(StringBuffer params) {
		StringBuffer url = new StringBuffer(getQueryURLStart().toString());
		url.append(params);
		return url.toString();
	}
	
	/**
	 * Creates the bugzilla query URL start.
	 * 
	 * Example: https://bugs.eclipse.org/bugs/buglist.cgi?
	 */
	private StringBuffer getQueryURLStart() {
		StringBuffer sb = new StringBuffer(BugzillaPlugin.getDefault().getServerName());
		
		if (sb.charAt(sb.length()-1) != '/') {
			sb.append('/');
		}
		sb.append("buglist.cgi?");
		
		// use the username and password if we have it
		if(BugzillaPreferences.getUserName() != null && !BugzillaPreferences.getUserName().equals("") && BugzillaPreferences.getPassword() != null && !BugzillaPreferences.getPassword().equals(""))
		{
			try {
				sb.append("GoAheadAndLogIn=1&Bugzilla_login=" + URLEncoder.encode(BugzillaPreferences.getUserName(), "UTF-8") + "&Bugzilla_password=" + URLEncoder.encode(BugzillaPreferences.getPassword(), "UTF-8") + "&");
			} catch (UnsupportedEncodingException e) {
				/*
				 * Do nothing. Every implementation of the Java platform is required
				 * to support the standard charset "UTF-8"
				 */
			}
		}
		
		return sb;
	}
	
	/**
	 * Goes through the query form and builds up the query parameters.
	 * 
	 * Example: short_desc_type=substring&amp;short_desc=bla&amp; ...
	 * @throws UnsupportedEncodingException
	 */
	private StringBuffer getQueryParameters() throws UnsupportedEncodingException {
		StringBuffer sb = new StringBuffer();
		
		sb.append("short_desc_type=");
		sb.append(patternOperationValues[summaryOperation.getSelectionIndex()]);
		
		sb.append("&short_desc=");
		sb.append(URLEncoder.encode(summaryPattern.getText(), "UTF-8"));
		
		int [] selected = product.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			sb.append("&product=");
			sb.append(URLEncoder.encode(product.getItem(selected[i]), "UTF-8"));
		}
		
		selected = component.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			sb.append("&component=");
			sb.append(URLEncoder.encode(component.getItem(selected[i]), "UTF-8"));
		}

		selected = version.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			sb.append("&version=");
			sb.append(URLEncoder.encode(version.getItem(selected[i]), "UTF-8"));
		}
		
		selected = target.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			sb.append("&target_milestone=");
			sb.append(URLEncoder.encode(target.getItem(selected[i]), "UTF-8"));
		}

		sb.append("&long_desc_type=");
		sb.append(patternOperationValues[commentOperation.getSelectionIndex()]);
		sb.append("&long_desc=");
		sb.append(URLEncoder.encode(commentPattern.getText(), "UTF-8"));
		
		selected = status.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			sb.append("&bug_status=");
			sb.append(status.getItem(selected[i]));
		}

		selected = resolution.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			sb.append("&resolution=");
			sb.append(resolution.getItem(selected[i]));
		}

		selected = severity.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			sb.append("&bug_severity=");
			sb.append(severity.getItem(selected[i]));
		}

		selected = priority.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			sb.append("&priority=");
			sb.append(priority.getItem(selected[i]));
		}

		selected = hardware.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			sb.append("&ref_platform=");
			sb.append(URLEncoder.encode(hardware.getItem(selected[i]), "UTF-8"));
		}

		selected = os.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			sb.append("&op_sys=");
			sb.append(URLEncoder.encode(os.getItem(selected[i]), "UTF-8"));
		}
		
		if (emailPattern.getText() != null) {
			for (int i = 0; i < emailButton.length; i++) {
				if (emailButton[i].getSelection()) {
					sb.append("&");
					sb.append(emailRoleValues[i]);
					sb.append("=1");
				}
			}
			sb.append("&emailtype1=");
			sb.append(emailOperationValues[emailOperation.getSelectionIndex()]);
			sb.append("&email1=");
			sb.append(URLEncoder.encode(emailPattern.getText(), "UTF-8"));
		}
		
		if (daysText.getText() != null && !daysText.getText().equals("")) {
			try
			{
				Integer.parseInt(daysText.getText());
				sb.append("&changedin=");
				sb.append(URLEncoder.encode(daysText.getText(), "UTF-8"));
			}
			catch(NumberFormatException ignored) {
				// this means that the days is not a number, so don't worry
			}
		}
		
		return sb;
	}
	
	//--------------- Configuration handling --------------

	// Dialog store id constants
	private final static String PAGE_NAME = "BugzillaSearchPage"; //$NON-NLS-1$

	private Combo summaryOperation;

	private List product;

	private List os;

	private List hardware;

	private List priority;

	private List severity;

	private List resolution;

	private List status;

	private Combo commentOperation;

	private Combo commentPattern;

	private List component;

	private List version;

	private List target;

	private Combo emailOperation;

	private Combo emailPattern;
	
	private Button [] emailButton;
	
	/** File containing saved queries */
	private static SavedQueryFile input;

	/** "Remember query" button */
	private Button saveButton;
	/** "Saved queries..." button */
	private Button loadButton;
	/** Run a remembered query */
	boolean rememberedQuery = false;
	/** Index of the saved query to run */
	int selIndex;

	private Button updateButton;
	
	private ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(BugzillaPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell());
	
	/**
	 * Returns the page settings for this Java search page.
	 * 
	 * @return the page settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		IDialogSettings settings = BugzillaPlugin.getDefault().getDialogSettings();
		fDialogSettings = settings.getSection(PAGE_NAME);
		if (fDialogSettings == null)
			fDialogSettings = settings.addNewSection(PAGE_NAME);
		return fDialogSettings;
	}
	
	/**
	 * Initializes itself from the stored page settings.
	 */
	private void readConfiguration() {
		getDialogSettings();
	}
}
