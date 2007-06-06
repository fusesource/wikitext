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

package org.eclipse.mylar.internal.bugzilla.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.IAttachmentHandler;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskDataHandler;
import org.eclipse.mylar.tasks.core.ITaskFactory;
import org.eclipse.mylar.tasks.core.QueryHitCollector;
import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;
import org.eclipse.mylar.tasks.core.TaskComment;
import org.eclipse.mylar.tasks.core.TaskList;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.core.UnrecognizedReponseException;
import org.eclipse.mylar.tasks.core.Task.PriorityLevel;

/**
 * @author Mik Kersten
 * @author Rob Elves
 */
public class BugzillaRepositoryConnector extends AbstractRepositoryConnector {

	private static final String BUG_ID = "&bug_id=";

	private static final String CHANGED_BUGS_CGI_ENDDATE = "&chfieldto=Now";

	private static final String CHANGED_BUGS_CGI_QUERY = "/buglist.cgi?query_format=advanced&chfieldfrom=";

	private static final String CLIENT_LABEL = "Bugzilla (supports uncustomized 2.18-3.0)";

	private static final String COMMENT_FORMAT = "yyyy-MM-dd HH:mm";

	private static final String DEADLINE_FORMAT = "yyyy-MM-dd";

	private BugzillaAttachmentHandler attachmentHandler;

	private BugzillaTaskDataHandler taskDataHandler;

	private BugzillaClientManager clientManager;

	@Override
	public void init(TaskList taskList) {
		super.init(taskList);
		this.taskDataHandler = new BugzillaTaskDataHandler(this);
		this.attachmentHandler = new BugzillaAttachmentHandler(this);
		BugzillaCorePlugin.setConnector(this);
	}

	@Override
	public String getLabel() {
		return CLIENT_LABEL;
	}

	@Override
	public IAttachmentHandler getAttachmentHandler() {
		return attachmentHandler;
	}

	@Override
	public ITaskDataHandler getTaskDataHandler() {
		return taskDataHandler;
	}

	@Override
	public String getRepositoryType() {
		return BugzillaCorePlugin.REPOSITORY_KIND;
	}

	public AbstractRepositoryTask createTask(String repositoryUrl, String id, String summary) {
		BugzillaTask task = new BugzillaTask(repositoryUrl, id, summary);
		task.setCreationDate(new Date());
		return task;
	}

	public void updateTaskFromTaskData(TaskRepository repository, AbstractRepositoryTask repositoryTask,
			RepositoryTaskData taskData) {
		BugzillaTask bugzillaTask = (BugzillaTask) repositoryTask;
		if (taskData != null) {

////			// subtasks
//			repositoryTask.dropSubTasks();
//			Set<String> subTaskIds = taskDataHandler.getSubTaskIds(taskData);
//			if (subTaskIds != null && !subTaskIds.isEmpty()) {
//				for (String subId : subTaskIds) {
//					ITask subTask = taskList.getTask(repository.getUrl(), subId);
////					if (subTask == null && retrieveSubTasks) {
////						if (!subId.trim().equals(taskData.getId()) && !subId.equals("")) {
////							try {
////								subTask = createTaskFromExistingId(repository, subId, false, new NullProgressMonitor());
////							} catch (CoreException e) {
////								// ignore
////							}
////						}
////					}
//					if (subTask != null) {
//						bugzillaTask.addSubTask(subTask);
//					}
//				}
//			}

			// Summary
			String summary = taskData.getSummary();
			bugzillaTask.setSummary(summary);

			// Owner
			String owner = taskData.getAssignedTo();
			if (owner != null && !owner.equals("")) {
				bugzillaTask.setOwner(owner);
			}

			// Completed
			boolean isComplete = false;
			// TODO: use repository configuration to determine what -completed-
			// states are
			if (taskData.getStatus() != null) {
				isComplete = taskData.getStatus().equals(IBugzillaConstants.VALUE_STATUS_RESOLVED)
						|| taskData.getStatus().equals(IBugzillaConstants.VALUE_STATUS_CLOSED)
						|| taskData.getStatus().equals(IBugzillaConstants.VALUE_STATUS_VERIFIED);
			}
			bugzillaTask.setCompleted(isComplete);

			// Completion Date
			if (isComplete) {
				Date completionDate = null;
				try {

					List<TaskComment> taskComments = taskData.getComments();
					if (taskComments != null && !taskComments.isEmpty()) {
						// TODO: fix not to be based on comment
						completionDate = new SimpleDateFormat(COMMENT_FORMAT).parse(taskComments.get(
								taskComments.size() - 1).getCreated());

					}

				} catch (Exception e) {

				}

				if (bugzillaTask.getCompletionDate() != null && completionDate != null) {
					// if changed:
					// TODO: get taskListManger.setDueDate(ITask task, Date
					// dueDate)
				}
				bugzillaTask.setCompletionDate(completionDate);

			}

			// Priority
			String priority = PriorityLevel.getDefault().toString();
			if (taskData.getAttribute(RepositoryTaskAttribute.PRIORITY) != null) {
				priority = taskData.getAttribute(RepositoryTaskAttribute.PRIORITY).getValue();
			}
			bugzillaTask.setPriority(priority);

			// Task Web Url
			String url = getTaskWebUrl(repository.getUrl(), taskData.getId());
			if (url != null) {
				bugzillaTask.setTaskUrl(url);
			}

			// Bugzilla Specific Attributes

			// Product
			if (taskData.getProduct() != null) {
				bugzillaTask.setProduct(taskData.getProduct());
			}

			// Severity
			String severity = taskData.getAttributeValue(BugzillaReportElement.BUG_SEVERITY.getKeyString());
			if (severity != null && !severity.equals("")) {
				bugzillaTask.setSeverity(severity);
			}

			// Due Date
			if (taskData.getAttribute(BugzillaReportElement.ESTIMATED_TIME.getKeyString()) != null) {
				Date dueDate = null;
				// HACK: if estimated_time field exists, time tracking is
				// enabled
				try {
					String dueStr = taskData.getAttributeValue(BugzillaReportElement.DEADLINE.getKeyString());
					if (dueStr != null) {
						dueDate = new SimpleDateFormat(DEADLINE_FORMAT).parse(dueStr);
					}
				} catch (Exception e) {
					// ignore
				}
				bugzillaTask.setDueDate(dueDate);
			}

		}
	}


	@Override
	public Set<AbstractRepositoryTask> getChangedSinceLastSync(TaskRepository repository,
			Set<AbstractRepositoryTask> tasks) throws CoreException {
		try {
			Set<AbstractRepositoryTask> changedTasks = new HashSet<AbstractRepositoryTask>();

			if (repository.getSyncTimeStamp() == null) {
				return tasks;
			}

			String dateString = repository.getSyncTimeStamp();
			if (dateString == null) {
				dateString = "";
			}
			String urlQueryBase;
			String urlQueryString;

			urlQueryBase = repository.getUrl() + CHANGED_BUGS_CGI_QUERY
					+ URLEncoder.encode(dateString, repository.getCharacterEncoding()) + CHANGED_BUGS_CGI_ENDDATE;

			urlQueryString = urlQueryBase + BUG_ID;

			int queryCounter = -1;
			Iterator<AbstractRepositoryTask> itr = tasks.iterator();
			while (itr.hasNext()) {
				queryCounter++;
				AbstractRepositoryTask task = itr.next();
				String newurlQueryString = URLEncoder.encode(task.getTaskId() + ",", repository.getCharacterEncoding());
				if ((urlQueryString.length() + newurlQueryString.length() + IBugzillaConstants.CONTENT_TYPE_RDF
						.length()) > IBugzillaConstants.MAX_URL_LENGTH) {
					queryForChanged(repository, changedTasks, urlQueryString);
					queryCounter = 0;
					urlQueryString = urlQueryBase + BUG_ID;
					urlQueryString += newurlQueryString;
				} else if (!itr.hasNext()) {
					urlQueryString += newurlQueryString;
					queryForChanged(repository, changedTasks, urlQueryString);
				} else {
					urlQueryString += newurlQueryString;
				}
			}
			return changedTasks;
		} catch (UnsupportedEncodingException e) {
			MylarStatusHandler.fail(e, "Repository configured with unsupported encoding: "
					+ repository.getCharacterEncoding() + "\n\n Unable to determine changed tasks.", true);
			return tasks;
		}
	}

	private void queryForChanged(final TaskRepository repository, Set<AbstractRepositoryTask> changedTasks,
			String urlQueryString) throws UnsupportedEncodingException, CoreException {
		QueryHitCollector collector = new QueryHitCollector(taskList, new ITaskFactory() {

			public AbstractRepositoryTask createTask(RepositoryTaskData taskData, boolean synchData, boolean forced, IProgressMonitor monitor) {
				// do not construct actual task objects here as query shouldn't result in new tasks
				return taskList.getTask(taskData.getRepositoryUrl(), taskData.getId());
			}
		});

		BugzillaRepositoryQuery query = new BugzillaRepositoryQuery(repository.getUrl(), urlQueryString, "", taskList);

		performQuery(query, repository, new NullProgressMonitor(), collector, false);

		for (ITask taskHit : collector.getTaskHits()) {
			// String handle =
			// AbstractRepositoryTask.getHandle(repository.getUrl(),
			// hit.getId());
			if (taskHit != null && taskHit instanceof AbstractRepositoryTask) {
				AbstractRepositoryTask repositoryTask = (AbstractRepositoryTask) taskHit;
				// Hack to avoid re-syncing last task from previous
				// synchronization
				// This can be removed once we are getting a query timestamp
				// from the repository rather than
				// using the last modified stamp of the last task modified in
				// the return hits.
				// (or the changeddate field in the hit rdf becomes consistent,
				// currently it doesn't return a proper modified date string)
				// if (repositoryTask.getTaskData() != null
				// &&
				// repositoryTask.getTaskData().getLastModified().equals(repository.getSyncTimeStamp()))
				// {
				// // String taskId =
				// //
				// RepositoryTaskHandleUtil.getTaskId(repositoryTask.getHandleIdentifier());
				// RepositoryTaskData taskData =
				// getTaskDataHandler().getTaskData(repository,
				// repositoryTask.getTaskId());
				// if (taskData != null &&
				// taskData.getLastModified().equals(repository.getSyncTimeStamp()))
				// {
				// continue;
				// }
				// }
				changedTasks.add(repositoryTask);
			}
		}
	}

	@Override
	public boolean canCreateTaskFromKey(TaskRepository repository) {
		return true;
	}

	@Override
	public boolean canCreateNewTask(TaskRepository repository) {
		return true;
	}

	@Override
	public IStatus performQuery(final AbstractRepositoryQuery query, TaskRepository repository,
			IProgressMonitor monitor, QueryHitCollector resultCollector, boolean forced) {
		IStatus queryStatus = Status.OK_STATUS;
		try {
			BugzillaClient client = getClientManager().getClient(repository);
			resultCollector.clear();
			Set<String> ids = client.getSearchHits(query);
			if (ids.size() == 0) {
				// XXX: HACK in case of ip change bugzilla can return 0 hits
				// due to invalid authorization token, forcing relogin fixes
				client.logout();
				ids = client.getSearchHits(query);
			}

			if (!forced) {
				// Only retrieve data for hits we don't already have
				for (ITask existingTask : query.getChildren()) {
					AbstractRepositoryTask repositoryTask = (AbstractRepositoryTask) existingTask;
					if (ids.contains(repositoryTask.getTaskId())) {
						resultCollector.accept(repositoryTask);
						ids.remove(repositoryTask.getTaskId());
					}
				}
			}

			Map<String, RepositoryTaskData> hits = client.getTaskData(ids);
			for (RepositoryTaskData data : hits.values()) {
				if (data != null) {
					taskDataHandler.configureTaskData(repository, data);
					resultCollector.accept(data);
				}
			}

		} catch (UnrecognizedReponseException e) {
			queryStatus = new Status(IStatus.ERROR, BugzillaCorePlugin.PLUGIN_ID, Status.INFO,
					"Unrecognized response from server", e);
		} catch (IOException e) {
			queryStatus = new Status(IStatus.ERROR, BugzillaCorePlugin.PLUGIN_ID, Status.ERROR,
					"Check repository configuration: " + e.getMessage(), e);
		} catch (CoreException e) {
			queryStatus = e.getStatus();
		}
		return queryStatus;

	}

	@Override
	public String getRepositoryUrlFromTaskUrl(String url) {
		if (url == null) {
			return null;
		}
		int index = url.indexOf(IBugzillaConstants.URL_GET_SHOW_BUG);
		return index == -1 ? null : url.substring(0, index);
	}

	@Override
	public String getTaskIdFromTaskUrl(String url) {
		if (url == null) {
			return null;
		}
		int index = url.indexOf(IBugzillaConstants.URL_GET_SHOW_BUG);
		return index == -1 ? null : url.substring(index + IBugzillaConstants.URL_GET_SHOW_BUG.length());
	}

	@Override
	public String getTaskWebUrl(String repositoryUrl, String taskId) {
		try {
			return BugzillaClient.getBugUrlWithoutLogin(repositoryUrl, taskId);
		} catch (Exception ex) {
			MylarStatusHandler.fail(ex, "Error constructing task url for " + repositoryUrl + "  id:" + taskId, false);
		}
		return null;
	}

	@Override
	public void updateTaskFromRepository(TaskRepository repository, AbstractRepositoryTask repositoryTask,
			IProgressMonitor monitor) {
		// ignore
	}

	@Override
	public String getTaskIdPrefix() {
		return "bug";
	}

	public BugzillaClientManager getClientManager() {
		if (clientManager == null) {
			clientManager = new BugzillaClientManager();
		}
		return clientManager;
	}

	@Override
	public void updateAttributes(TaskRepository repository, IProgressMonitor monitor) throws CoreException {
		if (repository != null) {
			BugzillaCorePlugin.getRepositoryConfiguration(repository, true);
		}
	}

	public void updateAttributeOptions(TaskRepository taskRepository, RepositoryTaskData existingReport)
			throws CoreException {
		String product = existingReport.getAttributeValue(BugzillaReportElement.PRODUCT.getKeyString());
		for (RepositoryTaskAttribute attribute : existingReport.getAttributes()) {
			BugzillaReportElement element = BugzillaReportElement.valueOf(attribute.getID().trim().toUpperCase(
					Locale.ENGLISH));
			attribute.clearOptions();
			List<String> optionValues = BugzillaCorePlugin.getRepositoryConfiguration(taskRepository, false)
					.getOptionValues(element, product);
			if (element != BugzillaReportElement.OP_SYS && element != BugzillaReportElement.BUG_SEVERITY
					&& element != BugzillaReportElement.PRIORITY && element != BugzillaReportElement.BUG_STATUS) {
				Collections.sort(optionValues);
			}
			if (element == BugzillaReportElement.TARGET_MILESTONE && optionValues.isEmpty()) {

				existingReport.removeAttribute(BugzillaReportElement.TARGET_MILESTONE);
				continue;
			}
			attribute.clearOptions();
			for (String option : optionValues) {
				attribute.addOption(option, option);
			}

			// TODO: bug#162428, bug#150680 - something along the lines of...
			// but must think about the case of multiple values selected etc.
			// if(attribute.hasOptions()) {
			// if(!attribute.getOptionValues().containsKey(attribute.getValue()))
			// {
			// // updateAttributes()
			// }
			// }
		}

	}

	/**
	 * Adds bug attributes to new bug model and sets defaults
	 * 
	 * @param proxySettings
	 *            TODO
	 * @param characterEncoding
	 *            TODO
	 * 
	 */
	public static void setupNewBugAttributes(TaskRepository taskRepository, RepositoryTaskData newTaskData)
			throws CoreException {

		String product = newTaskData.getProduct();

		newTaskData.removeAllAttributes();

		RepositoryConfiguration repositoryConfiguration = BugzillaCorePlugin.getRepositoryConfiguration(taskRepository,
				false);

		RepositoryTaskAttribute a = BugzillaClient.makeNewAttribute(BugzillaReportElement.PRODUCT);
		List<String> optionValues = repositoryConfiguration.getProducts();
		Collections.sort(optionValues);
		// for (String option : optionValues) {
		// a.addOptionValue(option, option);
		// }
		a.setValue(product);
		a.setReadOnly(true);

		newTaskData.addAttribute(BugzillaReportElement.PRODUCT.getKeyString(), a);
		// attributes.put(a.getName(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.BUG_STATUS);
		optionValues = repositoryConfiguration.getStatusValues();
		for (String option : optionValues) {
			a.addOption(option, option);
		}
		a.setValue(IBugzillaConstants.VALUE_STATUS_NEW);

		newTaskData.addAttribute(BugzillaReportElement.BUG_STATUS.getKeyString(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.SHORT_DESC);
		newTaskData.addAttribute(BugzillaReportElement.SHORT_DESC.getKeyString(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.VERSION);
		optionValues = repositoryConfiguration.getVersions(newTaskData.getProduct());
		Collections.sort(optionValues);
		for (String option : optionValues) {
			a.addOption(option, option);
		}
		if (optionValues != null && optionValues.size() > 0) {
			a.setValue(optionValues.get(optionValues.size() - 1));
		}

		newTaskData.addAttribute(BugzillaReportElement.VERSION.getKeyString(), a);
		// attributes.put(a.getName(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.COMPONENT);
		optionValues = repositoryConfiguration.getComponents(newTaskData.getProduct());
		Collections.sort(optionValues);
		for (String option : optionValues) {
			a.addOption(option, option);
		}
		if (optionValues != null && optionValues.size() > 0) {
			a.setValue(optionValues.get(0));
		}

		newTaskData.addAttribute(BugzillaReportElement.COMPONENT.getKeyString(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.REP_PLATFORM);
		optionValues = repositoryConfiguration.getPlatforms();
		Collections.sort(optionValues);
		for (String option : optionValues) {
			a.addOption(option, option);
		}
		if (optionValues != null && optionValues.size() > 0) {
			a.setValue(optionValues.get(0));
		}

		newTaskData.addAttribute(BugzillaReportElement.REP_PLATFORM.getKeyString(), a);
		// attributes.put(a.getName(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.OP_SYS);
		optionValues = repositoryConfiguration.getOSs();
		for (String option : optionValues) {
			a.addOption(option, option);
		}
		if (optionValues != null && optionValues.size() > 0) {
			a.setValue(optionValues.get(optionValues.size() - 1));
		}

		newTaskData.addAttribute(BugzillaReportElement.OP_SYS.getKeyString(), a);
		// attributes.put(a.getName(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.PRIORITY);
		optionValues = repositoryConfiguration.getPriorities();
		for (String option : optionValues) {
			a.addOption(option, option);
		}
		a.setValue(optionValues.get((optionValues.size() / 2)));

		newTaskData.addAttribute(BugzillaReportElement.PRIORITY.getKeyString(), a);
		// attributes.put(a.getName(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.BUG_SEVERITY);
		optionValues = repositoryConfiguration.getSeverities();
		for (String option : optionValues) {
			a.addOption(option, option);
		}
		a.setValue(optionValues.get((optionValues.size() / 2)));

		newTaskData.addAttribute(BugzillaReportElement.BUG_SEVERITY.getKeyString(), a);
		// attributes.put(a.getName(), a);

		// a = new
		// RepositoryTaskAttribute(BugzillaReportElement.TARGET_MILESTONE);
		// optionValues =
		// BugzillaCorePlugin.getDefault().getgetProductConfiguration(serverUrl).getTargetMilestones(
		// newReport.getProduct());
		// for (String option : optionValues) {
		// a.addOptionValue(option, option);
		// }
		// if(optionValues.size() > 0) {
		// // new bug posts will fail if target_milestone element is
		// included
		// // and there are no milestones on the server
		// newReport.addAttribute(BugzillaReportElement.TARGET_MILESTONE, a);
		// }

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.ASSIGNED_TO);
		a.setValue("");
		a.setReadOnly(false);

		newTaskData.addAttribute(BugzillaReportElement.ASSIGNED_TO.getKeyString(), a);
		// attributes.put(a.getName(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.BUG_FILE_LOC);
		a.setValue("http://");
		a.setHidden(false);

		newTaskData.addAttribute(BugzillaReportElement.BUG_FILE_LOC.getKeyString(), a);
		// attributes.put(a.getName(), a);

		// newReport.attributes = attributes;
	}

}
