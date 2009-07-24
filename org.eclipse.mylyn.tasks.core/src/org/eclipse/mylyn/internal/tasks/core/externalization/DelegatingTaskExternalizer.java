/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Ken Sueda - XML serialization support
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core.externalization;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.AbstractTaskCategory;
import org.eclipse.mylyn.internal.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.internal.tasks.core.DayDateRange;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.internal.tasks.core.ITransferList;
import org.eclipse.mylyn.internal.tasks.core.LocalRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.core.LocalTask;
import org.eclipse.mylyn.internal.tasks.core.RepositoryModel;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.RepositoryTaskHandleUtil;
import org.eclipse.mylyn.internal.tasks.core.TaskActivityUtil;
import org.eclipse.mylyn.internal.tasks.core.TaskCategory;
import org.eclipse.mylyn.internal.tasks.core.TaskExternalizationException;
import org.eclipse.mylyn.internal.tasks.core.TaskTask;
import org.eclipse.mylyn.internal.tasks.core.UncategorizedTaskContainer;
import org.eclipse.mylyn.internal.tasks.core.WeekDateRange;
import org.eclipse.mylyn.tasks.core.AbstractTaskListMigrator;
import org.eclipse.mylyn.tasks.core.IAttributeContainer;
import org.eclipse.mylyn.tasks.core.IRepositoryManager;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;
import org.eclipse.mylyn.tasks.core.ITask.SynchronizationState;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Subclass externalizers must override the get*TagName() methods for the types of externalized items they support to
 * ensure that their externalizer does not externalize tasks from other connectors incorrectly.
 * 
 * These tag names uniquely identify the externalizer to be used to read the task from externalized form on disk.
 * 
 * The canCreateElementFor methods specify which tasks the externalizer should write to disk.
 * 
 * The TaskList is read on startup, so externalizers extending this should not perform any slow (i.e., network)
 * operations when overriding methods.
 * 
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public final class DelegatingTaskExternalizer {

	static final String DEFAULT_PRIORITY = PriorityLevel.P3.toString();

	static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.S z"; //$NON-NLS-1$

	static final String KEY_NOTIFIED_INCOMING = "NotifiedIncoming"; //$NON-NLS-1$

	public static final String KEY_NAME = "Name"; //$NON-NLS-1$

	public static final String KEY_LABEL = "Label"; //$NON-NLS-1$

	public static final String KEY_QUERY = "Query"; //$NON-NLS-1$

	public static final String KEY_QUERY_STRING = "QueryString"; //$NON-NLS-1$

	static final String KEY_HANDLE = "Handle"; //$NON-NLS-1$

	public static final String KEY_REPOSITORY_URL = "RepositoryUrl"; //$NON-NLS-1$

	public static final String KEY_CATEGORY = "Category"; //$NON-NLS-1$

	static final String VAL_ROOT = "Root"; //$NON-NLS-1$

	static final String KEY_SUBTASK = "SubTask"; //$NON-NLS-1$

	static final String KEY_KIND = "Kind"; //$NON-NLS-1$

	static final String KEY_TASK_CATEGORY = "Task" + KEY_CATEGORY; //$NON-NLS-1$

	static final String KEY_LINK = "Link"; //$NON-NLS-1$

	static final String KEY_PLAN = "Plan"; //$NON-NLS-1$

	static final String KEY_TIME_ESTIMATED = "Estimated"; //$NON-NLS-1$

	static final String KEY_ISSUEURL = "IssueURL"; //$NON-NLS-1$

	static final String KEY_NOTES = "Notes"; //$NON-NLS-1$

	static final String KEY_ACTIVE = "Active"; //$NON-NLS-1$

	static final String KEY_PRIORITY = "Priority"; //$NON-NLS-1$

	static final String KEY_PATH = "Path"; //$NON-NLS-1$

	static final String VAL_FALSE = "false"; //$NON-NLS-1$

	static final String VAL_TRUE = "true"; //$NON-NLS-1$

	static final String KEY_DATE_END = "EndDate"; //$NON-NLS-1$

	static final String KEY_QUERY_HIT = "QueryHit"; //$NON-NLS-1$

	static final String KEY_TASK_REFERENCE = "TaskReference"; //$NON-NLS-1$

	static final String KEY_DATE_CREATION = "CreationDate"; //$NON-NLS-1$

	static final String KEY_DATE_REMINDER = "ReminderDate"; //$NON-NLS-1$

	static final String KEY_DATE_SCHEDULED_START = "ScheduledStartDate"; //$NON-NLS-1$

	static final String KEY_DATE_SCHEDULED_END = "ScheduledEndDate"; //$NON-NLS-1$

	static final String KEY_DATE_MODIFICATION = "ModificationDate"; //$NON-NLS-1$

	static final String KEY_DATE_DUE = "DueDate"; //$NON-NLS-1$

	static final String KEY_REMINDED = "Reminded"; //$NON-NLS-1$

	static final String KEY_FLOATING = "Floating"; //$NON-NLS-1$

	/**
	 * This element holds the date stamp recorded upon last transition to a synchronized state.
	 */
	static final String KEY_LAST_MOD_DATE = "LastModified"; //$NON-NLS-1$

	static final String KEY_DIRTY = "Dirty"; //$NON-NLS-1$

	static final String KEY_SYNC_STATE = "offlineSyncState"; //$NON-NLS-1$

	static final String KEY_OWNER = "Owner"; //$NON-NLS-1$

	static final String KEY_MARK_READ_PENDING = "MarkReadPending"; //$NON-NLS-1$

	static final String KEY_STALE = "Stale"; //$NON-NLS-1$

	static final String KEY_CONNECTOR_KIND = "ConnectorKind"; //$NON-NLS-1$

	static final String KEY_TASK_ID = "TaskId"; //$NON-NLS-1$

	public static final String KEY_LAST_REFRESH = "LastRefreshTimeStamp"; //$NON-NLS-1$

	static final String NODE_ATTRIBUTE = "Attribute"; //$NON-NLS-1$

	static final String NODE_QUERY = "Query"; //$NON-NLS-1$

	static final String NODE_TASK = "Task"; //$NON-NLS-1$

	static final String KEY_KEY = "Key"; //$NON-NLS-1$

	// 2.0 -> 3.0 migration holds tasks to category handles 
	private final Map<AbstractTask, String> parentCategoryMap;

	private final RepositoryModel repositoryModel;

	private List<AbstractTaskListMigrator> migrators;

	private boolean taskActivated;

	private final IRepositoryManager repositoryManager;

	private final List<IStatus> errors;

	public DelegatingTaskExternalizer(RepositoryModel repositoryModel, IRepositoryManager repositoryManager) {
		Assert.isNotNull(repositoryModel);
		Assert.isNotNull(repositoryManager);
		this.repositoryModel = repositoryModel;
		this.repositoryManager = repositoryManager;
		this.parentCategoryMap = new HashMap<AbstractTask, String>();
		this.errors = new ArrayList<IStatus>();
		this.migrators = Collections.emptyList();
	}

	public void initialize(List<AbstractTaskListMigrator> migrators) {
		Assert.isNotNull(migrators);
		this.migrators = migrators;
	}

	public Element createCategoryElement(AbstractTaskCategory category, Document doc, Element parent) {
		Element node = doc.createElement(getCategoryTagName());
		node.setAttribute(DelegatingTaskExternalizer.KEY_HANDLE, category.getHandleIdentifier());
		node.setAttribute(DelegatingTaskExternalizer.KEY_NAME, category.getSummary());
		parent.appendChild(node);
		for (ITask task : category.getChildren()) {
			createTaskReference(KEY_TASK_REFERENCE, task, doc, node);
		}
		return node;
	}

	@SuppressWarnings("deprecation")
	public Element createTaskElement(final AbstractTask task, Document doc, Element parent) {
		final Element node;
		if (task.getClass() == TaskTask.class || task instanceof LocalTask) {
			node = doc.createElement(NODE_TASK);
		} else {
			errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN, "No externalizer for task: " + task)); //$NON-NLS-1$
			return null;
		}

		node.setAttribute(KEY_CONNECTOR_KIND, task.getConnectorKind());
		node.setAttribute(KEY_REPOSITORY_URL, task.getRepositoryUrl());
		node.setAttribute(KEY_TASK_ID, task.getTaskId());
		if (task.getTaskKey() != null) {
			node.setAttribute(KEY_KEY, task.getTaskKey());
		}
		node.setAttribute(KEY_HANDLE, task.getHandleIdentifier());
		node.setAttribute(KEY_LABEL, stripControlCharacters(task.getSummary()));

		node.setAttribute(KEY_PRIORITY, task.getPriority());
		node.setAttribute(KEY_KIND, task.getTaskKind());

		if (task.isActive()) {
			node.setAttribute(KEY_ACTIVE, VAL_TRUE);
		} else {
			node.setAttribute(KEY_ACTIVE, VAL_FALSE);
		}

		if (task.getUrl() != null) {
			node.setAttribute(KEY_ISSUEURL, task.getUrl());
		}
		node.setAttribute(KEY_NOTES, stripControlCharacters(task.getNotes()));
		node.setAttribute(KEY_TIME_ESTIMATED, "" + task.getEstimatedTimeHours()); //$NON-NLS-1$
		node.setAttribute(KEY_DATE_END, formatExternDate(task.getCompletionDate()));
		node.setAttribute(KEY_DATE_CREATION, formatExternDate(task.getCreationDate()));
		node.setAttribute(KEY_DATE_MODIFICATION, formatExternDate(task.getModificationDate()));
		node.setAttribute(KEY_DATE_DUE, formatExternDate(task.getDueDate()));
		if (task.getScheduledForDate() != null) {
			node.setAttribute(KEY_DATE_SCHEDULED_START, formatExternCalendar(task.getScheduledForDate().getStartDate()));
			node.setAttribute(KEY_DATE_SCHEDULED_END, formatExternCalendar(task.getScheduledForDate().getEndDate()));
		}
		if (task.isReminded()) {
			node.setAttribute(KEY_REMINDED, VAL_TRUE);
		} else {
			node.setAttribute(KEY_REMINDED, VAL_FALSE);
		}
		if (task.isStale()) {
			node.setAttribute(KEY_STALE, VAL_TRUE);
		} else {
			node.setAttribute(KEY_STALE, VAL_FALSE);
		}
		if (task.isMarkReadPending()) {
			node.setAttribute(KEY_MARK_READ_PENDING, VAL_TRUE);
		} else {
			node.setAttribute(KEY_MARK_READ_PENDING, VAL_FALSE);
		}
		if (task.getLastReadTimeStamp() != null) {
			node.setAttribute(KEY_LAST_MOD_DATE, task.getLastReadTimeStamp());
		}
		if (task.isNotified()) {
			node.setAttribute(KEY_NOTIFIED_INCOMING, VAL_TRUE);
		} else {
			node.setAttribute(KEY_NOTIFIED_INCOMING, VAL_FALSE);
		}
		if (task.getSynchronizationState() != null) {
			node.setAttribute(KEY_SYNC_STATE, task.getSynchronizationState().name());
		} else {
			node.setAttribute(KEY_SYNC_STATE, SynchronizationState.SYNCHRONIZED.name());
		}
		if (task.getOwner() != null) {
			node.setAttribute(KEY_OWNER, task.getOwner());
		}
		createAttributes(task, doc, node);
		for (ITask t : task.getChildren()) {
			createTaskReference(KEY_SUBTASK, t, doc, node);
		}

		parent.appendChild(node);
		return node;
	}

	private void createAttributes(IAttributeContainer container, Document doc, Element parent) {
		Map<String, String> attributes = container.getAttributes();
		for (Map.Entry<String, String> entry : attributes.entrySet()) {
			Element node = doc.createElement(NODE_ATTRIBUTE);
			node.setAttribute(KEY_KEY, entry.getKey());
			node.setTextContent(entry.getValue());
			parent.appendChild(node);
		}

	}

	/**
	 * creates nested task reference nodes named nodeName which include a handle to the task
	 * 
	 * @return
	 */
	public Element createTaskReference(String nodeName, ITask task, Document doc, Element parent) {
		Element node = doc.createElement(nodeName);
		node.setAttribute(KEY_HANDLE, task.getHandleIdentifier());
		parent.appendChild(node);
		return node;
	}

	/**
	 * create tasks from the nodes provided and places them within the given container
	 */
	public void readTaskReferences(AbstractTaskContainer task, NodeList nodes, ITransferList tasklist) {
		for (int j = 0; j < nodes.getLength(); j++) {
			Node child = nodes.item(j);
			Element element = (Element) child;
			if (element.hasAttribute(KEY_HANDLE)) {
				String handle = element.getAttribute(KEY_HANDLE);
				AbstractTask subTask = tasklist.getTask(handle);
				if (subTask != null) {
					tasklist.addTask(subTask, task);
				} else {
					errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN,
							"Failed to add subtask with handle \"" + handle + "\" to \"" + task + "\"")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
		}
	}

	@SuppressWarnings( { "restriction" })
	private String stripControlCharacters(String text) {
		if (text == null) {
			return ""; //$NON-NLS-1$
		}
		return org.eclipse.mylyn.internal.commons.core.XmlStringConverter.cleanXmlString(text);
	}

	private String formatExternDate(Date date) {
		if (date == null) {
			return ""; //$NON-NLS-1$
		}
		String f = DATE_FORMAT;
		SimpleDateFormat format = new SimpleDateFormat(f, Locale.ENGLISH);
		return format.format(date);
	}

	private String formatExternCalendar(Calendar date) {
		if (date == null) {
			return ""; //$NON-NLS-1$
		}
		String f = DATE_FORMAT;
		SimpleDateFormat format = new SimpleDateFormat(f, Locale.ENGLISH);
		return format.format(date.getTime());
	}

	public void readCategory(Node node, ITransferList taskList) {
		Element element = (Element) node;
		AbstractTaskCategory category = null;
		if (element.hasAttribute(KEY_NAME)) {
			String name = element.getAttribute(KEY_NAME);
			String handle = name;
			if (element.hasAttribute(KEY_HANDLE)) {
				handle = element.getAttribute(KEY_HANDLE);
			}
			category = taskList.getContainerForHandle(handle);
			if (category == null) {
				category = new TaskCategory(handle, name);
				taskList.addCategory((TaskCategory) category);
			} else if (!UncategorizedTaskContainer.HANDLE.equals(handle)) {
				errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN, "Category with handle \"" + name //$NON-NLS-1$
						+ "\" already exists in task list")); //$NON-NLS-1$
			}
		} else {
			errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN, "Category is missing name attribute")); //$NON-NLS-1$
			// LEGACY: registry categories did not have names
			// category = taskList.getArchiveContainer();
			// a null category will now go into appropriate orphaned category
		}

		NodeList list = node.getChildNodes();
		readTaskReferences(category, list, taskList);
	}

	@SuppressWarnings("deprecation")
	public final AbstractTask readTask(Node node, AbstractTaskCategory legacyCategory, ITask parent)
			throws CoreException {
		String handle;
		String taskId;
		String repositoryUrl;
		String summary = ""; //$NON-NLS-1$
		final Element element = (Element) node;
		if (element.hasAttribute(KEY_REPOSITORY_URL) && element.hasAttribute(KEY_TASK_ID)
				&& element.hasAttribute(KEY_HANDLE)) {
			handle = element.getAttribute(KEY_HANDLE);
			repositoryUrl = element.getAttribute(KEY_REPOSITORY_URL);
			taskId = element.getAttribute(KEY_TASK_ID);
		} else if (element.hasAttribute(KEY_HANDLE)) {
			handle = element.getAttribute(KEY_HANDLE);
			repositoryUrl = RepositoryTaskHandleUtil.getRepositoryUrl(handle);
			taskId = RepositoryTaskHandleUtil.getTaskId(handle);
		} else {
			errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN, "Task is missing handle attribute")); //$NON-NLS-1$
			return null;
		}
		if (element.hasAttribute(KEY_LABEL)) {
			summary = element.getAttribute(KEY_LABEL);
		}

		AbstractTask task = null;
		AbstractTaskListMigrator taskMigrator = null;
		if (NODE_TASK.equals(node.getNodeName())) {
			String connectorKind = element.getAttribute(DelegatingTaskExternalizer.KEY_CONNECTOR_KIND);
			task = readDefaultTask(connectorKind, repositoryUrl, taskId, summary, element);
		}
		// attempt migration from < 3.0 task list
		if (task == null) {
			for (AbstractTaskListMigrator migrator : migrators) {
				if (node.getNodeName().equals(migrator.getTaskElementName())) {
					task = readDefaultTask(migrator.getConnectorKind(), repositoryUrl, taskId, summary, element);
					taskMigrator = migrator;
					break;
				}
			}
		}
		// populate common attributes
		if (task != null) {
			if (repositoryManager.getRepositoryConnector(task.getConnectorKind()) == null) {
				errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN,
						"Missing connector for task with kind \"" + task.getConnectorKind() + "\"")); //$NON-NLS-1$ //$NON-NLS-2$
				return null;
			}

			readTaskInfo(task, element, parent, legacyCategory);
			readAttributes(task, element);
			if (taskMigrator != null) {
				if (task.getSynchronizationState() == SynchronizationState.INCOMING
						&& task.getLastReadTimeStamp() == null) {
					task.setSynchronizationState(SynchronizationState.INCOMING_NEW);
				}
				task.setTaskKey(task.getTaskId());
				final AbstractTaskListMigrator finalTaskMigrator = taskMigrator;
				final AbstractTask finalTask = task;
				SafeRunner.run(new ISafeRunnable() {

					public void handleException(Throwable e) {
						errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN,
								"Task migration failed for task \"" + finalTask + "\"", e)); //$NON-NLS-1$ //$NON-NLS-2$
					}

					public void run() throws Exception {
						finalTaskMigrator.migrateTask(finalTask, element);
					}

				});
			}
			return task;
		} else {
			errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN, "Missing connector for task node \"" //$NON-NLS-1$
					+ node.getNodeName() + "\"")); //$NON-NLS-1$
			return null;
		}
	}

	private void readAttributes(IAttributeContainer container, Element parent) {
		NodeList list = parent.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child instanceof Element && child.getNodeName().equals(NODE_ATTRIBUTE)) {
				Element element = (Element) child;
				String key = element.getAttribute(KEY_KEY);
				if (key.length() > 0) {
					container.setAttribute(key, element.getTextContent());
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void readTaskInfo(AbstractTask task, Element element, ITask parent, AbstractTaskCategory legacyCategory) {
		if (element.hasAttribute(KEY_CATEGORY)) {
			// Migration 2.0 -> 3.0 task list.  Category no longer maintained on the task element but
			// task handles held within category nodes similar to query children
			String categoryHandle = element.getAttribute(KEY_CATEGORY);
			if (categoryHandle.equals(VAL_ROOT)) {
				categoryHandle = UncategorizedTaskContainer.HANDLE;
			}
			//task.setCategoryHandle(categoryHandle);
			parentCategoryMap.put(task, categoryHandle);
		}
		if (element.hasAttribute(KEY_PRIORITY)) {
			task.setPriority(element.getAttribute(KEY_PRIORITY));
		} else {
			task.setPriority(DEFAULT_PRIORITY);
		}
		if (element.hasAttribute(KEY_KIND)) {
			task.setTaskKind(element.getAttribute(KEY_KIND));
		}
		if (!taskActivated && element.getAttribute(KEY_ACTIVE).compareTo(VAL_TRUE) == 0) {
			task.setActive(true);
			taskActivated = true;
		} else {
			task.setActive(false);
		}
		if (element.hasAttribute(KEY_ISSUEURL)) {
			task.setUrl(element.getAttribute(KEY_ISSUEURL));
		} else {
			task.setUrl(""); //$NON-NLS-1$
		}
		if (element.hasAttribute(KEY_NOTES)) {
			task.setNotes(element.getAttribute(KEY_NOTES));
		} else {
			task.setNotes(""); //$NON-NLS-1$
		}
		if (element.hasAttribute(KEY_TIME_ESTIMATED)) {
			String est = element.getAttribute(KEY_TIME_ESTIMATED);
			try {
				int estimate = Integer.parseInt(est);
				task.setEstimatedTimeHours(estimate);
			} catch (Exception e) {
				task.setEstimatedTimeHours(0);
			}
		} else {
			task.setEstimatedTimeHours(0);
		}
		if (element.hasAttribute(KEY_DATE_END)) {
			task.setCompletionDate(getDateFromString(element.getAttribute(KEY_DATE_END)));
		} else {
			task.setCompletionDate(null);
		}
		if (element.hasAttribute(KEY_DATE_CREATION)) {
			task.setCreationDate(getDateFromString(element.getAttribute(KEY_DATE_CREATION)));
		} else {
			task.setCreationDate(null);
		}
		if (element.hasAttribute(KEY_DATE_MODIFICATION)) {
			task.setModificationDate(getDateFromString(element.getAttribute(KEY_DATE_MODIFICATION)));
		} else {
			task.setModificationDate(null);
		}
		if (element.hasAttribute(KEY_DATE_DUE)) {
			task.setDueDate(getDateFromString(element.getAttribute(KEY_DATE_DUE)));
		} else {
			task.setDueDate(null);
		}
		// Legacy 2.3.2 -> 3.0 migration of scheduled date
		boolean isFloating = false;
		if (element.hasAttribute(KEY_FLOATING) && element.getAttribute(KEY_FLOATING).compareTo(VAL_TRUE) == 0) {
			isFloating = true;
		} else {
			isFloating = false;
		}
		if (element.hasAttribute(KEY_DATE_REMINDER)) {
			Date date = getDateFromString(element.getAttribute(KEY_DATE_REMINDER));
			if (date != null) {
				if (isFloating) {
					task.setScheduledForDate(TaskActivityUtil.getWeekOf(date));
				} else {
					task.setScheduledForDate(TaskActivityUtil.getDayOf(date));
				}
			}
		}
		// Scheduled date range (3.0)
		if (element.hasAttribute(KEY_DATE_SCHEDULED_START) && element.hasAttribute(KEY_DATE_SCHEDULED_END)) {
			Date startDate = getDateFromString(element.getAttribute(KEY_DATE_SCHEDULED_START));
			Date endDate = getDateFromString(element.getAttribute(KEY_DATE_SCHEDULED_END));
			if (startDate != null && endDate != null && startDate.compareTo(endDate) <= 0) {
				Calendar calStart = TaskActivityUtil.getCalendar();
				calStart.setTime(startDate);
				Calendar calEnd = TaskActivityUtil.getCalendar();
				calEnd.setTime(endDate);
				if (DayDateRange.isDayRange(calStart, calEnd)) {
					task.setScheduledForDate(new DayDateRange(calStart, calEnd));
				} else if (WeekDateRange.isWeekRange(calStart, calEnd)) {
					task.setScheduledForDate(new WeekDateRange(calStart, calEnd));
				} else {
					// Neither week nor day found, default to today 
					task.setScheduledForDate(TaskActivityUtil.getDayOf(new Date()));
				}
			}
		}
		if (element.hasAttribute(KEY_REMINDED) && element.getAttribute(KEY_REMINDED).compareTo(VAL_TRUE) == 0) {
			task.setReminded(true);
		} else {
			task.setReminded(false);
		}
		if (element.hasAttribute(KEY_STALE) && element.getAttribute(KEY_STALE).compareTo(VAL_TRUE) == 0) {
			task.setStale(true);
		} else {
			task.setStale(false);
		}
		if (element.hasAttribute(KEY_MARK_READ_PENDING)
				&& element.getAttribute(KEY_MARK_READ_PENDING).compareTo(VAL_TRUE) == 0) {
			task.setMarkReadPending(true);
		} else {
			task.setMarkReadPending(false);
		}
		task.setSynchronizing(false);
		if (element.hasAttribute(KEY_REPOSITORY_URL)) {
			task.setRepositoryUrl(element.getAttribute(KEY_REPOSITORY_URL));
		}
		if (element.hasAttribute(KEY_LAST_MOD_DATE) && !element.getAttribute(KEY_LAST_MOD_DATE).equals("")) { //$NON-NLS-1$
			task.setLastReadTimeStamp(element.getAttribute(KEY_LAST_MOD_DATE));
		}
		if (element.hasAttribute(KEY_OWNER)) {
			task.setOwner(element.getAttribute(KEY_OWNER));
		}
		if (VAL_TRUE.equals(element.getAttribute(KEY_NOTIFIED_INCOMING))) {
			task.setNotified(true);
		} else {
			task.setNotified(false);
		}
		if (element.hasAttribute(KEY_SYNC_STATE)) {
			try {
				SynchronizationState state = SynchronizationState.valueOf(element.getAttribute(KEY_SYNC_STATE));
				task.setSynchronizationState(state);
			} catch (IllegalArgumentException e) {
				// invalid sync state, ignore
				// TODO log this to a multi-status
			}
		}
		if (element.hasAttribute(KEY_KEY)) {
			task.setTaskKey(element.getAttribute(KEY_KEY));
		} else {
			task.setTaskKey(null);
		}
	}

	private Date getDateFromString(String dateString) {
		Date date = null;
		if ("".equals(dateString)) { //$NON-NLS-1$
			return null;
		}
		String formatString = DATE_FORMAT;
		SimpleDateFormat format = new SimpleDateFormat(formatString, Locale.ENGLISH);
		try {
			date = format.parse(dateString);
		} catch (ParseException e) {
			errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN, "Could not parse date \"" //$NON-NLS-1$
					+ dateString + "\"", e)); //$NON-NLS-1$
		}
		return date;
	}

	private String getCategoryTagName() {
		return KEY_TASK_CATEGORY;
	}

	public Element createQueryElement(final RepositoryQuery query, Document doc, Element parent) {
		final Element node;
		if (query.getClass() == RepositoryQuery.class) {
			node = doc.createElement(NODE_QUERY);
		} else {
			errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN,
					"Missing factory to externalize query \"" + query + "\"")); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}

		node.setAttribute(KEY_HANDLE, query.getHandleIdentifier());
		node.setAttribute(KEY_CONNECTOR_KIND, query.getConnectorKind());
		node.setAttribute(KEY_NAME, query.getSummary());
		node.setAttribute(KEY_QUERY_STRING, query.getUrl());
		node.setAttribute(KEY_REPOSITORY_URL, query.getRepositoryUrl());
		if (query.getLastSynchronizedTimeStamp() != null) {
			node.setAttribute(KEY_LAST_REFRESH, query.getLastSynchronizedTimeStamp());
		}
		createAttributes(query, doc, node);
		for (ITask hit : query.getChildren()) {
			createTaskReference(KEY_QUERY_HIT, hit, doc, node);
		}

		parent.appendChild(node);
		return node;
	}

	public Map<AbstractTask, String> getLegacyParentCategoryMap() {
		return parentCategoryMap;
	}

	/**
	 * Reads the Query from the specified Node. If taskList is not null, then also adds this query to the TaskList
	 * 
	 * @throws TaskExternalizationException
	 */
	public RepositoryQuery readQuery(Node node) {
		final Element element = (Element) node;
		String repositoryUrl = element.getAttribute(DelegatingTaskExternalizer.KEY_REPOSITORY_URL);
		String queryString = element.getAttribute(KEY_QUERY_STRING);
		if (queryString.length() == 0) { // fall back for legacy
			queryString = element.getAttribute(KEY_QUERY);
		}
		String label = element.getAttribute(DelegatingTaskExternalizer.KEY_NAME);
		if (label.length() == 0) { // fall back for legacy
			label = element.getAttribute(DelegatingTaskExternalizer.KEY_LABEL);
		}

		AbstractTaskListMigrator queryMigrator = null;
		RepositoryQuery query = null;
		if (NODE_QUERY.equals(node.getNodeName())) {
			String connectorKind = element.getAttribute(DelegatingTaskExternalizer.KEY_CONNECTOR_KIND);
			query = readDefaultQuery(connectorKind, repositoryUrl, queryString, label, element);
		}
		// attempt migration from < 3.0 task list
		if (query == null) {
			for (AbstractTaskListMigrator migrator : migrators) {
				Set<String> queryTagNames = migrator.getQueryElementNames();
				if (queryTagNames != null && queryTagNames.contains(node.getNodeName())) {
					query = readDefaultQuery(migrator.getConnectorKind(), repositoryUrl, queryString, label, element);
					queryMigrator = migrator;
					break;
				}
			}
		}
		// populate common attributes
		if (query != null) {
			if (repositoryManager.getRepositoryConnector(query.getConnectorKind()) == null) {
				errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN,
						"Missing connector for query with kind \"" + query.getConnectorKind() + "\"")); //$NON-NLS-1$ //$NON-NLS-2$
				return null;
			}

			if (element.getAttribute(DelegatingTaskExternalizer.KEY_LAST_REFRESH) != null
					&& !element.getAttribute(DelegatingTaskExternalizer.KEY_LAST_REFRESH).equals("")) { //$NON-NLS-1$
				query.setLastSynchronizedStamp(element.getAttribute(DelegatingTaskExternalizer.KEY_LAST_REFRESH));
			}
			String handle = element.getAttribute(DelegatingTaskExternalizer.KEY_HANDLE);
			if (handle.length() > 0) {
				query.setHandleIdentifier(handle);
			}
			readAttributes(query, element);
			if (queryMigrator != null) {
				query.setHandleIdentifier(label);
				final AbstractTaskListMigrator finalQueryMigrator = queryMigrator;
				final RepositoryQuery finalQuery = query;
				SafeRunner.run(new ISafeRunnable() {

					public void handleException(Throwable e) {
						errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN,
								"Query migration failed for query \"" + finalQuery + "\"", e)); //$NON-NLS-1$ //$NON-NLS-2$
					}

					public void run() throws Exception {
						finalQueryMigrator.migrateQuery(finalQuery, element);
					}

				});
			}
			return query;
		} else {
			errors.add(new Status(IStatus.WARNING, ITasksCoreConstants.ID_PLUGIN, "Missing connector for query node \"" //$NON-NLS-1$
					+ node.getNodeName() + "\"")); //$NON-NLS-1$
			return null;
		}
	}

	private RepositoryQuery readDefaultQuery(String connectorKind, String repositoryUrl, String queryString,
			String label, Element childElement) {
		TaskRepository taskRepository = repositoryModel.getTaskRepository(connectorKind, repositoryUrl);
		IRepositoryQuery query = repositoryModel.createRepositoryQuery(taskRepository);
		query.setSummary(label);
		query.setUrl(queryString);
		return (RepositoryQuery) query;
	}

	private AbstractTask readDefaultTask(String connectorKind, String repositoryUrl, String taskId, String summary,
			Element element) {
		TaskRepository taskRepository = repositoryModel.getTaskRepository(connectorKind, repositoryUrl);
		if (repositoryUrl.equals(LocalRepositoryConnector.REPOSITORY_URL)) {
			LocalTask task = new LocalTask(taskId, summary);
			return task;
		}
		ITask task = repositoryModel.createTask(taskRepository, taskId);
		task.setSummary(summary);
		return (AbstractTask) task;
	}

	public void reset() {
		parentCategoryMap.clear();
		errors.clear();
	}

	public void clearErrorStatus() {
		errors.clear();
	}

	public Status getErrorStatus() {
		if (errors.size() > 0) {
			return new MultiStatus(ITasksCoreConstants.ID_PLUGIN, 0, errors.toArray(new IStatus[0]),
					"Problems encounted while externalizing task list", null); //$NON-NLS-1$
		}
		return null;
	}

}
