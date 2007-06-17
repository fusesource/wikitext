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
package org.eclipse.mylyn.internal.tasks.ui.views;

import java.util.List;
import java.util.Set;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.mylyn.internal.tasks.core.ScheduledTaskContainer;
import org.eclipse.mylyn.internal.tasks.ui.TaskListColorsAndFonts;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.internal.tasks.ui.actions.ActiveTaskHistoryDropDownAction;
import org.eclipse.mylyn.internal.tasks.ui.actions.TaskWorkingSetAction;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.ITaskActivityListener;
import org.eclipse.mylyn.tasks.core.ITaskListChangeListener;
import org.eclipse.mylyn.tasks.core.TaskContainerDelta;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

/**
 * @author Mik Kersten
 * @author Leo Dos Santos - Task Working Set UI
 */
public class TaskListFilteredTree extends AbstractFilteredTree {

	private static final String LABEL_ACTIVE_NONE = "... ";

	private static final String LABEL_SETS_NONE = "All Tasks";

	private static final String LABEL_SETS_EDIT = "Edit Task Working Sets...";
	
	private static final String LABEL_SETS_MULTIPLE = "<multiple>";

	private Hyperlink workingSetLink;

	private Hyperlink activeTaskLink;

	private WorkweekProgressBar taskProgressBar;

	private int totalTasks;

	private int completeTime;

	private int completeTasks;

	private int incompleteTime;

	private IWorkingSet currentWorkingSet;

	public TaskListFilteredTree(Composite parent, int treeStyle, PatternFilter filter) {
		super(parent, treeStyle, filter);
	}
	
	@Override
	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		// Use a single Composite for the Tree to being able to use the
		// TreeColumnLayout. See Bug 177891 for more details.
		Composite container = new Composite(parent, SWT.None);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.verticalIndent = 0;
		gridData.horizontalIndent = 0;
		container.setLayoutData(gridData);
		container.setLayout(new TreeColumnLayout());
		return super.doCreateTreeViewer(container, style);
	}

	@Override
	protected Composite createProgressComposite(Composite container) {
		Composite progressComposite = new Composite(container, SWT.NONE);
		GridLayout progressLayout = new GridLayout(1, false);
		progressLayout.marginWidth = 4;
		progressLayout.marginHeight = 0;
		progressLayout.marginBottom = 0;
		progressLayout.horizontalSpacing = 0;
		progressLayout.verticalSpacing = 0;
		progressComposite.setLayout(progressLayout);
		progressComposite.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 4, 1));

		taskProgressBar = new WorkweekProgressBar(progressComposite);
		taskProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		updateTaskProgressBar();

		TasksUiPlugin.getTaskListManager().getTaskList().addChangeListener(new ITaskListChangeListener() {

			public void containersChanged(Set<TaskContainerDelta> containers) {
				for (TaskContainerDelta taskContainerDelta : containers) {
					if (taskContainerDelta.getContainer() instanceof AbstractTask) {
						updateTaskProgressBar();
						break;
					}
				}
			}
		});

		TasksUiPlugin.getTaskListManager().addActivityListener(new ITaskActivityListener() {

			public void activityChanged(ScheduledTaskContainer week) {
				updateTaskProgressBar();
			}

			public void calendarChanged() {
			}

			public void taskActivated(AbstractTask task) {
			}

			public void taskDeactivated(AbstractTask task) {
			}

			public void taskListRead() {
			}

			public void tasksActivated(List<AbstractTask> tasks) {
			}

		});
		return progressComposite;
	}

	private void updateTaskProgressBar() {
		if (taskProgressBar.isDisposed()) {
			return;
		}

		Set<AbstractTask> tasksThisWeek = TasksUiPlugin.getTaskListManager().getScheduledForThisWeek();
		totalTasks = tasksThisWeek.size();
		completeTime = 0;
		completeTasks = 0;
		incompleteTime = 0;
		for (AbstractTask task : tasksThisWeek) {
			if (task.isCompleted()) {
				completeTasks++;
				if (task.getEstimateTimeHours() > 0) {
					completeTime += task.getEstimateTimeHours();
				} else {
					completeTime++;
				}
			} else {
				if (task.getEstimateTimeHours() > 0) {
					incompleteTime += task.getEstimateTimeHours();
				} else {
					incompleteTime++;
				}
			}
		}

		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (PlatformUI.isWorkbenchRunning() && !taskProgressBar.isDisposed()) {
					taskProgressBar.reset(completeTime, (completeTime + incompleteTime));
					taskProgressBar.setToolTipText("Workweek Progress" + "\n     Hours: " + completeTime + " of "
							+ (completeTime + incompleteTime) + " estimated" + "\n     Tasks: " + completeTasks
							+ " of " + totalTasks + " scheduled");
				}
			}
		});
	}

	@Override
	protected Composite createWorkingSetComposite(Composite container) {
		final ImageHyperlink workingSetButton = new ImageHyperlink(container, SWT.FLAT);
		workingSetButton.setImage(TasksUiImages.getImage(TasksUiImages.TOOLBAR_ARROW_RIGHT));
		workingSetButton.setToolTipText("Select Task Working Set");
		
		final TaskWorkingSetAction action = new TaskWorkingSetAction();
		workingSetButton.addHyperlinkListener(new IHyperlinkListener() {

			public void linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent e) {
				action.getMenu(workingSetButton).setVisible(true);
			}

			public void linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent e) {
				workingSetButton.setImage(TasksUiImages.getImage(TasksUiImages.TOOLBAR_ARROW_DOWN));
			}

			public void linkExited(org.eclipse.ui.forms.events.HyperlinkEvent e) {
				workingSetButton.setImage(TasksUiImages.getImage(TasksUiImages.TOOLBAR_ARROW_RIGHT));
			}
		});
		
		workingSetLink = new Hyperlink(container, SWT.LEFT);
		workingSetLink.setText(LABEL_SETS_NONE);
		workingSetLink.setUnderlined(false);
		workingSetLink.setForeground(TaskListColorsAndFonts.COLOR_HYPERLINK);
		workingSetLink.addMouseTrackListener(new MouseTrackListener() {

			public void mouseEnter(MouseEvent e) {
				workingSetLink.setUnderlined(true);
			}

			public void mouseExit(MouseEvent e) {
				workingSetLink.setUnderlined(false);
			}

			public void mouseHover(MouseEvent e) {
			}
		});
		
		indicateActiveTaskWorkingSet();

		workingSetLink.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (currentWorkingSet != null) {
					action.run(currentWorkingSet);
				} else {
					action.run();
				}
			}
		});

//		ToolBarManager manager = new ToolBarManager(SWT.FLAT);
//		manager.add(action);
//		manager.createControl(container);

		return workingSetLink;
	}

	@Override
	protected Composite createStatusComposite(Composite container) {
		final ImageHyperlink activeTaskButton = new ImageHyperlink(container, SWT.LEFT);// SWT.ARROW | SWT.RIGHT);
		activeTaskButton.setImage(TasksUiImages.getImage(TasksUiImages.TOOLBAR_ARROW_RIGHT));
		activeTaskButton.setToolTipText("Select Active Task");
		 
		activeTaskLink = new Hyperlink(container, SWT.LEFT);
		activeTaskLink.setText(LABEL_ACTIVE_NONE);
		AbstractTask activeTask = TasksUiPlugin.getTaskListManager().getTaskList().getActiveTask();
		if (activeTask != null) {
			indicateActiveTask(activeTask);
		}
		
 		final ActiveTaskHistoryDropDownAction action = new ActiveTaskHistoryDropDownAction(TasksUiPlugin.getTaskListManager()
				.getTaskActivationHistory(), true);

		activeTaskButton.addHyperlinkListener(new IHyperlinkListener() {

			public void linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent e) {
				action.getMenu(activeTaskButton).setVisible(true);
			}

			public void linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent e) {
				activeTaskButton.setImage(TasksUiImages.getImage(TasksUiImages.TOOLBAR_ARROW_DOWN));
			}

			public void linkExited(org.eclipse.ui.forms.events.HyperlinkEvent e) {
				activeTaskButton.setImage(TasksUiImages.getImage(TasksUiImages.TOOLBAR_ARROW_RIGHT));
			}
		});

		activeTaskLink.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(MouseEvent e) {
				if (TaskListFilteredTree.super.filterText.getText().length() > 0) {
					TaskListFilteredTree.super.filterText.setText("");
					TaskListFilteredTree.this.textChanged();
				}
				if (TaskListView.getFromActivePerspective().getDrilledIntoCategory() != null) {
					TaskListView.getFromActivePerspective().goUpToRoot();
				}
				TasksUiUtil.refreshAndOpenTaskListElement((TasksUiPlugin.getTaskListManager().getTaskList().getActiveTask()));
			}

		});

//		ToolBarManager manager = new ToolBarManager(SWT.FLAT);
//		manager.add(action);
//		manager.createControl(container);

		return activeTaskLink;
	}

	public void indicateActiveTaskWorkingSet() {
		Set<IWorkingSet> activeSets = TaskListView.getActiveWorkingSets();
		if (filterComposite.isDisposed() || activeSets == null) {
			return;
		}

		if (activeSets.size() == 0) {
			workingSetLink.setText(LABEL_SETS_NONE);
			workingSetLink.setToolTipText(LABEL_SETS_EDIT);
			currentWorkingSet = null;
		} else if (activeSets.size() > 1) {
			workingSetLink.setText(LABEL_SETS_MULTIPLE);
			workingSetLink.setToolTipText(LABEL_SETS_EDIT);
			currentWorkingSet = null;
		} else {
			Object[] array = activeSets.toArray();
			IWorkingSet workingSet = (IWorkingSet) array[0];
			workingSetLink.setText(workingSet.getLabel());
			workingSetLink.setToolTipText(LABEL_SETS_EDIT);
			currentWorkingSet = workingSet;
		}
		filterComposite.layout();
	}

	public void indicateActiveTask(AbstractTask task) {
		if (filterComposite.isDisposed()) {
			return;
		}

		String text = task.getSummary() + " "; // hack for padding
		activeTaskLink.setText(text);
		activeTaskLink.setUnderlined(false);
		activeTaskLink.setForeground(TaskListColorsAndFonts.COLOR_HYPERLINK);
		activeTaskLink.setToolTipText("Open: " + task.getSummary());
		activeTaskLink.addMouseTrackListener(new MouseTrackListener() {

			public void mouseEnter(MouseEvent e) {
				activeTaskLink.setUnderlined(true);
			}

			public void mouseExit(MouseEvent e) {
				activeTaskLink.setUnderlined(false);
			}

			public void mouseHover(MouseEvent e) {
			}
		});
		
		filterComposite.layout();
	}

	public String getActiveTaskLabelText() {
		return activeTaskLink.getText();
	}

	public void indicateNoActiveTask() {
		if (filterComposite.isDisposed()) {
			return;
		}

		activeTaskLink.setText(LABEL_ACTIVE_NONE);
		activeTaskLink.setToolTipText("");
		filterComposite.layout();
	}

	@Override
	public void setFilterText(String string) {
		if (filterText != null) {
			filterText.setText(string);
			selectAll();
		}
	}
}
