/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.TaskAttachment;
import org.eclipse.mylyn.internal.tasks.core.TaskDataStorageManager;
import org.eclipse.mylyn.internal.tasks.core.data.FileTaskAttachmentSource;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractAttachmentHandler;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractLegacyRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.core.deprecated.FileAttachment;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryAttachment;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskAttachment;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.ITask.SynchronizationState;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * @author Steffen Pingel
 */
public class AttachmentUtil {

	private static final String CONTEXT_DESCRIPTION = "mylyn/context/zip";

	private static final String CONTEXT_DESCRIPTION_LEGACY = "mylar/context/zip";

	private static final String CONTEXT_FILENAME = "mylyn-context.zip";

	private static final int BUFFER_SIZE = 1024;

	/**
	 * Attaches the associated context to <code>task</code>.
	 * 
	 * @return false, if operation is not supported by repository
	 */
	@Deprecated
	public static boolean attachContext(AbstractAttachmentHandler attachmentHandler, TaskRepository repository,
			ITask task, String longComment, IProgressMonitor monitor) throws CoreException {
		ContextCore.getContextManager().saveContext(task.getHandleIdentifier());
		final File sourceContextFile = ContextCore.getContextManager().getFileForContext(task.getHandleIdentifier());

		SynchronizationState previousState = task.getSynchronizationState();

		if (sourceContextFile != null && sourceContextFile.exists()) {
			try {
				((AbstractTask) task).setSubmitting(true);
				((AbstractTask) task).setSynchronizationState(SynchronizationState.OUTGOING);
				FileAttachment attachment = new FileAttachment(sourceContextFile);
				attachment.setDescription(CONTEXT_DESCRIPTION);
				attachment.setFilename(CONTEXT_FILENAME);
				attachmentHandler.uploadAttachment(repository, task, attachment, longComment, monitor);
			} catch (CoreException e) {
				// TODO: Calling method should be responsible for returning
				// state of task. Wizard will have different behaviour than
				// editor.
				((AbstractTask) task).setSynchronizationState(previousState);
				throw e;
			} catch (OperationCanceledException e) {
				return true;
			}
		}
		return true;
	}

	public static boolean postContext(AbstractRepositoryConnector connector, TaskRepository repository, ITask task,
			String comment, IProgressMonitor monitor) throws CoreException {
		AbstractTaskAttachmentHandler attachmentHandler = connector.getTaskAttachmentHandler();
		ContextCore.getContextManager().saveContext(task.getHandleIdentifier());
		File file = ContextCore.getContextManager().getFileForContext(task.getHandleIdentifier());
		if (file != null && file.exists()) {
			FileTaskAttachmentSource attachment = new FileTaskAttachmentSource(file);
			attachment.setDescription(CONTEXT_DESCRIPTION);
			attachment.setName(CONTEXT_FILENAME);
			attachmentHandler.postContent(repository, task, attachment, comment, null, monitor);
			return true;
		}
		return false;
	}

	/**
	 * Implementors of this repositoryOperations must perform it locally without going to the server since it is used
	 * for frequent repositoryOperations such as decoration.
	 * 
	 * @return an empty set if no contexts
	 */
	public static Set<RepositoryAttachment> getLegacyContextAttachments(TaskRepository repository, ITask task) {
		TaskDataStorageManager taskDataManager = TasksUiPlugin.getTaskDataStorageManager();
		Set<RepositoryAttachment> contextAttachments = new HashSet<RepositoryAttachment>();
		if (taskDataManager != null) {
			RepositoryTaskData newData = taskDataManager.getNewTaskData(task.getRepositoryUrl(), task.getTaskId());
			if (newData != null) {
				for (RepositoryAttachment attachment : newData.getAttachments()) {
					if (attachment.getDescription().equals(CONTEXT_DESCRIPTION)) {
						contextAttachments.add(attachment);
					} else if (attachment.getDescription().equals(CONTEXT_DESCRIPTION_LEGACY)) {
						contextAttachments.add(attachment);
					}
				}
			}
		}
		return contextAttachments;
	}

	public static ITaskAttachment[] getContextAttachments(TaskRepository repository, ITask task) {
		List<ITaskAttachment> contextAttachments = new ArrayList<ITaskAttachment>();
		TaskData taskData;
		try {
			taskData = TasksUi.getTaskDataManager().getTaskData(task, task.getConnectorKind());
		} catch (CoreException e) {
			// ignore
			return new ITaskAttachment[0];
		}
		if (taskData != null) {
			TaskAttribute[] taskAttachments = taskData.getAttributeMapper().getAttributesByType(taskData,
					TaskAttribute.TYPE_ATTACHMENT);
			for (TaskAttribute attribute : taskAttachments) {
				TaskAttachment taskAttachment = new TaskAttachment(repository, task, attribute);
				taskData.getAttributeMapper().updateTaskAttachment(taskAttachment, attribute);
				if (isContext(taskAttachment)) {
					contextAttachments.add(taskAttachment);
				}
			}
		}
		return contextAttachments.toArray(new ITaskAttachment[0]);
	}

	public static boolean hasContext(TaskRepository repository, ITask task) {
		if (repository == null || task == null) {
			return false;
		} else {
			Set<RepositoryAttachment> remoteContextAttachments = getLegacyContextAttachments(repository, task);
			return (remoteContextAttachments != null && remoteContextAttachments.size() > 0);
		}
	}

	public static boolean hasContextAttachment(ITask task) {
		Assert.isNotNull(task);
		TaskRepository repository = TasksUi.getRepositoryManager().getRepository(task.getConnectorKind(),
				task.getRepositoryUrl());
		AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
				repository.getConnectorKind());
		if (connector instanceof AbstractLegacyRepositoryConnector) {
			Set<RepositoryAttachment> remoteContextAttachments = getLegacyContextAttachments(repository, task);
			return (remoteContextAttachments != null && remoteContextAttachments.size() > 0);
		} else {
			ITaskAttachment[] contextAttachments = getContextAttachments(repository, task);
			return contextAttachments.length > 0;
		}
	}

	public static boolean hasLocalContext(ITask task) {
		Assert.isNotNull(task);
		return ContextCore.getContextManager().hasContext(task.getHandleIdentifier());
	}

	@Deprecated
	public static boolean isContext(RepositoryAttachment attachment) {
		return CONTEXT_DESCRIPTION.equals(attachment.getDescription())
				|| CONTEXT_DESCRIPTION_LEGACY.equals(attachment.getDescription());
	}

	public static boolean isContext(ITaskAttachment attachment) {
		return CONTEXT_DESCRIPTION.equals(attachment.getDescription())
				|| CONTEXT_DESCRIPTION_LEGACY.equals(attachment.getDescription());
	}

	/**
	 * Retrieves a context stored in <code>attachment</code> from <code>task</code>.
	 * 
	 * @return false, if operation is not supported by repository
	 */
	@Deprecated
	public static boolean retrieveContext(AbstractAttachmentHandler attachmentHandler, TaskRepository repository,
			ITask task, RepositoryAttachment attachment, String destinationPath, IProgressMonitor monitor)
			throws CoreException {

		File destinationContextFile = ContextCore.getContextManager().getFileForContext(task.getHandleIdentifier());

		// TODO: add functionality for not overwriting previous context
		if (destinationContextFile.exists()) {
			if (!destinationContextFile.delete()) {
				return false;
			}
		}
		FileOutputStream out;
		try {
			out = new FileOutputStream(destinationContextFile);
			try {
				attachmentHandler.downloadAttachment(repository, attachment, out, monitor);
			} finally {
				try {
					out.close();
				} catch (IOException e) {
					StatusHandler.log(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Error closing context file",
							e));
				}
			}
		} catch (FileNotFoundException e) {
			throw new CoreException(new RepositoryStatus(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN,
					RepositoryStatus.ERROR_INTERNAL, "Could not create context file", e));
		}
		return true;
	}

	public static boolean getContext(AbstractRepositoryConnector connector, TaskRepository repository, ITask task,
			TaskAttribute attribute, IProgressMonitor monitor) throws CoreException {
		AbstractTaskAttachmentHandler attachmentHandler = connector.getTaskAttachmentHandler();
		File file = ContextCore.getContextManager().getFileForContext(task.getHandleIdentifier());
		try {
			FileOutputStream out = new FileOutputStream(file);
			try {
				InputStream in = attachmentHandler.getContent(repository, task, attribute, monitor);
				try {
					int len;
					byte[] buffer = new byte[BUFFER_SIZE];
					while ((len = in.read(buffer)) != -1) {
						out.write(buffer, 0, len);
					}
				} finally {
					in.close();
				}
			} finally {
				out.close();
			}
		} catch (IOException e) {
			throw new CoreException(new RepositoryStatus(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN,
					RepositoryStatus.ERROR_INTERNAL, "Could not create context file", e));
		}
		return true;
	}

	public static boolean canUploadAttachment(ITask task) {
		TaskRepository repository = TasksUi.getRepositoryManager().getRepository(task.getConnectorKind(),
				task.getRepositoryUrl());
		AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
				repository.getConnectorKind());
		if (connector instanceof AbstractLegacyRepositoryConnector) {
			AbstractAttachmentHandler attachmentHandler = ((AbstractLegacyRepositoryConnector) connector).getAttachmentHandler();
			if (attachmentHandler != null) {
				return attachmentHandler.canUploadAttachment(repository, task);
			}
		} else {
			AbstractTaskAttachmentHandler attachmentHandler = connector.getTaskAttachmentHandler();
			if (attachmentHandler != null) {
				return attachmentHandler.canPostContent(repository, task);
			}
		}
		return false;
	}

	public static boolean canDownloadAttachment(ITask task) {
		TaskRepository repository = TasksUi.getRepositoryManager().getRepository(task.getConnectorKind(),
				task.getRepositoryUrl());
		AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
				repository.getConnectorKind());
		if (connector instanceof AbstractLegacyRepositoryConnector) {
			AbstractAttachmentHandler attachmentHandler = ((AbstractLegacyRepositoryConnector) connector).getAttachmentHandler();
			if (attachmentHandler != null) {
				return attachmentHandler.canDownloadAttachment(repository, task);
			}
		} else {
			AbstractTaskAttachmentHandler attachmentHandler = connector.getTaskAttachmentHandler();
			if (attachmentHandler != null) {
				return attachmentHandler.canGetContent(repository, task);
			}
		}
		return false;
	}

}
