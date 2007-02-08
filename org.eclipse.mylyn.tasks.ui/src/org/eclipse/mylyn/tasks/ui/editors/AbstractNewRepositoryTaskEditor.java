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
package org.eclipse.mylar.tasks.ui.editors;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.AbstractTaskContainer;
import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylar.tasks.core.TaskCategory;
import org.eclipse.mylar.tasks.core.TaskList;
import org.eclipse.mylar.tasks.ui.DatePicker;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.search.SearchHitCollector;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * An editor used to view a locally created bug that does not yet exist on a
 * server.
 * 
 * @author Rob Elves (modifications)
 */
public abstract class AbstractNewRepositoryTaskEditor extends AbstractRepositoryTaskEditor {

	private static final int DEFAULT_FIELD_WIDTH = 150;

	private static final int DEFAULT_ESTIMATED_TIME = 1;

	private static final String LABEL_CREATE = "Create New";

	private static final String LABEL_SEARCH_DUPS = "Search for Duplicates";

	private static final String ERROR_CREATING_BUG_REPORT = "Error creating bug report";

	private static final String NO_STACK_MESSAGE = "Unable to locate a stack trace in the description text.\nDuplicate search currently only supports stack trace matching.";

	protected Button searchForDuplicates;

	protected DatePicker scheduledForDate;

	protected Spinner estimatedTime;

	protected String newSummary = "";

	protected Button addToCategory;

	protected CCombo categoryChooser;

	public AbstractNewRepositoryTaskEditor(FormEditor editor) {
		super(editor);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) {
		NewTaskEditorInput ei = (NewTaskEditorInput) input;
		setSite(site);
		setInput(input);
		editorInput = ei;
		changedAttributes = new HashSet<RepositoryTaskAttribute>();
		taskData = ei.getTaskData();
		taskOutlineModel = RepositoryTaskOutlineNode.parseBugReport(taskData, false);
		newSummary = taskData.getSummary();
		repository = editorInput.getRepository();
		connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(repository.getKind());
		isDirty = false;
		IWorkbenchSiteProgressService progressService = getProgressService();
		if (progressService != null) {
			progressService.showBusyForFamily(FAMILY_SUBMIT);
		}
	}

	@Override
	protected void createDescriptionLayout(Composite composite) {
		FormToolkit toolkit = this.getManagedForm().getToolkit();
		Section section = toolkit.createSection(composite, ExpandableComposite.TITLE_BAR);
		section.setText(getSectionLabel(SECTION_NAME.DESCRIPTION_SECTION));
		section.setExpanded(true);
		section.setLayout(new GridLayout());
		section.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite descriptionComposite = toolkit.createComposite(section);
		GridLayout descriptionLayout = new GridLayout();

		descriptionComposite.setLayout(descriptionLayout);
		GridData descriptionData = new GridData(GridData.FILL_BOTH);
		descriptionData.grabExcessVerticalSpace = true;
		descriptionComposite.setLayoutData(descriptionData);
		section.setClient(descriptionComposite);

		descriptionTextViewer = addTextEditor(repository, descriptionComposite, taskData.getDescription(), true,
				SWT.FLAT | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		descriptionTextViewer.setEditable(true);

		GridData descriptionTextData = new GridData(GridData.FILL_BOTH);
		descriptionTextViewer.getControl().setLayoutData(descriptionTextData);
		descriptionTextViewer.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);

		toolkit.paintBordersFor(descriptionComposite);

	}

	@Override
	protected void createReportHeaderLayout(Composite comp) {
		addSummaryText(comp);
	}

	@Override
	protected void createAttachmentLayout(Composite comp) {
		// currently can't attach while creating new bug
	}

	@Override
	protected void createCommentLayout(Composite comp) {
		// ignore
	}

	@Override
	protected void createNewCommentLayout(Composite comp) {
		createPlanningLayout(comp);
	}

	protected void createPlanningLayout(Composite comp) {
		FormToolkit toolkit = this.getManagedForm().getToolkit();

		Section section = toolkit.createSection(comp, ExpandableComposite.TITLE_BAR | Section.TWISTIE);
		section.setText("Personal Planning");
		section.setLayout(new GridLayout());
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		section.setExpanded(true);

		Composite sectionClient = toolkit.createComposite(section);
		section.setClient(sectionClient);
		GridLayout layout = new GridLayout();
		layout.numColumns = 7;
		layout.makeColumnsEqualWidth = false;
		sectionClient.setLayout(layout);
		GridData clientDataLayout = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		sectionClient.setLayoutData(clientDataLayout);

		// Reminder
		toolkit.createLabel(sectionClient, "Scheduled for:");
//		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		scheduledForDate = new DatePicker(sectionClient, SWT.NONE, DatePicker.LABEL_CHOOSE);
		scheduledForDate.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		scheduledForDate.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		Calendar newTaskSchedule = Calendar.getInstance();
		int scheduledEndHour = TasksUiPlugin.getDefault().getPreferenceStore().getInt(
				TaskListPreferenceConstants.PLANNING_ENDHOUR);
		// If past scheduledEndHour set for following day
		if (newTaskSchedule.get(Calendar.HOUR_OF_DAY) >= scheduledEndHour) {
			TasksUiPlugin.getTaskListManager().setSecheduledIn(newTaskSchedule, 1);
		} else {
			TasksUiPlugin.getTaskListManager().setScheduledToday(newTaskSchedule);
		}
		scheduledForDate.setDate(newTaskSchedule);
		Button removeReminder = toolkit.createButton(sectionClient, "Clear", SWT.PUSH | SWT.CENTER);
		removeReminder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				scheduledForDate.setDate(null);
			}
		});

		// 1 Blank column after Reminder clear button
		Label dummy = toolkit.createLabel(sectionClient, "");
		GridData dummyLabelDataLayout = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		dummyLabelDataLayout.horizontalSpan = 1;
		dummyLabelDataLayout.widthHint = 30;
		dummy.setLayoutData(dummyLabelDataLayout);

		// Estimated time
		toolkit.createLabel(sectionClient, "Estimated time:");
//		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		estimatedTime = new Spinner(sectionClient, SWT.NONE);
		estimatedTime.setDigits(0);
		estimatedTime.setMaximum(100);
		estimatedTime.setMinimum(0);
		estimatedTime.setIncrement(1);
		estimatedTime.setSelection(DEFAULT_ESTIMATED_TIME);
		estimatedTime.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		GridData estimatedDataLayout = new GridData();
		estimatedDataLayout.widthHint = 110;
		estimatedTime.setLayoutData(estimatedDataLayout);
		toolkit.createLabel(sectionClient, "hours ");
//		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));

		// 1 Blank column
		Label blankLabel2 = toolkit.createLabel(sectionClient, "");
		GridData blankLabl2Layout = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		blankLabl2Layout.horizontalSpan = 1;
		blankLabl2Layout.widthHint = 25;
		blankLabel2.setLayoutData(blankLabl2Layout);

		toolkit.paintBordersFor(sectionClient);
	}

	@Override
	protected void addRadioButtons(Composite buttonComposite) {
		// Since NewBugModels have no special submitting actions,
		// no radio buttons are required.
	}

	@Override
	protected void createCustomAttributeLayout(Composite composite) {
		// ignore
	}

	public String getStackTraceFromDescription() {
		String description = descriptionTextViewer.getTextWidget().getText().trim();
		String stackTrace = null;

		if (description == null) {
			return null;
		}

		String punct = "!\"#$%&'\\(\\)*+,-./:;\\<=\\>?@\\[\\]^_`\\{|\\}~\n";
		String lineRegex = " *at\\s+[\\w" + punct + "]+ ?\\(.*\\) *\n?";
		Pattern tracePattern = Pattern.compile(lineRegex);
		Matcher match = tracePattern.matcher(description);

		if (match.find()) {
			// record the index of the first stack trace line
			int start = match.start();
			int lastEnd = match.end();

			// find the last stack trace line
			while (match.find()) {
				lastEnd = match.end();
			}

			// make sure there's still room to find the exception
			if (start <= 0) {
				return null;
			}

			// count back to the line before the stack trace to find the
			// exception
			int stackStart = 0;
			int index = start - 1;
			while (index > 1 && description.charAt(index) == ' ') {
				index--;
			}

			// locate the exception line index
			stackStart = description.substring(0, index - 1).lastIndexOf("\n");
			stackStart = (stackStart == -1) ? 0 : stackStart + 1;

			stackTrace = description.substring(stackStart, lastEnd);
		}

		return stackTrace;
	}

	@Override
	protected void updateTask() {
		taskData.setSummary(newSummary);
		taskData.setDescription(descriptionTextViewer.getTextWidget().getText());
		super.updateTask();
	}

	/**
	 * A listener for selection of the description textbox.
	 */
	protected class DescriptionListener implements Listener {
		public void handleEvent(Event event) {
			fireSelectionChanged(new SelectionChangedEvent(selectionProvider, new StructuredSelection(
					new RepositoryTaskSelection(taskData.getId(), taskData.getRepositoryUrl(), "New Description",
							false, taskData.getSummary()))));
		}
	}

	@Override
	public void handleSummaryEvent() {
		String sel = summaryText.getText();
		if (!(newSummary.equals(sel))) {
			newSummary = sel;
			markDirty(true);
		}
	}

	@Override
	protected void validateInput() {
		// ignore
	}

	@Override
	public boolean isDirty() {
		return true;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/**
	 * Creates the button layout. This displays options and buttons at the
	 * bottom of the editor to allow actions to be performed on the bug.
	 */
	@Override
	protected void createActionsLayout(Composite formComposite) {
		Section section = toolkit.createSection(formComposite, ExpandableComposite.TITLE_BAR);
		
		section.setText(getSectionLabel(SECTION_NAME.ACTIONS_SECTION));
		section.setExpanded(true);
		section.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, true).applyTo(section);
		

		Composite buttonComposite = toolkit.createComposite(section);
		buttonComposite.setLayout(new GridLayout(4, false));
		buttonComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		section.setClient(buttonComposite);
		
		
		addToCategory = toolkit.createButton(buttonComposite, "Add to Category", SWT.CHECK);
		categoryChooser = new CCombo(buttonComposite, SWT.FLAT | SWT.READ_ONLY);
		categoryChooser.setLayoutData(GridDataFactory.swtDefaults().hint(150, SWT.DEFAULT).create());
		toolkit.adapt(categoryChooser, true, true);
		categoryChooser.setFont(TEXT_FONT);
		TaskList taskList = TasksUiPlugin.getTaskListManager().getTaskList();
		List<AbstractTaskContainer> categories = taskList.getUserCategories();
		Collections.sort(categories, new Comparator<AbstractTaskContainer>() {

			public int compare(AbstractTaskContainer c1, AbstractTaskContainer c2) {
				return c1.getSummary().compareToIgnoreCase(c2.getSummary());
			}

		});
		categoryChooser.add("<root>");
		for (AbstractTaskContainer category : categories) {
			categoryChooser.add(category.getSummary());
		}
		categoryChooser.select(0);
		categoryChooser.setEnabled(false);
		categoryChooser.setData(categories);
		addToCategory.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				categoryChooser.setEnabled(addToCategory.getSelection());
			}

		});
		
		GridDataFactory.fillDefaults().hint(DEFAULT_FIELD_WIDTH, SWT.DEFAULT).span(3, SWT.DEFAULT).applyTo(categoryChooser);
		
		
		addActionButtons(buttonComposite);

		

		toolkit.paintBordersFor(buttonComposite);
	}

	/**
	 * Returns the {@link AbstractTaskContainer category} the new task belongs
	 * to
	 * 
	 * @return {@link AbstractTaskContainer category} where the new task must be
	 *         added to, or null if it must not be added to the task list
	 */
	@SuppressWarnings("unchecked")
	protected AbstractTaskContainer getCategory() {
		int index = categoryChooser.getSelectionIndex();
		if (addToCategory.getSelection() && index != -1) {
			if (index == 0) {
				return TasksUiPlugin.getTaskListManager().getTaskList().getRootCategory();
			}
			return ((List<AbstractTaskContainer>) categoryChooser.getData()).get(index - 1);
		}
		return null;
	}

	@Override
	protected void addActionButtons(Composite buttonComposite) {
		FormToolkit toolkit = new FormToolkit(buttonComposite.getDisplay());

		searchForDuplicates = toolkit.createButton(buttonComposite, LABEL_SEARCH_DUPS, SWT.NONE);
		GridData searchDuplicatesButtonData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		searchForDuplicates.setLayoutData(searchDuplicatesButtonData);
		searchForDuplicates.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				searchForDuplicates();
			}
		});

		submitButton = toolkit.createButton(buttonComposite, LABEL_CREATE, SWT.NONE);
		GridData submitButtonData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		submitButton.setLayoutData(submitButtonData);
		submitButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				submitToRepository();
			}
		});
		submitButton.setToolTipText("Submit to " + this.repository.getUrl());
	}

	protected boolean prepareSubmit() {
		submitButton.setEnabled(false);
		showBusy(true);

		if (summaryText != null && summaryText.getText().trim().equals("")) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					MessageDialog.openInformation(AbstractNewRepositoryTaskEditor.this.getSite().getShell(),
							ERROR_CREATING_BUG_REPORT, "A summary must be provided with new bug reports.");
					summaryText.setFocus();
					submitButton.setEnabled(true);
					showBusy(false);
				}
			});
			return false;
		}

		if (descriptionTextViewer != null && descriptionTextViewer.getTextWidget().getText().trim().equals("")) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					MessageDialog.openInformation(AbstractNewRepositoryTaskEditor.this.getSite().getShell(),
							ERROR_CREATING_BUG_REPORT, "A description must be provided with new reports.");
					descriptionTextViewer.getTextWidget().setFocus();
					submitButton.setEnabled(true);
					showBusy(false);
				}
			});
			return false;
		}

		return true;
	}

	public boolean searchForDuplicates() {

		String stackTrace = getStackTraceFromDescription();
		if (stackTrace == null) {
			MessageDialog.openWarning(null, "No Stack Trace Found", NO_STACK_MESSAGE);
			return false;
		}
		SearchHitCollector collector = getDuplicateSearchCollector(stackTrace);
		if (collector != null) {
			NewSearchUI.runQueryInBackground(collector);
			return true;
		}

		return false;
	}

	@Override
	protected void createPeopleLayout(Composite composite) {
		// ignore, new editor doesn't have people section
	}

	public AbstractRepositoryTask handleNewBugPost(String id) throws CoreException {
		final AbstractRepositoryTask newTask = super.handleNewBugPost(id);

		if (newTask != null) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					Calendar selectedDate = scheduledForDate.getDate();
					if (selectedDate != null) {
						// NewLocalTaskAction.scheduleNewTask(newTask);
						TasksUiPlugin.getTaskListManager().setScheduledFor(newTask, selectedDate.getTime());
					}

					newTask.setEstimatedTimeHours(estimatedTime.getSelection());

					Object selectedObject = null;
					if (TaskListView.getFromActivePerspective() != null)
						selectedObject = ((IStructuredSelection) TaskListView.getFromActivePerspective().getViewer()
								.getSelection()).getFirstElement();

					if (selectedObject instanceof TaskCategory) {
						TasksUiPlugin.getTaskListManager().getTaskList().moveToContainer(
								((TaskCategory) selectedObject), newTask);
					}
				}
			});
		}

		return newTask;
	}

	protected abstract SearchHitCollector getDuplicateSearchCollector(String description);

}
