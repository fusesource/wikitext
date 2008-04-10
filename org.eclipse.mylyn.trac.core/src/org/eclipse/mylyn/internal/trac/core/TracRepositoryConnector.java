/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.trac.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.internal.trac.core.ITracClient.Version;
import org.eclipse.mylyn.internal.trac.core.TracAttributeFactory.Attribute;
import org.eclipse.mylyn.internal.trac.core.TracTask.Kind;
import org.eclipse.mylyn.internal.trac.core.model.TracPriority;
import org.eclipse.mylyn.internal.trac.core.model.TracTicket;
import org.eclipse.mylyn.internal.trac.core.model.TracTicket.Key;
import org.eclipse.mylyn.internal.trac.core.util.TracUtils;
import org.eclipse.mylyn.tasks.core.AbstractAttachmentHandler;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskCollector;
import org.eclipse.mylyn.tasks.core.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.RepositoryOperation;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.SynchronizationEvent;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
import org.eclipse.mylyn.web.core.AuthenticationCredentials;
import org.eclipse.mylyn.web.core.AuthenticationType;
import org.eclipse.mylyn.web.core.Policy;

/**
 * @author Steffen Pingel
 */
public class TracRepositoryConnector extends AbstractRepositoryConnector {

	private final static String CLIENT_LABEL = "Trac (supports 0.9 or 0.10 through Web and XML-RPC)";

	private TracClientManager clientManager;

	private final TracTaskDataHandler taskDataHandler = new TracTaskDataHandler(this);

	private final TracAttachmentHandler attachmentHandler = new TracAttachmentHandler(this);

	private TaskRepositoryLocationFactory taskRepositoryLocationFactory = new TaskRepositoryLocationFactory();

	private File repositoryConfigurationCacheFile;

	public TracRepositoryConnector(File repositoryConfigurationCacheFile) {
		this.repositoryConfigurationCacheFile = repositoryConfigurationCacheFile;

	}

	public TracRepositoryConnector() {
		if (TracCorePlugin.getDefault() != null) {
			TracCorePlugin.getDefault().setConnector(this);
			IPath path = TracCorePlugin.getDefault().getRepostioryAttributeCachePath();
			repositoryConfigurationCacheFile = path.toFile();
		}
	}

	@Override
	public boolean canCreateNewTask(TaskRepository repository) {
		return true;
	}

	@Override
	public boolean canCreateTaskFromKey(TaskRepository repository) {
		return true;
	}

	private final TracWikiHandler wikiHandler = new TracWikiHandler(this);

	public boolean hasWiki(TaskRepository repository) {
		// check the access mode to validate Wiki support
		ITracClient client = getClientManager().getRepository(repository);
		if (client instanceof ITracWikiClient) {
			return true;
		}
		return false;
	}

	public AbstractWikiHandler getWikiHandler() {
		return wikiHandler;
	}

	@Override
	public String getLabel() {
		return CLIENT_LABEL;
	}

	@Override
	public String getConnectorKind() {
		return TracCorePlugin.REPOSITORY_KIND;
	}

	@Override
	public String getRepositoryUrlFromTaskUrl(String url) {
		if (url == null) {
			return null;
		}
		int index = url.lastIndexOf(ITracClient.TICKET_URL);
		return index == -1 ? null : url.substring(0, index);
	}

	@Override
	public String getTaskIdFromTaskUrl(String url) {
		if (url == null) {
			return null;
		}
		int index = url.lastIndexOf(ITracClient.TICKET_URL);
		return index == -1 ? null : url.substring(index + ITracClient.TICKET_URL.length());
	}

	@Override
	public String getTaskUrl(String repositoryUrl, String taskId) {
		return repositoryUrl + ITracClient.TICKET_URL + taskId;
	}

	@Override
	public AbstractAttachmentHandler getAttachmentHandler() {
		return attachmentHandler;
	}

	@Override
	public AbstractTaskDataHandler getTaskDataHandler() {
		return taskDataHandler;
	}

	@Override
	public IStatus performQuery(TaskRepository repository, AbstractRepositoryQuery query,
			AbstractTaskCollector resultCollector, SynchronizationEvent event, IProgressMonitor monitor) {
		try {
			monitor.beginTask("Querying repository", IProgressMonitor.UNKNOWN);

			final List<TracTicket> tickets = new ArrayList<TracTicket>();

			ITracClient client;
			try {
				client = getClientManager().getRepository(repository);
				if (query instanceof TracRepositoryQuery) {
					client.search(((TracRepositoryQuery) query).getTracSearch(), tickets, monitor);
				}

				client.updateAttributes(monitor, false);
				for (TracTicket ticket : tickets) {
					RepositoryTaskData taskData = taskDataHandler.createTaskDataFromTicket(client, repository, ticket,
							monitor);
					resultCollector.accept(taskData);
				}
			} catch (Throwable e) {
				return TracCorePlugin.toStatus(e, repository);
			}

			return Status.OK_STATUS;
		} finally {
			monitor.done();
		}
	}

	@Override
	public void preSynchronization(SynchronizationEvent event, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask("Getting changed tasks", IProgressMonitor.UNKNOWN);

			if (!event.fullSynchronization) {
				return;
			}

			// there are no Trac tasks in the task list, skip contacting the repository
			if (event.tasks.isEmpty()) {
				return;
			}

			TaskRepository repository = event.taskRepository;
			if (!TracRepositoryConnector.hasChangedSince(repository)) {
				// always run the queries for web mode
				return;
			}

			if (repository.getSynchronizationTimeStamp() == null
					|| repository.getSynchronizationTimeStamp().length() == 0) {
				for (AbstractTask task : event.tasks) {
					task.setStale(true);
				}
				return;
			}

			Date since = new Date(0);
			try {
				since = TracUtils.parseDate(Integer.parseInt(repository.getSynchronizationTimeStamp()));
			} catch (NumberFormatException e) {
			}

			try {
				ITracClient client = getClientManager().getRepository(repository);
				Set<Integer> ids = client.getChangedTickets(since, monitor);
				if (ids.isEmpty()) {
					// repository is unchanged
					event.performQueries = false;
					return;
				}

				if (ids.size() == 1) {
					// getChangedTickets() is expected to always return at least
					// one ticket because
					// the repository synchronization timestamp is set to the
					// most recent modification date
					Integer id = ids.iterator().next();
					Date lastChanged = client.getTicketLastChanged(id, monitor);
					if (since.equals(lastChanged)) {
						// repository didn't actually change
						event.performQueries = false;
						return;
					}
				}

				for (AbstractTask task : event.tasks) {
					Integer id = getTicketId(task.getTaskId());
					if (ids.contains(id)) {
						task.setStale(true);
					}
				}
			} catch (OperationCanceledException e) {
				throw e;
			} catch (Exception e) {
				// TODO catch TracException
				throw new CoreException(TracCorePlugin.toStatus(e, repository));
			}
		} finally {
			monitor.done();
		}
	}

	@Override
	public AbstractTask createTask(String repositoryUrl, String id, String summary) {
		TracTask tracTask = new TracTask(repositoryUrl, id, summary);
		tracTask.setCreationDate(new Date());
		return tracTask;
	}

	@Override
	public boolean updateTaskFromTaskData(TaskRepository taskRepository, AbstractTask task, RepositoryTaskData taskData) {
		TracTask tracTask = (TracTask) task;
		ITracClient client = getClientManager().getRepository(taskRepository);

		task.setSummary(taskData.getSummary());
		task.setOwner(taskData.getAttributeValue(RepositoryTaskAttribute.USER_ASSIGNED));
		task.setCompleted(TracTask.isCompleted(taskData.getStatus()));
		task.setUrl(taskRepository.getRepositoryUrl() + ITracClient.TICKET_URL + taskData.getTaskId());

		String priority = taskData.getAttributeValue(Attribute.PRIORITY.getTracKey());
		TracPriority[] tracPriorities = client.getPriorities();
		task.setPriority(TracTask.getTaskPriority(priority, tracPriorities).toString());

		Kind kind = TracTask.Kind.fromType(taskData.getAttributeValue(Attribute.TYPE.getTracKey()));
		task.setTaskKind((kind != null) ? kind.toString() : null);

		tracTask.setSupportsSubtasks(taskDataHandler.canInitializeSubTaskData(null, taskData));
		// TODO: Completion Date

		// TODO check return value
		return false;
	}

	public static int getTicketId(String taskId) throws CoreException {
		try {
			return Integer.parseInt(taskId);
		} catch (NumberFormatException e) {
			throw new CoreException(new Status(IStatus.ERROR, TracCorePlugin.PLUGIN_ID, IStatus.OK,
					"Invalid ticket id: " + taskId, e));
		}
	}

	public synchronized TracClientManager getClientManager() {
		if (clientManager == null) {
			clientManager = new TracClientManager(repositoryConfigurationCacheFile, taskRepositoryLocationFactory);
		}
		return clientManager;
	}

	public static boolean hasChangedSince(TaskRepository repository) {
		return Version.XML_RPC.name().equals(repository.getVersion());
	}

	public static boolean hasRichEditor(TaskRepository repository) {
		return Version.XML_RPC.name().equals(repository.getVersion());
	}

	public static boolean hasRichEditor(TaskRepository repository, AbstractTask task) {
		return hasRichEditor(repository);
	}

	public static boolean hasAttachmentSupport(TaskRepository repository, AbstractTask task) {
		return Version.XML_RPC.name().equals(repository.getVersion());
	}

	public void stop() {
		if (clientManager != null) {
			clientManager.writeCache();
		}
	}

	@Override
	public void updateAttributes(TaskRepository repository, IProgressMonitor monitor) throws CoreException {
		try {
			ITracClient client = getClientManager().getRepository(repository);
			client.updateAttributes(monitor, true);
		} catch (Exception e) {
			throw new CoreException(RepositoryStatus.createStatus(repository.getRepositoryUrl(), IStatus.WARNING,
					TracCorePlugin.PLUGIN_ID, "Could not update attributes"));
		}
	}

	public static String getDisplayUsername(TaskRepository repository) {
		AuthenticationCredentials credentials = repository.getCredentials(AuthenticationType.REPOSITORY);
		if (credentials != null && credentials.getUserName().length() > 0) {
			return ITracClient.DEFAULT_USERNAME;
		}
		return repository.getUserName();
	}

	@Override
	public String getTaskIdPrefix() {
		return "#";
	}

	public static TracTicket getTracTicket(TaskRepository repository, RepositoryTaskData data)
			throws InvalidTicketException, CoreException {
		TracTicket ticket = new TracTicket(getTicketId(data.getTaskId()));

		List<RepositoryTaskAttribute> attributes = data.getAttributes();
		for (RepositoryTaskAttribute attribute : attributes) {
			if (TracAttributeFactory.isInternalAttribute(attribute.getId())) {
				// ignore
			} else if (!attribute.isReadOnly()) {
				ticket.putValue(attribute.getId(), attribute.getValue());
			}
		}

		// set cc value
		StringBuilder sb = new StringBuilder();
		List<String> removeValues = data.getAttributeValues(RepositoryTaskAttribute.REMOVE_CC);
		List<String> values = data.getAttributeValues(RepositoryTaskAttribute.USER_CC);
		for (String user : values) {
			if (!removeValues.contains(user)) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(user);
			}
		}
		if (data.getAttributeValue(RepositoryTaskAttribute.NEW_CC).length() > 0) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(data.getAttributeValue(RepositoryTaskAttribute.NEW_CC));
		}
		if (RepositoryTaskAttribute.TRUE.equals(data.getAttributeValue(RepositoryTaskAttribute.ADD_SELF_CC))) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(repository.getUserName());
		}
		ticket.putBuiltinValue(Key.CC, sb.toString());

		RepositoryOperation operation = data.getSelectedOperation();
		if (operation != null) {
			String action = operation.getKnobName();
			if (!"leave".equals(action)) {
				if ("accept".equals(action)) {
					ticket.putValue("status", TracTask.Status.ASSIGNED.toStatusString());
					ticket.putValue("owner", getDisplayUsername(repository));
				} else if ("resolve".equals(action)) {
					ticket.putValue("status", TracTask.Status.CLOSED.toStatusString());
					ticket.putValue("resolution", operation.getOptionSelection());
				} else if ("reopen".equals(action)) {
					ticket.putValue("status", TracTask.Status.REOPENED.toStatusString());
					ticket.putValue("resolution", "");
				} else if ("reassign".equals(operation.getKnobName())) {
					ticket.putValue("status", TracTask.Status.NEW.toStatusString());
					ticket.putValue("owner", operation.getInputValue());
				}
			}
		}

		return ticket;
	}

	public TaskRepositoryLocationFactory getTaskRepositoryLocationFactory() {
		return taskRepositoryLocationFactory;
	}

	public synchronized void setTaskRepositoryLocationFactory(
			TaskRepositoryLocationFactory taskRepositoryLocationFactory) {
		this.taskRepositoryLocationFactory = taskRepositoryLocationFactory;
		if (this.clientManager != null) {
			clientManager.setTaskRepositoryLocationFactory(taskRepositoryLocationFactory);
		}
	}

	@Override
	public boolean hasCredentialsManagement() {
		return true;
	}

	@Override
	public RepositoryTaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor)
			throws CoreException {
		return getTaskDataHandler().getTaskData(repository, taskId, monitor);
	}

}