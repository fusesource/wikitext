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

package org.eclipse.mylar.internal.tasks.ui.editors;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.mylar.context.core.ContextCorePlugin;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.internal.core.util.DateUtil;
import org.eclipse.mylar.internal.tasks.ui.RetrieveTitleFromUrlJob;
import org.eclipse.mylar.internal.tasks.ui.TaskListImages;
import org.eclipse.mylar.internal.tasks.ui.actions.NewLocalTaskAction;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylar.monitor.ui.MylarMonitorUiPlugin;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.AbstractTaskContainer;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskListChangeListener;
import org.eclipse.mylar.tasks.core.Task;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.core.Task.PriorityLevel;
import org.eclipse.mylar.tasks.ui.DatePicker;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.editors.TaskEditor;
import org.eclipse.mylar.tasks.ui.editors.TaskEditorInput;
import org.eclipse.mylar.tasks.ui.editors.TaskFormPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

/**
 * @author Mik Kersten
 * @author Ken Sueda (initial prototype)
 * @author Rob Elves
 */
public class TaskPlanningEditor extends TaskFormPage {

	private static final String LABEL_DUE = "Due:";

	public static final String PLANNING_EDITOR_ID = "org.eclipse.mylar.editors.planning";

	private static final String LABEL_SCHEDULE = "Scheduled for:";

	private static final String DESCRIPTION_ESTIMATED = "Time that the task has been actively worked on in Eclipse.\n Inactivity timeout is "
			+ MylarMonitorUiPlugin.getDefault().getInactivityTimeout() / (60 * 1000) + " minutes.";

	public static final String LABEL_INCOMPLETE = "Incomplete";

	public static final String LABEL_COMPLETE = "Complete";

	private static final String LABEL_PLAN = "Personal Planning";

	private static final String NO_TIME_ELAPSED = "0 seconds";

	// private static final String LABEL_OVERVIEW = "Task Info";

	private static final String LABEL_NOTES = "Notes";

	private DatePicker dueDatePicker;

	private DatePicker datePicker;

	private ITask task;

	private Composite editorComposite;

	protected static final String CONTEXT_MENU_ID = "#MylarPlanningEditor";

	private Button removeReminder;

	private Text pathText;

	private Text endDate;

	private ScrolledForm form;

	private Text summary;

	private Text issueReportURL;

	private CCombo priorityCombo;

	private CCombo statusCombo;

	private TextViewer noteEditor;

	private Spinner estimated;

	private Button getDescButton;

	private TaskEditor parentEditor = null;

	private ITaskListChangeListener TASK_LIST_LISTENER = new ITaskListChangeListener() {

		public void localInfoChanged(final ITask updateTask) {
			if (updateTask != null && task != null
					&& updateTask.getHandleIdentifier().equals(task.getHandleIdentifier())) {
				if (PlatformUI.getWorkbench() != null && !PlatformUI.getWorkbench().isClosing()) {
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						public void run() {

							updateTaskData(updateTask);
						}

					});
				}
			}
		}

		public void repositoryInfoChanged(ITask task) {
			localInfoChanged(task);
		}

		public void taskMoved(ITask task, AbstractTaskContainer fromContainer, AbstractTaskContainer toContainer) {
			// ignore
		}

		public void taskDeleted(ITask task) {
			// ignore
		}

		public void containerAdded(AbstractTaskContainer container) {
			// ignore
		}

		public void containerDeleted(AbstractTaskContainer container) {
			// ignore
		}

		public void taskAdded(ITask task) {
			// ignore
		}

		public void containerInfoChanged(AbstractTaskContainer container) {
			// ignore
		}

	};

	private FormToolkit toolkit;

	public TaskPlanningEditor(FormEditor editor) {
		super(editor, PLANNING_EDITOR_ID, "Planning");
		TasksUiPlugin.getTaskListManager().getTaskList().addChangeListener(TASK_LIST_LISTENER);
	}

	/** public for testing */
	public void updateTaskData(final ITask updateTask) {
		if (datePicker != null && !datePicker.isDisposed()) {
			if (updateTask.getScheduledForDate() != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(updateTask.getScheduledForDate());
				datePicker.setDate(cal);
			} else {
				datePicker.setDate(null);
			}
		}

		if (summary == null)
			return;
		if (!summary.isDisposed()) {
			if (!summary.getText().equals(updateTask.getSummary())) {
				boolean wasDirty = TaskPlanningEditor.this.isDirty;
				summary.setText(updateTask.getSummary());
				TaskPlanningEditor.this.markDirty(wasDirty);
			}
			if (parentEditor != null) {
				parentEditor.changeTitle();
			}
			if (form != null && updateTask != null) {
				form.setText(updateTask.getSummary());
			}
		}

		if (!priorityCombo.isDisposed()) {
			PriorityLevel level = PriorityLevel.fromString(updateTask.getPriority());
			if (level != null) {
				int prioritySelectionIndex = priorityCombo.indexOf(level.getDescription());
				priorityCombo.select(prioritySelectionIndex);
			}
		}
		if (!statusCombo.isDisposed()) {
			if (task.isCompleted()) {
				statusCombo.select(0);
			} else {
				statusCombo.select(1);
			}
		}
		if (!(updateTask instanceof AbstractRepositoryTask) && !endDate.isDisposed()) {
			endDate.setText(getTaskDateString(updateTask));
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (!(task instanceof AbstractRepositoryTask)) {
			String label = summary.getText();
			// task.setDescription(label);
			TasksUiPlugin.getTaskListManager().getTaskList().renameTask((Task) task, label);

			// TODO: refactor mutation into TaskList?
			task.setTaskUrl(issueReportURL.getText());
			String priorityDescription = priorityCombo.getItem(priorityCombo.getSelectionIndex());
			PriorityLevel level = PriorityLevel.fromDescription(priorityDescription);
			if (level != null) {
				task.setPriority(level.toString());
			}
			if (statusCombo.getSelectionIndex() == 0) {
				task.setCompleted(true);
			} else {
				task.setCompleted(false);
			}
		}

		String note = noteEditor.getTextWidget().getText();// notes.getText();
		task.setNotes(note);
		task.setEstimatedTimeHours(estimated.getSelection());
		if (datePicker != null && datePicker.getDate() != null) {
			TasksUiPlugin.getTaskListManager().setScheduledFor(task, datePicker.getDate().getTime());
		} else {
			TasksUiPlugin.getTaskListManager().setScheduledFor(task, null);
		}
		if (dueDatePicker != null && dueDatePicker.getDate() != null) {
			TasksUiPlugin.getTaskListManager().setDueDate(task, dueDatePicker.getDate().getTime());
		} else {
			TasksUiPlugin.getTaskListManager().setDueDate(task, null);
		}

		if (parentEditor != null) {
			parentEditor.notifyTaskChanged();
		}
		markDirty(false);
	}

	@Override
	public void doSaveAs() {
		// don't support saving as
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		TaskEditorInput taskEditorInput = (TaskEditorInput) getEditorInput();

		task = taskEditorInput.getTask();

		form = managedForm.getForm();
		toolkit = managedForm.getToolkit();
		form.setImage(TaskListImages.getImage(TaskListImages.CALENDAR));
		toolkit.decorateFormHeading(form.getForm());

		editorComposite = form.getBody();
		GridLayout editorLayout = new GridLayout();
		editorLayout.verticalSpacing = 0;
		editorComposite.setLayout(editorLayout);
		editorComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		// try {
		if (task instanceof AbstractRepositoryTask) {
			form.setText("Planning");
		} else {
			createSummarySection(editorComposite);
			form.setText("Task: " + task.getSummary());
		}
		createPlanningSection(editorComposite);
		createNotesSection(editorComposite);

		if (summary != null && NewLocalTaskAction.DESCRIPTION_DEFAULT.equals(summary.getText())) {
			// summary.setSelection(0, summary.getText().length());
			summary.setFocus();
		} else if (summary != null) {
			summary.setFocus();
		}
	}

	@Override
	public void setFocus() {
		// form.setFocus();
		if (summary != null && !summary.isDisposed()) {
			summary.setFocus();
		}
	}

	public Control getControl() {
		return form;
	}

	private Text addNameValueComp(Composite parent, String label, String value, int style) {
		Composite nameValueComp = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 3;
		nameValueComp.setLayout(layout);
		toolkit.createLabel(nameValueComp, label, SWT.NONE).setForeground(
				toolkit.getColors().getColor(IFormColors.TITLE));
		Text text;
		if ((SWT.READ_ONLY & style) == SWT.READ_ONLY) {
			text = new Text(nameValueComp, style);
			toolkit.adapt(text, true, true);
			text.setText(value);
		} else {
			text = toolkit.createText(nameValueComp, value, style);
		}
		return text;
	}

	private void createSummarySection(Composite parent) {

		// Summary
		Composite summaryComposite = toolkit.createComposite(parent);
		GridLayout summaryLayout = new GridLayout(2, false);
		summaryLayout.verticalSpacing = 0;
		summaryLayout.marginHeight = 0;
		summaryLayout.marginLeft = 5;
		summaryComposite.setLayout(summaryLayout);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(summaryComposite);

		summary = toolkit.createText(summaryComposite, task.getSummary(), SWT.NONE);
		summary.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		GridData summaryGridData = new GridData(GridData.FILL_HORIZONTAL);
		summaryGridData.horizontalSpan = 7;
		summary.setLayoutData(summaryGridData);

		if (task instanceof AbstractRepositoryTask) {
			summary.setEnabled(false);
		} else {
			summary.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					markDirty(true);
				}
			});
		}

		Composite statusComposite = toolkit.createComposite(parent);
		GridLayout compLayout = new GridLayout(7, false);
		compLayout.verticalSpacing = 0;
		compLayout.horizontalSpacing = 5;
		compLayout.marginHeight = 3;
		statusComposite.setLayout(compLayout);
		statusComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite nameValueComp = toolkit.createComposite(statusComposite);
		GridLayout nameValueLayout = new GridLayout(2, false);
		nameValueLayout.marginHeight = 3;
		nameValueComp.setLayout(nameValueLayout);
		toolkit.createLabel(nameValueComp, "Priority:").setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		priorityCombo = new CCombo(nameValueComp, SWT.FLAT);
		priorityCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		toolkit.paintBordersFor(nameValueComp);

		// Populate the combo box with priority levels
		for (String priorityLevel : TaskListView.PRIORITY_LEVEL_DESCRIPTIONS) {
			priorityCombo.add(priorityLevel);
		}

		PriorityLevel level = PriorityLevel.fromString(task.getPriority());
		if (level != null) {
			int prioritySelectionIndex = priorityCombo.indexOf(level.getDescription());
			priorityCombo.select(prioritySelectionIndex);
		}

		if (task instanceof AbstractRepositoryTask) {
			priorityCombo.setEnabled(false);
		} else {
			priorityCombo.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					TaskPlanningEditor.this.markDirty(true);

				}
			});
		}

		nameValueComp = toolkit.createComposite(statusComposite);
		nameValueComp.setLayout(new GridLayout(2, false));
		toolkit.createLabel(nameValueComp, "Status:").setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		statusCombo = new CCombo(nameValueComp, SWT.FLAT);
		statusCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		toolkit.paintBordersFor(nameValueComp);
		statusCombo.add(LABEL_COMPLETE);
		statusCombo.add(LABEL_INCOMPLETE);
		if (task.isCompleted()) {
			statusCombo.select(0);
		} else {
			statusCombo.select(1);
		}
		if (task instanceof AbstractRepositoryTask) {
			statusCombo.setEnabled(false);
		} else {
			statusCombo.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					if (statusCombo.getSelectionIndex() == 0) {
						task.setCompleted(true);
					} else {
						task.setCompleted(false);
					}
					TaskPlanningEditor.this.markDirty(true);
				}
			});
		}

		String creationDateString = "";
		try {
			creationDateString = DateFormat.getDateInstance(DateFormat.LONG).format(task.getCreationDate());
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "Could not format creation date", true);
		}
		addNameValueComp(statusComposite, "Created:", creationDateString, SWT.FLAT | SWT.READ_ONLY);

		nameValueComp = makeComposite(statusComposite, 3);
		Label label = toolkit.createLabel(nameValueComp, LABEL_DUE);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		dueDatePicker = new DatePicker(nameValueComp, SWT.FLAT, DatePicker.LABEL_CHOOSE);

		Calendar calendar = Calendar.getInstance();
		if (task.getDueDate() != null) {
			calendar.setTime(task.getDueDate());
			dueDatePicker.setDate(calendar);
		}

		dueDatePicker.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		dueDatePicker.addPickerSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent arg0) {
				TaskPlanningEditor.this.markDirty(true);
			}
		});

		dueDatePicker.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		toolkit.adapt(dueDatePicker, true, true);
		toolkit.paintBordersFor(nameValueComp);
		Button clearDueDate = toolkit.createButton(nameValueComp, "", SWT.PUSH | SWT.CENTER);
		clearDueDate.setImage(TaskListImages.getImage(TaskListImages.REMOVE));

		clearDueDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dueDatePicker.setDate(null);
				TaskPlanningEditor.this.markDirty(true);
			}
		});

		String completionDateString = "";
		if (task.isCompleted()) {
			completionDateString = getTaskDateString(task);			
		}
		endDate = addNameValueComp(statusComposite, "Completed:", completionDateString, SWT.FLAT | SWT.READ_ONLY);
		// URL
		Composite urlComposite = toolkit.createComposite(parent);
		GridLayout urlLayout = new GridLayout(3, false);
		urlLayout.verticalSpacing = 0;
		urlLayout.marginHeight = 2;
		urlLayout.marginLeft = 5;
		urlComposite.setLayout(urlLayout);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(urlComposite);

		label = toolkit.createLabel(urlComposite, "URL:");
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		issueReportURL = toolkit.createText(urlComposite, task.getTaskUrl(), SWT.FLAT);
		issueReportURL.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		if (task instanceof AbstractRepositoryTask) {
			issueReportURL.setEditable(false);
		} else {
			issueReportURL.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					markDirty(true);
				}
			});
		}

		getDescButton = toolkit.createButton(urlComposite, "Get Description", SWT.FLAT | SWT.PUSH);
		getDescButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		setButtonStatus();

		issueReportURL.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				setButtonStatus();
			}

			public void keyReleased(KeyEvent e) {
				setButtonStatus();
			}
		});

		getDescButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				retrieveTaskDescription(issueReportURL.getText());
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		toolkit.paintBordersFor(urlComposite);
		toolkit.paintBordersFor(statusComposite);
	}

	/**
	 * Attempts to set the task pageTitle to the title from the specified url
	 */
	protected void retrieveTaskDescription(final String url) {

		try {
			RetrieveTitleFromUrlJob job = new RetrieveTitleFromUrlJob(issueReportURL.getText()) {

				@Override
				protected void setTitle(final String pageTitle) {
					summary.setText(pageTitle);
					TaskPlanningEditor.this.markDirty(true);
				}

			};
			job.schedule();

		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "could not open task web page", false);
		}
	}

	/**
	 * Sets the Get Description button enabled or not depending on whether there
	 * is a URL specified
	 */
	protected void setButtonStatus() {
		String url = issueReportURL.getText();

		if (url.length() > 10 && (url.startsWith("http://") || url.startsWith("https://"))) {
			// String defaultPrefix =
			// ContextCorePlugin.getDefault().getPreferenceStore().getString(
			// TaskListPreferenceConstants.DEFAULT_URL_PREFIX);
			// if (url.equals(defaultPrefix)) {
			// getDescButton.setEnabled(false);
			// } else {
			getDescButton.setEnabled(true);
			// }
		} else {
			getDescButton.setEnabled(false);
		}
	}

	private void createPlanningSection(Composite parent) {

		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR | Section.TWISTIE);
		section.setText(LABEL_PLAN);
		section.setLayout(new GridLayout());
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		section.setExpanded(true);
		section.addExpansionListener(new IExpansionListener() {
			public void expansionStateChanging(ExpansionEvent e) {
				form.reflow(true);
			}

			public void expansionStateChanged(ExpansionEvent e) {
				form.reflow(true);
			}
		});

		Composite sectionClient = toolkit.createComposite(section);
		section.setClient(sectionClient);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.horizontalSpacing = 15;
		layout.makeColumnsEqualWidth = false;
		sectionClient.setLayout(layout);
		GridData clientDataLayout = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		sectionClient.setLayoutData(clientDataLayout);

		Composite nameValueComp = makeComposite(sectionClient, 3);
		Label label = toolkit.createLabel(nameValueComp, LABEL_SCHEDULE);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		datePicker = new DatePicker(nameValueComp, SWT.FLAT, DatePicker.LABEL_CHOOSE);

		Calendar calendar = Calendar.getInstance();
		if (task.getScheduledForDate() != null) {
			calendar.setTime(task.getScheduledForDate());
			datePicker.setDate(calendar);
		}

		datePicker.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		datePicker.addPickerSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent arg0) {
				task.setReminded(false);
				TaskPlanningEditor.this.markDirty(true);
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {
				// ignore
			}
		});

		datePicker.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		toolkit.adapt(datePicker, true, true);
		toolkit.paintBordersFor(nameValueComp);

		removeReminder = toolkit.createButton(nameValueComp, "", SWT.PUSH | SWT.CENTER);
		removeReminder.setImage(TaskListImages.getImage(TaskListImages.REMOVE));
		removeReminder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				datePicker.setDate(null);
				task.setReminded(false);
				TaskPlanningEditor.this.markDirty(true);
			}
		});

		// Estimated time
		nameValueComp = makeComposite(sectionClient, 3);
		label = toolkit.createLabel(nameValueComp, "Estimated time:");
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		estimated = new Spinner(nameValueComp, SWT.NONE);
		toolkit.adapt(estimated, true, true);
		estimated.setSelection(task.getEstimateTimeHours());
		estimated.setDigits(0);
		estimated.setMaximum(100);
		estimated.setMinimum(0);
		estimated.setIncrement(1);
		estimated.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				TaskPlanningEditor.this.markDirty(true);
			}
		});

		estimated.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		toolkit.paintBordersFor(nameValueComp);
		GridData estimatedDataLayout = new GridData();
		estimatedDataLayout.widthHint = 30;
		estimated.setLayoutData(estimatedDataLayout);

		label = toolkit.createLabel(nameValueComp, "hours ");
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		// Active Time
		nameValueComp = makeComposite(sectionClient, 3);
		// GridDataFactory.fillDefaults().span(2, 1).align(SWT.LEFT,
		// SWT.DEFAULT).applyTo(nameValueComp);
		label = toolkit.createLabel(nameValueComp, "Active time:");
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		label.setToolTipText(DESCRIPTION_ESTIMATED);

		String elapsedTimeString = NO_TIME_ELAPSED;
		try {
			elapsedTimeString = DateUtil.getFormattedDuration(TasksUiPlugin.getTaskListManager().getElapsedTime(task),
					true);
			if (elapsedTimeString.equals(""))
				elapsedTimeString = NO_TIME_ELAPSED;
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "Could not format elapsed time", true);
		}

		final Text elapsedTimeText = new Text(nameValueComp, SWT.READ_ONLY | SWT.FLAT);
		elapsedTimeText.setText(elapsedTimeString);
		GridData td = new GridData(GridData.FILL_HORIZONTAL);
		td.widthHint = 120;
		elapsedTimeText.setLayoutData(td);
		elapsedTimeText.setEditable(false);

		// Refresh Button
		Button timeRefresh = toolkit.createButton(nameValueComp, "Refresh", SWT.PUSH | SWT.CENTER);
		// timeRefresh.setImage(TaskListImages.getImage(TaskListImages.REFRESH));

		timeRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String elapsedTimeString = NO_TIME_ELAPSED;
				try {
					elapsedTimeString = DateUtil.getFormattedDuration(TasksUiPlugin.getTaskListManager()
							.getElapsedTime(task), true);
					if (elapsedTimeString.equals("")) {
						elapsedTimeString = NO_TIME_ELAPSED;
					}

				} catch (RuntimeException e1) {
					MylarStatusHandler.fail(e1, "Could not format elapsed time", true);
				}
				elapsedTimeText.setText(elapsedTimeString);
			}
		});

		toolkit.paintBordersFor(sectionClient);
	}

	private Composite makeComposite(Composite parent, int col) {
		Composite nameValueComp = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 3;
		nameValueComp.setLayout(layout);
		return nameValueComp;
	}

	private void createNotesSection(Composite parent) {
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		section.setText(LABEL_NOTES);
		section.setExpanded(true);
		section.setLayout(new GridLayout());
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.addExpansionListener(new IExpansionListener() {
			public void expansionStateChanging(ExpansionEvent e) {
				form.reflow(true);
			}

			public void expansionStateChanged(ExpansionEvent e) {
				form.reflow(true);
			}
		});
		Composite container = toolkit.createComposite(section);
		section.setClient(container);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		TaskRepository repository = null;
		if (task instanceof AbstractRepositoryTask) {
			AbstractRepositoryTask repositoryTask = (AbstractRepositoryTask) task;
			repository = TasksUiPlugin.getRepositoryManager().getRepository(repositoryTask.getRepositoryKind(),
					repositoryTask.getRepositoryUrl());
		}

		noteEditor = addTextEditor(repository, container, task.getNotes(), true, SWT.FLAT | SWT.MULTI | SWT.WRAP
				| SWT.V_SCROLL);

		noteEditor.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		noteEditor.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		noteEditor.setEditable(true);

		noteEditor.getTextWidget().addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				markDirty(true);
			}
		});

		// commentViewer.addSelectionChangedListener(new
		// ISelectionChangedListener() {
		//
		// public void selectionChanged(SelectionChangedEvent event) {
		// getSite().getSelectionProvider().setSelection(commentViewer.getSelection());
		//				
		// }});

		toolkit.paintBordersFor(container);
	}

	private String getTaskDateString(ITask task) {

		if (task == null)
			return "";
		if (task.getCompletionDate() == null)
			return "";

		String completionDateString = "";
		try {
			completionDateString = DateFormat.getDateInstance(DateFormat.LONG).format(task.getCompletionDate());
		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "Could not format date", true);
			return completionDateString;
		}
		return completionDateString;
	}

	// TODO: unused, delete?
	void createResourcesSection(Composite parent) {
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR | Section.TWISTIE);
		section.setText("Resources");
		section.setLayout(new GridLayout());
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		section.addExpansionListener(new IExpansionListener() {
			public void expansionStateChanging(ExpansionEvent e) {
				form.reflow(true);
			}

			public void expansionStateChanged(ExpansionEvent e) {
				form.reflow(true);
			}
		});

		Composite container = toolkit.createComposite(section);
		section.setClient(container);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		toolkit.createLabel(container, "Task context file:");
		File contextFile = ContextCorePlugin.getContextManager().getFileForContext(task.getHandleIdentifier());
		if (contextFile != null) {
			pathText = toolkit.createText(container, contextFile.getAbsolutePath(), SWT.NONE);
			pathText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
			GridDataFactory.fillDefaults().hint(400, SWT.DEFAULT).applyTo(pathText);
			pathText.setEditable(false);
			pathText.setEnabled(true);
		}
		toolkit.paintBordersFor(container);
	}

	public void setParentEditor(TaskEditor parentEditor) {
		this.parentEditor = parentEditor;
	}

	@Override
	public void dispose() {
		TasksUiPlugin.getTaskListManager().getTaskList().removeChangeListener(TASK_LIST_LISTENER);
	}

	@Override
	public String toString() {
		return "(info editor for task: " + task + ")";
	}

	/** for testing - should cause dirty state */
	public void setNotes(String notes) {
		this.noteEditor.getTextWidget().setText(notes);
	}

	/** for testing - should cause dirty state */
	public void setDescription(String desc) {
		this.summary.setText(desc);
	}

	/** for testing */
	public String getDescription() {
		return this.summary.getText();
	}

	/** for testing */
	public String getFormTitle() {
		return form.getText();
	}

}
