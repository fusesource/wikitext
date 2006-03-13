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

package org.eclipse.mylar.internal.tasklist.ui.views;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.mylar.internal.core.dt.MylarWebRef;
import org.eclipse.mylar.internal.tasklist.planner.ui.ReminderCellEditor;
import org.eclipse.mylar.internal.tasklist.ui.TaskListColorsAndFonts;
import org.eclipse.mylar.internal.tasklist.ui.actions.OpenTaskListElementAction;
import org.eclipse.mylar.provisional.tasklist.AbstractQueryHit;
import org.eclipse.mylar.provisional.tasklist.DateRangeActivityDelegate;
import org.eclipse.mylar.provisional.tasklist.DateRangeContainer;
import org.eclipse.mylar.provisional.tasklist.ITask;
import org.eclipse.mylar.provisional.tasklist.ITaskActivityListener;
import org.eclipse.mylar.provisional.tasklist.ITaskListChangeListener;
import org.eclipse.mylar.provisional.tasklist.AbstractTaskContainer;
import org.eclipse.mylar.provisional.tasklist.MylarTaskListPlugin;
import org.eclipse.mylar.provisional.tasklist.TaskListManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.themes.IThemeManager;

/**
 * @author Rob Elves
 */
public class TaskActivityView extends ViewPart {

	public static final String ID = "org.eclipse.mylar.tasklist.activity";

	private static TaskActivityView INSTANCE;

	private OpenTaskListElementAction openTaskEditor;

	// private OpenTaskInExternalBrowserAction openUrlInExternal;

	private String[] columnNames = new String[] { " ", " !", "Description", "Elapsed", "Estimated", "Reminder" };

	private int[] columnWidths = new int[] { 60, 12, 160, 60, 70, 100 };

	private TreeColumn[] columns;

	private TaskActivityLabelProvider taskHistoryTreeLabelProvider;

	private TreeViewer treeViewer;

	private TaskActivityContentProvider taskActivityTableContentProvider;

	private IThemeManager themeManager;
	
	private final IPropertyChangeListener THEME_CHANGE_LISTENER = new IPropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(IThemeManager.CHANGE_CURRENT_THEME)
					|| event.getProperty().equals(TaskListColorsAndFonts.THEME_COLOR_ID_TASKLIST_CATEGORY)) {
				taskHistoryTreeLabelProvider.setCategoryBackgroundColor(themeManager.getCurrentTheme().getColorRegistry().get(TaskListColorsAndFonts.THEME_COLOR_ID_TASKLIST_CATEGORY));
				refresh();
			} 
		}
	};
	
	/**
	 * TODO: need lazier refresh policy.
	 */
	private final ITaskActivityListener ACTIVITY_LISTENER = new ITaskActivityListener() {

		public void taskActivated(ITask task) {
			refresh();
			// TaskActivityView.this.treeViewer.refresh(task);
		}

		public void tasksActivated(List<ITask> tasks) {
			for (ITask task : tasks) {
				taskActivated(task);
			}
		}

		public void taskDeactivated(ITask task) {
			// don't need to refresh here
			// TaskActivityView.this.treeViewer.refresh(task);
		}

		public void activityChanged(DateRangeContainer week) {
			refresh();
//			TaskActivityView.this.treeViewer.refresh(week);
		}

		public void tasklistRead() {
			refresh();
		}
	};

	private ITaskListChangeListener TASK_CHANGE_LISTENER = new ITaskListChangeListener() {

		public void localInfoChanged(final ITask updateTask) {
			refresh();
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
	};

	public static TaskActivityView openInActivePerspective() {
		try {
			return (TaskActivityView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ID);
		} catch (Exception e) {
			return null;
		}
	}

	public TaskActivityView() {
		INSTANCE = this;
		MylarTaskListPlugin.getTaskListManager().addActivityListener(ACTIVITY_LISTENER);
		MylarTaskListPlugin.getTaskListManager().getTaskList().addChangeListener(TASK_CHANGE_LISTENER);
	}

	@Override
	public void dispose() {
		super.dispose();
		MylarTaskListPlugin.getTaskListManager().removeActivityListener(ACTIVITY_LISTENER);
		MylarTaskListPlugin.getTaskListManager().getTaskList().removeChangeListener(TASK_CHANGE_LISTENER);
	}

	@Override
	public void createPartControl(Composite parent) {
		themeManager = getSite().getWorkbenchWindow().getWorkbench().getThemeManager();
		themeManager.addPropertyChangeListener(THEME_CHANGE_LISTENER);
		
		int treeStyle = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION;
		treeViewer = new TreeViewer(parent, treeStyle);

		getViewer().getTree().setHeaderVisible(true);
		getViewer().getTree().setLinesVisible(true);
		getViewer().setColumnProperties(columnNames);
		getViewer().setUseHashlookup(true);

		columns = new TreeColumn[columnNames.length];
		for (int i = 0; i < columnNames.length; i++) {
			columns[i] = new TreeColumn(getViewer().getTree(), SWT.LEFT);
			columns[i].setText(columnNames[i]);
			columns[i].setWidth(columnWidths[i]);
			// final int index = i;
			// columns[i].addSelectionListener(new SelectionAdapter() {
			// @Override
			// public void widgetSelected(SelectionEvent e) {
			// sortIndex = index;
			// sortDirection *= DEFAULT_SORT_DIRECTION;
			// getViewer().setSorter(new TaskActivityTableSorter());
			// }
			// });
			columns[i].addControlListener(new ControlListener() {
				public void controlResized(ControlEvent e) {
					for (int j = 0; j < columnWidths.length; j++) {
						if (columns[j].equals(e.getSource())) {
							columnWidths[j] = columns[j].getWidth();
						}
					}
				}

				public void controlMoved(ControlEvent e) {
					// don't care if the control is moved
				}
			});
		}

		IThemeManager themeManager = getSite().getWorkbenchWindow().getWorkbench().getThemeManager();
		Color categoryBackground = themeManager.getCurrentTheme().getColorRegistry().get(
				TaskListColorsAndFonts.THEME_COLOR_ID_TASKLIST_CATEGORY);
		
		taskHistoryTreeLabelProvider = new TaskActivityLabelProvider(new TaskElementLabelProvider(), PlatformUI
				.getWorkbench().getDecoratorManager().getLabelDecorator(), categoryBackground);

		getViewer().setSorter(new TaskActivityTableSorter());
		taskActivityTableContentProvider = new TaskActivityContentProvider(MylarTaskListPlugin.getTaskListManager());

		getViewer().setContentProvider(taskActivityTableContentProvider);
		getViewer().setLabelProvider(taskHistoryTreeLabelProvider);
		getViewer().setInput(getViewSite());
		createCellEditorListener();
		makeActions();
		initDrop();
		hookOpenAction();

	}
	
	@MylarWebRef(name = "Drag and drop article", url = "http://www.eclipse.org/articles/Article-Workbench-DND/drag_drop.html")
	private void initDrop() {
		Transfer[] types = new Transfer[] { TextTransfer.getInstance() };

		treeViewer.addDropSupport(DND.DROP_MOVE, types, new ViewerDropAdapter(treeViewer) {
			{
				setFeedbackEnabled(false);
			}

			@Override
			public boolean performDrop(Object data) {

				IStructuredSelection selection = ((IStructuredSelection) TaskListView.getDefault().getViewer()
						.getSelection());

				Object target = getCurrentTarget();
				DateRangeContainer container;
				Calendar reminderCalendar;
				if (target instanceof DateRangeContainer) {
					container = (DateRangeContainer) target;
					reminderCalendar = container.getStart();
				} else if (target instanceof DateRangeActivityDelegate) {
					DateRangeActivityDelegate dateRangeActivityDelegate = (DateRangeActivityDelegate) target;
					reminderCalendar = dateRangeActivityDelegate.getDateRangeContainer().getStart();
				} else {
					return false;
				}

				for (Iterator iter = selection.iterator(); iter.hasNext();) {
					Object selectedObject = iter.next();
					ITask task = null;
					if (selectedObject instanceof ITask) {
						task = (ITask) selectedObject;
					} else if (selectedObject instanceof AbstractQueryHit) {
						task = ((AbstractQueryHit) selectedObject).getOrCreateCorrespondingTask();
					}
					if (task != null) {
						MylarTaskListPlugin.getTaskListManager().setReminder(task, reminderCalendar.getTime());						
					}
				}

				// treeViewer.refresh();
				return true;
			}

			@Override
			public boolean validateDrop(Object targetObject, int operation, TransferData transferType) {
				Object selectedObject = ((IStructuredSelection) TaskListView.getDefault().getViewer().getSelection())
						.getFirstElement();

				if (selectedObject instanceof AbstractTaskContainer) {
					return false;
				}

				Object target = getCurrentTarget();
				DateRangeContainer dateRangeContainer = null;
				if (target instanceof DateRangeContainer) {
					dateRangeContainer = (DateRangeContainer) target;
				} else if (target instanceof DateRangeActivityDelegate) {
					DateRangeActivityDelegate dateRangeActivityDelegate = (DateRangeActivityDelegate) target;
					dateRangeContainer = dateRangeActivityDelegate.getDateRangeContainer();
				}

				if (dateRangeContainer != null && (dateRangeContainer.isPresent() || dateRangeContainer.isFuture())) {
					return true;
				}
				return false;
			}
		});
	}

	private void makeActions() {
		openTaskEditor = new OpenTaskListElementAction(this.getViewer());
		// openUrlInExternal = new OpenTaskInExternalBrowserAction();
	}

	private void hookOpenAction() {
		getViewer().addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				openTaskEditor.run();
			}
		});
	}

	public static TaskActivityView getDefault() {
		return INSTANCE;
	}

	private TreeViewer getViewer() {
		return treeViewer;
	}

	private void refresh() {	
		if (PlatformUI.getWorkbench() != null && !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (getViewer().getControl() != null && !getViewer().getControl().isDisposed()) {
						TaskActivityView.this.treeViewer.refresh();
					}
				}
			});
		}
	}	

	public ITask getSelectedTask() {
		ISelection selection = getViewer().getSelection();
		if (selection.isEmpty())
			return null;
		if (selection instanceof StructuredSelection) {
			StructuredSelection structuredSelection = (StructuredSelection) selection;
			Object element = structuredSelection.getFirstElement();
			if (element instanceof ITask) {
				return (ITask) structuredSelection.getFirstElement();
			}
		}
		return null;
	}

	@Override
	public void setFocus() {
		// ignore
	}

	private void createCellEditorListener() {
		CellEditor[] editors = new CellEditor[columnNames.length];
		final ComboBoxCellEditor estimateEditor = new ComboBoxCellEditor(treeViewer.getTree(),
				TaskListManager.ESTIMATE_TIMES, SWT.READ_ONLY);
		final ReminderCellEditor reminderEditor = new ReminderCellEditor(treeViewer.getTree());
		editors[0] = null; // not used
		editors[1] = null;// not used
		editors[2] = null;// not used
		editors[3] = null;// not used
		editors[4] = estimateEditor;
		editors[5] = reminderEditor;
		reminderEditor.addListener(new ICellEditorListener() {
			public void applyEditorValue() {
				Object selection = ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
				if (selection instanceof DateRangeActivityDelegate) {
// ((ITask) selection).setReminderDate(reminderEditor.getReminderDate());
// treeViewer.refresh();
					DateRangeActivityDelegate dateRangeActivityDelegate = (DateRangeActivityDelegate)selection;
					MylarTaskListPlugin.getTaskListManager().setReminder(dateRangeActivityDelegate.getCorrespondingTask(), reminderEditor.getReminderDate());
					// MylarTaskListPlugin.getTaskListManager().notifyLocalInfoChanged((ITask)
					// selection);
				}
			}

			public void cancelEditor() {
			}

			public void editorValueChanged(boolean oldValidState, boolean newValidState) {
			}

		});
		estimateEditor.addListener(new ICellEditorListener() {
			public void applyEditorValue() {
				Object selection = ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
				if (selection instanceof ITask) {
					ITask task = (ITask) selection;
					int estimate = (Integer) estimateEditor.getValue();
					if (estimate == -1) {
						estimate = 0;
					}
					task.setEstimatedTimeHours(estimate);
					// updateLabels();
					refresh();
//					treeViewer.refresh();
				}
			}

			public void cancelEditor() {
			}

			public void editorValueChanged(boolean oldValidState, boolean newValidState) {
			}

		});
		treeViewer.setCellEditors(editors);
		getViewer().setCellModifier(new TaskActivityCellModifier(treeViewer));
	}

	private class TaskActivityCellModifier implements ICellModifier {

		private TreeViewer treeViewer;

		public TaskActivityCellModifier(TreeViewer tableViewer) {
			this.treeViewer = tableViewer;
		}

		public boolean canModify(Object element, String property) {
			int columnIndex = Arrays.asList(columnNames).indexOf(property);
			if (columnIndex == 4 || columnIndex == 5) {
				return true;
			}
			return false;
		}

		public Object getValue(Object element, String property) {
			if (element instanceof ITask) {
				int columnIndex = Arrays.asList(columnNames).indexOf(property);
				if (element instanceof ITask) {
					if (columnIndex == 5) {
						if (((ITask) element).getReminderDate() != null) {
							return DateFormat.getDateInstance(DateFormat.MEDIUM).format(
									((ITask) element).getReminderDate());
						} else {
							return null;
						}
					} else if (columnIndex == 4) {
						return new Integer(Arrays.asList(TaskListManager.ESTIMATE_TIMES).indexOf(
								((ITask) element).getEstimateTimeHours()));
					}
				}
			}
			return null;
		}

		public void modify(Object element, String property, Object value) {
			int columnIndex = Arrays.asList(columnNames).indexOf(property);
			if (element instanceof ITask) {
				ITask task = (ITask) element;
				if (columnIndex == 4) {
					if (value instanceof Integer) {
						task.setEstimatedTimeHours(((Integer) value).intValue() * 10);
						treeViewer.refresh();
					}
				}
			}
		}
	}

	
	private class TaskActivityTableSorter extends ViewerSorter {

		public TaskActivityTableSorter() {
			super();
		}

		@Override
		public int compare(Viewer compareViewer, Object o1, Object o2) {
			if (o1 instanceof DateRangeContainer) {
				if (o2 instanceof DateRangeContainer) {
					DateRangeContainer dateRangeTaskContainer1 = (DateRangeContainer) o1;
					DateRangeContainer dateRangeTaskContainer2 = (DateRangeContainer) o2;
					return dateRangeTaskContainer2.getStart().compareTo(dateRangeTaskContainer1.getStart());
				} else {
					return 1;
				}
			} else if (o1 instanceof ITask) {
				if (o2 instanceof AbstractTaskContainer) {
					return -1;
				} else if (o2 instanceof DateRangeActivityDelegate) {
					DateRangeActivityDelegate task1 = (DateRangeActivityDelegate) o1;
					DateRangeActivityDelegate task2 = (DateRangeActivityDelegate) o2;
					Calendar calendar1 = task1.getStart();// MylarTaskListPlugin.getTaskActivityManager().getLastOccurrence(task1.getHandleIdentifier());
					Calendar calendar2 = task2.getStart();// MylarTaskListPlugin.getTaskActivityManager().getLastOccurrence(task2.getHandleIdentifier());
					if (calendar1 != null && calendar2 != null) {
						return calendar2.compareTo(calendar1);
					}
				}
			}
			return 0;
		}
	}
}
