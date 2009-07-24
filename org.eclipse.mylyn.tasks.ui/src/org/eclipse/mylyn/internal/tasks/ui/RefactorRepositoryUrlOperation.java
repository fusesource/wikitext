/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.context.core.InteractionContext;
import org.eclipse.mylyn.internal.context.core.InteractionContextManager;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.internal.tasks.core.RepositoryTaskHandleUtil;
import org.eclipse.mylyn.internal.tasks.core.data.TaskDataManager;
import org.eclipse.mylyn.monitor.core.InteractionEvent;
import org.eclipse.mylyn.tasks.core.ITask;

/**
 * @author Rob Elves
 */
public class RefactorRepositoryUrlOperation extends TaskListModifyOperation {

	private final String oldUrl;

	private final String newUrl;

	public RefactorRepositoryUrlOperation(String oldUrl, String newUrl) {
		super(ITasksCoreConstants.ROOT_SCHEDULING_RULE);
		Assert.isNotNull(oldUrl);
		Assert.isNotNull(newUrl);
		Assert.isTrue(!oldUrl.equals(newUrl));
		this.oldUrl = oldUrl;
		this.newUrl = newUrl;
	}

	@Override
	protected void operations(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
			InterruptedException {
		try {
			//TasksUiPlugin.getTaskListManager().deactivateAllTasks();
			monitor.beginTask(Messages.RefactorRepositoryUrlOperation_Repository_URL_update, IProgressMonitor.UNKNOWN);
			refactorOfflineHandles(oldUrl, newUrl);
			getTaskList().refactorRepositoryUrl(oldUrl, newUrl);
			refactorMetaContextHandles(oldUrl, newUrl);
			refactorContextFileNames();
			TasksUiPlugin.getTaskActivityMonitor().reloadActivityTime();
		} finally {
			monitor.done();
		}
	}

	@SuppressWarnings("restriction")
	public void refactorContextFileNames() {

		File dataDir = new File(TasksUiPlugin.getDefault().getDataDirectory(), ITasksCoreConstants.CONTEXTS_DIRECTORY);
		if (dataDir.exists() && dataDir.isDirectory()) {
			File[] files = dataDir.listFiles();
			if (files != null) {
				for (File file : dataDir.listFiles()) {
					int dotIndex = file.getName().lastIndexOf(".xml"); //$NON-NLS-1$
					if (dotIndex != -1) {
						String storedHandle;
						try {
							storedHandle = URLDecoder.decode(file.getName().substring(0, dotIndex),
									InteractionContextManager.CONTEXT_FILENAME_ENCODING);
							int delimIndex = storedHandle.lastIndexOf(RepositoryTaskHandleUtil.HANDLE_DELIM);
							if (delimIndex != -1) {
								String storedUrl = storedHandle.substring(0, delimIndex);
								if (oldUrl.equals(storedUrl)) {
									String id = RepositoryTaskHandleUtil.getTaskId(storedHandle);
									String newHandle = RepositoryTaskHandleUtil.getHandle(newUrl, id);
									File newFile = ContextCorePlugin.getContextStore().getFileForContext(newHandle);
									file.renameTo(newFile);
								}
							}
						} catch (Exception e) {
							StatusHandler.log(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN,
									"Could not move context file: " + file.getName(), e)); //$NON-NLS-1$
						}
					}
				}
			}
		}
	}

	private void refactorOfflineHandles(String oldRepositoryUrl, String newRepositoryUrl) throws CoreException {
		TaskDataManager taskDataManager = TasksUiPlugin.getTaskDataManager();
		for (ITask task : getTaskList().getAllTasks()) {
			if (oldRepositoryUrl.equals(task.getAttribute(ITasksCoreConstants.ATTRIBUTE_OUTGOING_NEW_REPOSITORY_URL))) {
				taskDataManager.refactorRepositoryUrl(task, task.getRepositoryUrl(), newRepositoryUrl);
			}
			if (task.getRepositoryUrl().equals(oldRepositoryUrl)) {
				taskDataManager.refactorRepositoryUrl(task, newRepositoryUrl, newRepositoryUrl);
//					RepositoryTaskData newTaskData = taskDataManager.getNewTaskData(repositoryTask.getRepositoryUrl(),
//							repositoryTask.getTaskId());
//					RepositoryTaskData oldTaskData = taskDataManager.getOldTaskData(repositoryTask.getRepositoryUrl(),
//							repositoryTask.getTaskId());
//					Set<RepositoryTaskAttribute> edits = taskDataManager.getEdits(repositoryTask.getRepositoryUrl(),
//							repositoryTask.getTaskId());
//					taskDataManager.remove(repositoryTask.getRepositoryUrl(), repositoryTask.getTaskId());
//
//					if (newTaskData != null) {
//						newTaskData.setRepositoryURL(newRepositoryUrl);
//						taskDataManager.setNewTaskData(newTaskData);
//					}
//					if (oldTaskData != null) {
//						oldTaskData.setRepositoryURL(newRepositoryUrl);
//						taskDataManager.setOldTaskData(oldTaskData);
//					}
//					if (!edits.isEmpty()) {
//						taskDataManager.saveEdits(newRepositoryUrl, repositoryTask.getTaskId(), edits);
//					}
			}
		}
//		TasksUiPlugin.getTaskDataStorageManager().saveNow();
	}

	@SuppressWarnings("restriction")
	private void refactorMetaContextHandles(String oldRepositoryUrl, String newRepositoryUrl) {
		InteractionContext metaContext = ContextCorePlugin.getContextManager().getActivityMetaContext();
		ContextCorePlugin.getContextManager().resetActivityMetaContext();
		InteractionContext newMetaContext = ContextCorePlugin.getContextManager().getActivityMetaContext();
		for (InteractionEvent event : metaContext.getInteractionHistory()) {
			if (event.getStructureHandle() != null) {
				String storedUrl = RepositoryTaskHandleUtil.getRepositoryUrl(event.getStructureHandle());
				if (storedUrl != null) {
					if (oldRepositoryUrl.equals(storedUrl)) {
						String taskId = RepositoryTaskHandleUtil.getTaskId(event.getStructureHandle());
						if (taskId != null) {
							String newHandle = RepositoryTaskHandleUtil.getHandle(newRepositoryUrl, taskId);
							event = new InteractionEvent(event.getKind(), event.getStructureKind(), newHandle,
									event.getOriginId(), event.getNavigation(), event.getDelta(),
									event.getInterestContribution(), event.getDate(), event.getEndDate());
						}
					}
				}
			}
			newMetaContext.parseEvent(event);
		}
	}

}
