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

package org.eclipse.mylar.internal.tasklist.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylar.internal.tasklist.TaskListPreferenceConstants;
import org.eclipse.mylar.internal.tasklist.ui.TaskListImages;
import org.eclipse.mylar.internal.tasklist.ui.TaskListUiUtil;
import org.eclipse.mylar.internal.tasklist.ui.views.TaskInputDialog;
import org.eclipse.mylar.internal.tasklist.ui.views.TaskListView;
import org.eclipse.mylar.provisional.core.MylarPlugin;
import org.eclipse.mylar.provisional.tasklist.ITask;
import org.eclipse.mylar.provisional.tasklist.MylarTaskListPlugin;
import org.eclipse.mylar.provisional.tasklist.Task;
import org.eclipse.mylar.provisional.tasklist.TaskCategory;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;

/**
 * @author Mik Kersten
 */
public class NewLocalTaskAction extends Action {

	public static final String NEW_TASK_DESCRIPTION = "new task";

	public static final String ID = "org.eclipse.mylar.tasklist.actions.create.task";

	private final TaskListView view;

	public NewLocalTaskAction(TaskListView view) {
		this.view = view;
		setText(TaskInputDialog.LABEL_SHELL);
		setToolTipText(TaskInputDialog.LABEL_SHELL);
		setId(ID);
		setImageDescriptor(TaskListImages.TASK_NEW);
	}

	/**
	 * Returns the default URL text for the task by first checking the contents
	 * of the clipboard and then using the default prefix preference if that
	 * fails
	 */
	protected String getDefaultIssueURL() {

		String clipboardText = getClipboardText();
		if ((clipboardText.startsWith("http://") || clipboardText.startsWith("https://") && clipboardText.length() > 10)) {
			return clipboardText;
		}

		String defaultPrefix = MylarPlugin.getDefault().getPreferenceStore().getString(
				TaskListPreferenceConstants.DEFAULT_URL_PREFIX);
		if (!defaultPrefix.equals("")) {
			return defaultPrefix;
		}

		return "";
	}	
	
	/**
	 * Returns the contents of the clipboard or "" if no text content was
	 * available
	 */
	protected String getClipboardText() {
		Clipboard clipboard = new Clipboard(Display.getDefault());
		TextTransfer transfer = TextTransfer.getInstance();
		String contents = (String) clipboard.getContents(transfer);
		if (contents != null) {
			return contents;
		} else {
			return "";
		}
	}
	
	@Override
	public void run() {
		// TaskInputDialog dialog = new
		// TaskInputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		// int dialogResult = dialog.open();
		// if (dialogResult == Window.OK) {
		Task newTask = new Task(MylarTaskListPlugin.getTaskListManager().genUniqueTaskHandle(), NEW_TASK_DESCRIPTION, true);
		// Task newTask = new
		// Task(MylarTaskListPlugin.getTaskListManager().genUniqueTaskHandle(),
		// dialog
		// .getTaskname(), true);
		// MylarTaskListPlugin.getTaskListManager().getTaskList().addTask(newTask);
		// newTask.setPriority(dialog.getSelectedPriority());
		// newTask.setReminderDate(dialog.getReminderDate());
		 newTask.setUrl(getDefaultIssueURL());

		Object selectedObject = ((IStructuredSelection) view.getViewer().getSelection()).getFirstElement();

		if (selectedObject instanceof TaskCategory) {
			MylarTaskListPlugin.getTaskListManager().getTaskList().addTask(newTask, (TaskCategory) selectedObject);
		} else if (selectedObject instanceof ITask) {
			ITask task = (ITask) selectedObject;
			if (task.getContainer() instanceof TaskCategory) {
				MylarTaskListPlugin.getTaskListManager().getTaskList().addTask(newTask,
						(TaskCategory) task.getContainer());
			} else if (view.getDrilledIntoCategory() instanceof TaskCategory) {
				MylarTaskListPlugin.getTaskListManager().getTaskList().addTask(newTask,
						(TaskCategory) view.getDrilledIntoCategory());
			} else {
				MylarTaskListPlugin.getTaskListManager().getTaskList().addTask(newTask,
						MylarTaskListPlugin.getTaskListManager().getTaskList().getRootCategory());
				// MylarTaskListPlugin.getTaskListManager().getTaskList().moveToRoot(newTask);
			}
		} else if (view.getDrilledIntoCategory() != null) {
			MylarTaskListPlugin.getTaskListManager().getTaskList().addTask(newTask,
					(TaskCategory) view.getDrilledIntoCategory());
		} else {
			MylarTaskListPlugin.getTaskListManager().getTaskList().addTask(newTask,
					MylarTaskListPlugin.getTaskListManager().getTaskList().getRootCategory());
		}
		TaskListUiUtil.openEditor(newTask);
		// newTask.openTaskInEditor(false);
		view.getViewer().refresh();
		view.getViewer().setSelection(new StructuredSelection(newTask));
		// }
	}
}
