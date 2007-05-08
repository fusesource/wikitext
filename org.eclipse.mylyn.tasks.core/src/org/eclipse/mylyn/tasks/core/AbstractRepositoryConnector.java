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

package org.eclipse.mylar.tasks.core;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylar.context.core.ContextCorePlugin;
import org.eclipse.mylar.internal.tasks.core.TaskDataManager;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask.RepositoryTaskSyncState;

/**
 * Operations on a task repository
 * 
 * @author Mik Kersten
 * @author Rob Elves
 */
public abstract class AbstractRepositoryConnector {

	public static final String MESSAGE_ATTACHMENTS_NOT_SUPPORTED = "Attachments not supported by connector: ";

	public static final String MYLAR_CONTEXT_DESCRIPTION = "mylar/context/zip";

	private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

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
	public AbstractRepositoryTask createTaskFromExistingId(TaskRepository repository, String id) throws CoreException {
		return createTaskFromExistingId(repository, id, true);
	}
	
	/**
	 * Create new repository task, storing necessary task data
	 * 
	 * @param taskId
	 *            identifier, e.g. "123" bug Bugzilla bug 123
	 * @return null if task could not be created
	 * @throws CoreException
	 *             TODO
	 */
	protected AbstractRepositoryTask createTaskFromExistingId(TaskRepository repository, String id, boolean retrieveSubTasks) throws CoreException {
		ITask task = taskList.getTask(repository.getUrl(), id);
		AbstractRepositoryTask repositoryTask = null;
		if (task instanceof AbstractRepositoryTask) {
			repositoryTask = (AbstractRepositoryTask) task;
		} else if (task == null && getTaskDataHandler() != null) {
			RepositoryTaskData taskData = null;
			taskData = getTaskDataHandler().getTaskData(repository, id);
			if (taskData != null) {
				// Use connector task factory
				repositoryTask = makeTask(repository.getUrl(), id, taskData.getId() + ": " + taskData.getDescription());
				updateTaskFromTaskData(repository, repositoryTask, taskData, retrieveSubTasks);
				
				taskList.addTask(repositoryTask);
				getTaskDataManager().setNewTaskData(repositoryTask.getHandleIdentifier(), taskData);
			}
		} // TODO: Handle case similar to web tasks (no taskDataHandler but
		// have tasks)

		return repositoryTask;
	}

	/**
	 * Utility method for construction of connector specific task object TODO:
	 * Move to 'task' factory
	 * 
	 * @return instance of AbstractRepositoryTask
	 */
	protected abstract AbstractRepositoryTask makeTask(String repositoryUrl, String id, String summary);

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
			IProgressMonitor monitor, QueryHitCollector resultCollector);

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
	public abstract void updateTaskFromRepository(TaskRepository repository, AbstractRepositoryTask repositoryTask)
			throws CoreException;

	/**
	 * Sets all fields on the given task
	 * @param TaskRepository
	 * @param repositoryTask to update
	 * @param RepositoryTaskData new repository task data from which to update task information
	 * @param retrieveSubTasks true if method should result in construction of missing subtasks, false otherwise
	 * 
	 * @since 2.0
	 */
	public abstract void updateTaskFromTaskData(TaskRepository repository, AbstractRepositoryTask repositoryTask,
			RepositoryTaskData taskData, boolean retrieveSubTasks);

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
	public final Set<RepositoryAttachment> getContextAttachments(TaskRepository repository, AbstractRepositoryTask task) {
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
	public final boolean hasRepositoryContext(TaskRepository repository, AbstractRepositoryTask task) {
		if (repository == null || task == null) {
			return false;
		} else {
			Set<RepositoryAttachment> remoteContextAttachments = getContextAttachments(repository, task);
			return (remoteContextAttachments != null && remoteContextAttachments.size() > 0);
		}
	}

	/**
	 * Of <code>tasks</code> provided, return all that have changed since last
	 * synchronization of <code>repository</code>
	 * 
	 * All errors should be thrown as <code>CoreException</code> for the
	 * framework to handle, since background synchronizations fail silently when
	 * disconnected.
	 * 
	 * TODO: Add progress monitor as parameter
	 * 
	 * @throws CoreException
	 */
	public abstract Set<AbstractRepositoryTask> getChangedSinceLastSync(TaskRepository repository,
			Set<AbstractRepositoryTask> tasks) throws CoreException;

	/**
	 * Attaches the associated context to <code>task</code>.
	 * 
	 * @return false, if operation is not supported by repository
	 */
	public final boolean attachContext(TaskRepository repository, AbstractRepositoryTask task, String longComment)
			throws CoreException {
		ContextCorePlugin.getContextManager().saveContext(task.getHandleIdentifier());
		File sourceContextFile = ContextCorePlugin.getContextManager().getFileForContext(task.getHandleIdentifier());

		RepositoryTaskSyncState previousState = task.getSyncState();

		if (sourceContextFile != null && sourceContextFile.exists()) {
			IAttachmentHandler handler = getAttachmentHandler();
			if (handler == null) {
				return false;
			}

			try {
				task.setSubmitting(true);
				task.setSyncState(RepositoryTaskSyncState.OUTGOING);
				handler.uploadAttachment(repository, task, longComment, MYLAR_CONTEXT_DESCRIPTION, sourceContextFile,
						APPLICATION_OCTET_STREAM, false);
			} catch (CoreException e) {
				// TODO: Calling method should be responsible for returning
				// state of task. Wizard will have different behaviour than
				// editor.
				task.setSyncState(previousState);
				throw e;
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
	public final boolean retrieveContext(TaskRepository repository, AbstractRepositoryTask task,
			RepositoryAttachment attachment, String destinationPath) throws CoreException {
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
		attachmentHandler.downloadAttachment(repository, attachment, destinationContextFile);
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
	public String getLastSyncTimestamp(TaskRepository repository, Set<AbstractRepositoryTask> changedTasks) {
		Date mostRecent = new Date(0);
		String mostRecentTimeStamp = repository.getSyncTimeStamp();
		for (AbstractRepositoryTask task : changedTasks) {
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

	private RepositoryTaskData getTaskData(AbstractRepositoryTask task) {
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
