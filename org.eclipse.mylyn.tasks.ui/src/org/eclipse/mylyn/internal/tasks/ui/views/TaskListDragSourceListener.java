/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.views;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylyn.internal.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.internal.tasks.ui.TaskTransfer;
import org.eclipse.mylyn.internal.tasks.ui.actions.CopyTaskDetailsAction;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;

/**
 * @author Mik Kersten
 * @author Jevgeni Holodkov
 * @author Leo Dos Santos
 * @author Steffen Pingel
 */
class TaskListDragSourceListener implements DragSourceListener {

	static final String DELIM = ", ";

	private final TaskListView view;

	private StructuredSelection selection;

	public TaskListDragSourceListener(TaskListView view) {
		this.view = view;
	}

	public void dragStart(DragSourceEvent event) {
		StructuredSelection selection = (StructuredSelection) this.view.getViewer().getSelection();
		if (selection.isEmpty()) {
			this.selection = null;
			event.doit = false;
		} else {
			this.selection = selection;
		}
	}

	private List<File> createTaskFiles(StructuredSelection selection) {
		// prepare temporary directory 
		File tempDir = new File(TasksUiPlugin.getDefault().getDataDirectory() + File.separator + "temp");
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}

		// extract queries and tasks from selection
		List<AbstractRepositoryQuery> queries = new ArrayList<AbstractRepositoryQuery>();
		List<AbstractTask> tasks = new ArrayList<AbstractTask>();
		for (Iterator<?> it = selection.iterator(); it.hasNext();) {
			Object element = it.next();
			if (element instanceof AbstractRepositoryQuery) {
				queries.add((AbstractRepositoryQuery) element);
			} else if (element instanceof AbstractTask) {
				tasks.add((AbstractTask) element);
			}
		}

		List<File> taskFiles = new ArrayList<File>(queries.size() + tasks.size());
		try {
			for (AbstractRepositoryQuery query : queries) {
				String encodedName = URLEncoder.encode(query.getHandleIdentifier(), ITasksUiConstants.FILENAME_ENCODING);
				File file = File.createTempFile(encodedName, ITasksUiConstants.FILE_EXTENSION, tempDir);
				file.deleteOnExit();

				TasksUiPlugin.getTaskListManager().getTaskListWriter().writeQueries(Collections.singletonList(query),
						file);
				taskFiles.add(file);
			}

			for (AbstractTask task : tasks) {
				String encodedName = URLEncoder.encode(task.getHandleIdentifier(), ITasksUiConstants.FILENAME_ENCODING);
				File file = File.createTempFile(encodedName, ITasksUiConstants.FILE_EXTENSION, tempDir);
				file.deleteOnExit();

				TasksUiPlugin.getTaskListManager().getTaskListWriter().writeTask(task, file);
				taskFiles.add(file);
			}

			return taskFiles;
		} catch (IOException e) {
			StatusHandler.fail(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Cannot create a temp query file for Drag&Drop", e));
			return null;
		}
	}

	public void dragSetData(DragSourceEvent event) {
		if (selection == null || selection.isEmpty()) {
			return;
		}

		if (TaskTransfer.getInstance().isSupportedType(event.dataType)) {
			List<AbstractTask> tasks = new ArrayList<AbstractTask>();
			for (Iterator<?> it = selection.iterator(); it.hasNext();) {
				AbstractTaskContainer element = (AbstractTaskContainer) it.next();
				if (element instanceof AbstractTask) {
					tasks.add((AbstractTask) element);
				}
			}
			event.data = tasks.toArray(new AbstractTask[0]);
		} else if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
			List<File> files = createTaskFiles(selection);
			if (files != null && !files.isEmpty()) {
				String[] paths = new String[files.size()];
				int i = 0;
				for (File file : files) {
					paths[i++] = file.getAbsolutePath();
				}
				event.data = paths;
			}
		} else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
			event.data = CopyTaskDetailsAction.getTextForTask(selection.getFirstElement());
		}
	}

	public void dragFinished(DragSourceEvent event) {
		// don't care if the drag is done
	}
}
