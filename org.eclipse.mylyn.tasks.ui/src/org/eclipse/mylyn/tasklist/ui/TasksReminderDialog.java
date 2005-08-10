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
package org.eclipse.mylar.tasklist.ui;

import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.mylar.tasklist.ITask;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * @author Ken Sueda
 */
public class TasksReminderDialog extends Dialog {
	private List<ITask> tasks = null;
	private Table table = null;
	private TableViewer tableViewer = null;
	private String[] columnNames = new String[] { "Description", "Priority", "Reminder Day"};
	
	private static final int DISMISS_ALL_ID = 200;
	private static final int DISMISS_ID = 201;
	private static final int SNOOZE_ID = 202;
	private static final String DISMISS_ALL_LABEL = "Dismiss All";
	private static final String DISMISS_LABEL = "Dismiss Selected";
	private static final String SNOOZE_ALL_LABEL = "Remind tommorrow";
	private static long DAY = 24 * 3600 * 1000;
	
	public TasksReminderDialog(Shell parentShell, List<ITask> remTasks) {
		super(parentShell);		
		tasks = remTasks;		
	}
	
	@Override
	protected void configureShell(Shell shell) {
	        super.configureShell(shell);
	        shell.setText("Reminders");
	}
	 
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		setBlockOnOpen(false);
		GridLayout gl = new GridLayout(1, false);
		composite.setLayout(gl);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		Composite container = new Composite(composite, SWT.NONE);
		gl = new GridLayout(1, false);
		container.setLayout(gl);
		createTable(container);		
		createTableViewer();
		
		return composite;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, DISMISS_ALL_ID, DISMISS_ALL_LABEL, false);
        createButton(parent, DISMISS_ID, DISMISS_LABEL, false);
        createButton(parent, SNOOZE_ID, SNOOZE_ALL_LABEL, true);        
    }
	
	private void createTable(Composite parent) {
		int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.HIDE_SELECTION;
		table = new Table(parent, style );		
		GridLayout tlayout = new GridLayout();
		table.setLayout(tlayout);
		GridData wd = new GridData(GridData.FILL_BOTH);
		wd.heightHint = 300;
		table.setLayoutData(wd);
				
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		TableColumn column = new TableColumn(table, SWT.LEFT, 0);
		column.setText(columnNames[0]);
		column.setWidth(180);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setSorter(new ReminderTaskSorter(ReminderTaskSorter.DESCRIPTION));

			}
		});

		column = new TableColumn(table, SWT.LEFT, 1);
		column.setText(columnNames[1]);
		column.setWidth(50);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setSorter(new ReminderTaskSorter(ReminderTaskSorter.PRIORITY));
			}
		});		
		
		column = new TableColumn(table, SWT.LEFT, 2);
		column.setText(columnNames[2]);
		column.setWidth(100);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setSorter(new ReminderTaskSorter(ReminderTaskSorter.DATE));
			}
		});		
	}
	
	private void createTableViewer() {
		tableViewer = new TableViewer(table);
		tableViewer.setUseHashlookup(true);		
		tableViewer.setContentProvider(new ReminderTasksContentProvider());
		tableViewer.setLabelProvider(new ReminderTasksLabelProvider());
		tableViewer.setInput(tasks);
	}
	
	
	protected void buttonPressed(int buttonId) {
		if (buttonId == DISMISS_ALL_ID) {
			for (ITask t : tasks) {
				t.setReminded(true);
			}
			okPressed();
		} else if (buttonId == DISMISS_ID) {
			Object sel = ((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
			if (sel != null && sel instanceof ITask ) {
				ITask t = (ITask) sel;
				t.setReminded(true);
				tasks.remove(t);
				if (tasks.isEmpty()) {
					okPressed();
				} else {
					tableViewer.refresh();
				}				
			}
		} else if (buttonId == SNOOZE_ID) {	
			Object sel = ((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
			if (sel != null && sel instanceof ITask ) {
				ITask t = (ITask) sel;
				t.setReminded(false);
				t.setReminderDate( new Date(new Date().getTime() + DAY));
				tasks.remove(t);
				if (tasks.isEmpty()) {
					okPressed();
				} else {
					tableViewer.refresh();
				}
			}
		}
		super.buttonPressed(buttonId);
	}
	
	private class ReminderTasksContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return tasks.toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	private class ReminderTasksLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof ITask) {
				ITask task = (ITask) element;
				switch (columnIndex) {
				case 0:
					return task.getDescription(true);
				case 1:
					return task.getPriority();
				case 2:
					return task.getReminderDateString(true);
				}
			}
			return null;
		}

	}

	class ReminderTaskSorter extends ViewerSorter {

		public final static int DESCRIPTION = 1;
		public final static int PRIORITY = 2;
		public final static int DATE = 3;	
 
		private int criteria;

		public ReminderTaskSorter(int criteria) {
			super();
			this.criteria = criteria;
		}

		@Override
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			ITask t1 = (ITask) obj1;
			ITask t2 = (ITask) obj2;

			switch (criteria) {
				case DESCRIPTION:
					return compareDescription(t1, t2);
				case PRIORITY:
					return comparePriority(t1, t2);
				case DATE:
					return compareDate(t1, t2);
				default:
					return 0;
			}
		}
		
		private int compareDescription(ITask task1, ITask task2) {
			return task1.getLabel().compareTo(task2.getLabel());
		}
		
		private int comparePriority(ITask task1, ITask task2) {
			return task1.getPriority().compareTo(task2.getPriority());
		}
		
		private int compareDate(ITask task1, ITask task2) {
			return task2.getReminderDate().compareTo(task1.getReminderDate());
		}		
	}
}
