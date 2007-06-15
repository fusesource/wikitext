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

package org.eclipse.mylyn.tasks.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.mylyn.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.monitor.core.util.StatusManager;
import org.eclipse.mylyn.internal.tasks.core.TaskDataManager;
import org.eclipse.mylyn.tasks.core.AbstractTask.RepositoryTaskSyncState;

/**
 * Operations on a task repository
 * 
 * @author Mik Kersten
 * @author Rob Elves
 */
public abstract class AbstractRepositoryConnector {

	public static final String MESSAGE_ATTACHMENTS_NOT_SUPPORTED = "Attachments not supported by connector: ";

	public static final String MYLAR_CONTEXT_DESCRIPTION = "mylar/context/zip";

	public final static String MYLAR_CONTEXT_FILENAME = "mylar-context.zip";

	protected Set<RepositoryTemplate> templates = new LinkedHashSet<RepositoryTemplate>();

	protected TaskList taskList;

	private boolean userManaged = true;

	private TaskDataManager taskDataManager;

	public void init(TaskList taskList) {
		this.taskList = taskList;
	}

	/**
	 * @return null if not supported
	 */
	public abstract IAttachmentHandler getAttachmentHandler();

	/**
	 * @return null if not supported
	 */
	public abstract ITaskDataHandler getTaskDataHandler();

	public abstract String getRepositoryUrlFromTaskUrl(String taskFullUrl);

	public abstract String getTaskIdFromTaskUrl(String taskFullUrl);

	public abstract String getTaskWebUrl(String repositoryUrl, String taskId);

	public String[] getTaskIdsFromComment(TaskRepository repository, String comment) {
		return null;
	}

	public abstract boolean canCreateTaskFromKey(TaskRepository repository);

	public abstract boolean canCreateNewTask(TaskRepository repository);

	/**
	 * create task and necessary subtasks (1 level nesting)
	 */
	public AbstractTask createTaskFromExistingId(TaskRepository repository, String id,
			IProgressMonitor monitor) throws CoreException {
		return createTaskFromExistingId(repository, id, true, monitor);
	}

	/**
	 * Create new repository task, adding result to tasklist
	 */
	public AbstractTask createTaskFromExistingId(TaskRepository repository, String id,
			boolean retrieveSubTasks, IProgressMonitor monitor) throws CoreException {
		AbstractTask task = taskList.getTask(repository.getUrl(), id);
		AbstractTask repositoryTask = null;
		if (task instanceof AbstractTask) {
			repositoryTask = (AbstractTask) task;
		} else if (task == null && getTaskDataHandler() != null) {
			RepositoryTaskData taskData = null;
			taskData = getTaskDataHandler().getTaskData(repository, id, new SubProgressMonitor(monitor, 1));
			if (taskData != null) {
				repositoryTask = createTaskFromTaskData(repository, taskData, retrieveSubTasks, new SubProgressMonitor(
						monitor, 1));
				if (repositoryTask != null) {
					taskList.addTask(repositoryTask);
				}
			}
		} // TODO: Handle case similar to web tasks (no taskDataHandler but
		// have tasks)

		return repositoryTask;
	}

	/**
	 * Creates a new task from the given task data. Does NOT add resulting task
	 * to the tasklist
	 */
	public AbstractTask createTaskFromTaskData(TaskRepository repository, RepositoryTaskData taskData,
			boolean retrieveSubTasks, IProgressMonitor monitor) throws CoreException {
		AbstractTask repositoryTask = null;
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		try {
			if (taskData != null && getTaskDataManager() != null) {
				// Use connector task factory
				repositoryTask = createTask(repository.getUrl(), taskData.getId(), taskData.getId() + ": "
						+ taskData.getDescription());
				updateTaskFromTaskData(repository, repositoryTask, taskData);
				getTaskDataManager().setNewTaskData(repositoryTask.getHandleIdentifier(), taskData);

				if (retrieveSubTasks) {
					monitor.beginTask("Creating task", getTaskDataHandler().getSubTaskIds(taskData).size());
					for (String subId : getTaskDataHandler().getSubTaskIds(taskData)) {
						if (subId == null || subId.trim().equals("")) {
							continue;
						}
						AbstractTask subTask = createTaskFromExistingId(repository, subId, false,
								new SubProgressMonitor(monitor, 1));
						if (subTask != null) {
							taskList.addTask(subTask, repositoryTask);
						}
					}
				}
			}
		} finally {
			monitor.done();
		}
		return repositoryTask;
	}

	/**
	 * Utility method for construction of connector specific task object TODO:
	 * Move to 'task' factory
	 * 
	 * @return instance of AbstractTask
	 */
	public abstract AbstractTask createTask(String repositoryUrl, String id, String summary);

	/**
	 * Implementors must execute query synchronously.
	 * 
	 * @param query
	 * @param repository
	 *            TODO
	 * @param monitor
	 * @param resultCollector
	 *            IQueryHitCollector that collects the hits found
	 */
	public abstract IStatus performQuery(AbstractRepositoryQuery query, TaskRepository repository,
			IProgressMonitor monitor, ITaskCollector resultCollector);

	/**
	 * The connector's summary i.e. "JIRA (supports 3.3.1 and later)"
	 */
	public abstract String getLabel();

	/**
	 * @return the unique type of the repository, e.g. "bugzilla"
	 */
	public abstract String getRepositoryType();

	/**
	 * Updates the properties of <code>repositoryTask</code>. Invoked when on
	 * task synchronization if {@link #getTaskDataHandler()} returns
	 * <code>null</code> or
	 * {@link ITaskDataHandler#getTaskData(TaskRepository, String)} returns
	 * <code>null</code>.
	 * 
	 * <p>
	 * Connectors that provide {@link RepositoryTaskData} objects for all tasks
	 * do not need to implement this method.
	 * 
	 * @param repository
	 *            the repository
	 * @param repositoryTask
	 *            the task that is synchronized
	 * @throws CoreException
	 *             thrown in case of error while synchronizing
	 * @see {@link #getTaskDataHandler()}
	 */
	public abstract void updateTaskFromRepository(TaskRepository repository, AbstractTask repositoryTask,
			IProgressMonitor monitor) throws CoreException;

	/**
	 * Updates task with latest information from {@code taskData}
	 * 
	 * @since 2.0
	 */
	public abstract void updateTaskFromTaskData(TaskRepository repository, AbstractTask repositoryTask,
			RepositoryTaskData taskData);

	public String[] repositoryPropertyNames() {
		return new String[] { IRepositoryConstants.PROPERTY_VERSION, IRepositoryConstants.PROPERTY_TIMEZONE,
				IRepositoryConstants.PROPERTY_ENCODING };
	}

	/**
	 * Implementors of this repositoryOperations must perform it locally without
	 * going to the server since it is used for frequent repositoryOperations
	 * such as decoration.
	 * 
	 * @return an empty set if no contexts
	 */
	public final Set<RepositoryAttachment> getContextAttachments(TaskRepository repository, AbstractTask task) {
		Set<RepositoryAttachment> contextAttachments = new HashSet<RepositoryAttachment>();

		if (taskDataManager != null && taskDataManager.getNewTaskData(task.getHandleIdentifier()) != null) {
			for (RepositoryAttachment attachment : taskDataManager.getNewTaskData(task.getHandleIdentifier())
					.getAttachments()) {
				if (attachment.getDescription().equals(MYLAR_CONTEXT_DESCRIPTION)) {
					contextAttachments.add(attachment);
				}
			}
		}
		return contextAttachments;
	}

	// TODO: move
	public final boolean hasRepositoryContext(TaskRepository repository, AbstractTask task) {
		if (repository == null || task == null) {
			return false;
		} else {
			Set<RepositoryAttachment> remoteContextAttachments = getContextAttachments(repository, task);
			return (remoteContextAttachments != null && remoteContextAttachments.size() > 0);
		}
	}

	/**
	 * Of <code>tasks</code> provided, return all that have changed since last synchronization of
	 * <code>repository</code>.
	 * 
	 * Tasks that need to be synchronized (i.e. task data updated) should be passed to
	 * <code>collector.accept(Task)</code> method, or if repository connector can update task data, it can use
	 * <code>collector.accept(RepositoryTaskData)</code> call.
	 * 
	 * All errors should be thrown as <code>CoreException</code> for the framework to handle, since background
	 * synchronizations fail silently when disconnected.
	 * @param tasks TODO
	 * 
	 * @return null if there was no tasks changed in the repository, otherwise collection of updated tasks (within
	 *         <code>tasks</code> collection), so empty collection means that there are some other tasks changed
	 * 
	 * @throws CoreException
	 */
	public abstract boolean markStaleTasks(TaskRepository repository, 
			Set<AbstractTask> tasks, IProgressMonitor monitor) throws CoreException;

	/**
	 * Attaches the associated context to <code>task</code>.
	 * 
	 * @return false, if operation is not supported by repository
	 */
	public final boolean attachContext(TaskRepository repository, AbstractTask task, String longComment,
			IProgressMonitor monitor) throws CoreException {
		ContextCorePlugin.getContextManager().saveContext(task.getHandleIdentifier());
		final File sourceContextFile = ContextCorePlugin.getContextManager().getFileForContext(
				task.getHandleIdentifier());

		RepositoryTaskSyncState previousState = task.getSyncState();

		if (sourceContextFile != null && sourceContextFile.exists()) {
			IAttachmentHandler handler = getAttachmentHandler();
			if (handler == null) {
				return false;
			}

			try {
				task.setSubmitting(true);
				task.setSyncState(RepositoryTaskSyncState.OUTGOING);
				FileAttachment attachment = new FileAttachment(sourceContextFile);
				attachment.setDescription(MYLAR_CONTEXT_DESCRIPTION);
				attachment.setFilename(MYLAR_CONTEXT_FILENAME);
				handler.uploadAttachment(repository, task, attachment, longComment, monitor);
			} catch (CoreException e) {
				// TODO: Calling method should be responsible for returning
				// state of task. Wizard will have different behaviour than
				// editor.
				task.setSyncState(previousState);
				throw e;
			} catch (OperationCanceledException e) {
				return true;
			}
		}
		return true;
	}

	/**
	 * Retrieves a context stored in <code>attachment</code> from
	 * <code>task</code>.
	 * 
	 * @return false, if operation is not supported by repository
	 */
	public final boolean retrieveContext(TaskRepository repository, AbstractTask task,
			RepositoryAttachment attachment, String destinationPath, IProgressMonitor monitor) throws CoreException {
		IAttachmentHandler attachmentHandler = getAttachmentHandler();
		if (attachmentHandler == null) {
			return false;
		}

		File destinationContextFile = ContextCorePlugin.getContextManager().getFileForContext(
				task.getHandleIdentifier());

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
					StatusManager.fail(e, "Could not close context file", false);
				}
			}
		} catch (FileNotFoundException e) {
			throw new CoreException(new RepositoryStatus(IStatus.ERROR, "org.eclipse.mylyn.tasks.core",
					RepositoryStatus.ERROR_INTERNAL, "Could not create context file", e));
		}
		return true;
	}

	public void addTemplate(RepositoryTemplate template) {
		this.templates.add(template);
	}

	public Set<RepositoryTemplate> getTemplates() {
		return templates;
	}

	public void removeTemplate(RepositoryTemplate template) {
		this.templates.remove(template);
	}

	/** returns null if template not found */
	public RepositoryTemplate getTemplate(String label) {
		for (RepositoryTemplate template : getTemplates()) {
			if (template.label.equals(label)) {
				return template;
			}
		}
		return null;
	}

	public String getTaskIdPrefix() {
		return "task";
	}

	/**
	 * Reset and update the repository attributes from the server (e.g.
	 * products, components)
	 * 
	 * TODO: remove?
	 */
	public abstract void updateAttributes(TaskRepository repository, IProgressMonitor monitor) throws CoreException;

	public void setUserManaged(boolean userManaged) {
		this.userManaged = userManaged;
	}

	public boolean isUserManaged() {
		return userManaged;
	}

	/**
	 * Following synchronization, the timestamp needs to be recorded. This
	 * provides a default implementation for determining the last
	 * synchronization timestamp. Override to return actual timestamp from
	 * repository.
	 */
	public String getSynchronizationTimestamp(TaskRepository repository, Set<AbstractTask> changedTasks) {
		Date mostRecent = new Date(0);
		String mostRecentTimeStamp = repository.getSyncTimeStamp();
		for (AbstractTask task : changedTasks) {
			Date taskModifiedDate;

			if (getTaskData(task) != null && getTaskDataHandler() != null
					&& getTaskData(task).getLastModified() != null) {
				taskModifiedDate = getTaskData(task).getAttributeFactory().getDateForAttributeType(
						RepositoryTaskAttribute.DATE_MODIFIED, getTaskData(task).getLastModified());
			} else {
				continue;
			}

			if (taskModifiedDate != null && taskModifiedDate.after(mostRecent)) {
				mostRecent = taskModifiedDate;
				mostRecentTimeStamp = getTaskData(task).getLastModified();
			}
		}
		return mostRecentTimeStamp;
	}

	private RepositoryTaskData getTaskData(AbstractTask task) {
		if (taskDataManager != null) {
			return taskDataManager.getNewTaskData(task.getHandleIdentifier());
		}
		return null;
	}

	private TaskDataManager getTaskDataManager() {
		return taskDataManager;
	}

	/**
	 * Set upon construction
	 */
	public void setTaskDataManager(TaskDataManager taskDataManager) {
		this.taskDataManager = taskDataManager;
	}


}
