/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core.externalization;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.mylyn.internal.tasks.core.ITaskListRunnable;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.internal.tasks.core.TaskList;
import org.eclipse.mylyn.tasks.core.ITaskListChangeListener;
import org.eclipse.mylyn.tasks.core.TaskContainerDelta;

/**
 * @author Rob Elves
 */
public class TaskListExternalizationParticipant implements IExternalizationParticipant, ITaskListChangeListener {

	private final ExternalizationManager manager;

	private final TaskListExternalizer taskListWriter;

	private final TaskList taskList;

	private boolean dirty;

	public TaskListExternalizationParticipant(TaskList taskList, TaskListExternalizer taskListExternalizer,
			ExternalizationManager manager) {
		this.manager = manager;
		this.taskList = taskList;
		this.taskListWriter = taskListExternalizer;
	}

	public ISchedulingRule getSchedulingRule() {
		return TaskList.getSchedulingRule();
	}

	public boolean isDirty() {
		return dirty;
	}

	public void execute(IExternalizationContext context, IProgressMonitor monitor) throws CoreException {

		Assert.isNotNull(context);
		final File taskListFile = new File(context.getRootPath() + File.separator
				+ ITasksCoreConstants.DEFAULT_TASK_LIST_FILE);

		if (!taskListFile.exists()) {
			try {
				taskListFile.createNewFile();
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN,
						"Task List file not found, error creating new file.", e));
			}
		}

		switch (context.getKind()) {
		case SAVE:
			ITaskListRunnable saveRunnable = new ITaskListRunnable() {

				public void execute(IProgressMonitor monitor) throws CoreException {
					taskListWriter.writeTaskList(taskList, taskListFile);
					synchronized (TaskListExternalizationParticipant.this) {
						dirty = false;
					}
				}
			};

			taskList.run(saveRunnable, monitor);
			break;
		case LOAD:

			ITaskListRunnable loadRunnable = new ITaskListRunnable() {

				public void execute(IProgressMonitor monitor) throws CoreException {
					if (taskListFile.exists()) {
						taskList.preTaskListRead();
						taskListWriter.readTaskList(taskList, taskListFile);
						taskList.postTaskListRead();
					} else {
						// XXX:
						//resetTaskList();
					}
				}

			};
			taskList.run(loadRunnable, monitor);
			break;
		case SNAPSHOT:
			break;
		}

	}

	public void containersChanged(Set<TaskContainerDelta> containers) {
		synchronized (TaskListExternalizationParticipant.this) {
			dirty = true;
		}
		manager.requestSave();
	}

	public void taskListRead() {
		// ignore
	}

	public String getDescription() {
		return "Task List";
	}

}
