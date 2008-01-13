/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.AbstractAttachmentHandler;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.ITaskCollector;
import org.eclipse.mylyn.tasks.core.ITaskFactory;
import org.eclipse.mylyn.tasks.core.QueryHitCollector;
import org.eclipse.mylyn.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskComment;
import org.eclipse.mylyn.tasks.core.TaskList;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.AbstractTask.PriorityLevel;

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

	private Set<BugzillaLanguageSettings> languages = new LinkedHashSet<BugzillaLanguageSettings>();

	@Override
	public void init(TaskList taskList) {
		super.init(taskList);
		this.taskDataHandler = new BugzillaTaskDataHandler(this);
		this.attachmentHandler = new BugzillaAttachmentHandler(this);
		BugzillaCorePlugin.setConnector(this);
		BugzillaLanguageSettings enSetting = new BugzillaLanguageSettings(IBugzillaConstants.DEFAULT_LANG);
		enSetting.addLanguageAttribute("error_login", "Login");
		enSetting.addLanguageAttribute("error_login", "log in");
		enSetting.addLanguageAttribute("error_login", "check e-mail");
		enSetting.addLanguageAttribute("error_login", "Invalid Username Or Password");
		enSetting.addLanguageAttribute("error_collision", "Mid-air collision!");
		enSetting.addLanguageAttribute("error_comment_required", "Comment Required");
		enSetting.addLanguageAttribute("error_logged_out", "logged out");
		enSetting.addLanguageAttribute("bad_login", "Login");
		enSetting.addLanguageAttribute("bad_login", "log in");
		enSetting.addLanguageAttribute("bad_login", "check e-mail");
		enSetting.addLanguageAttribute("bad_login", "Invalid Username Or Password");
		enSetting.addLanguageAttribute("bad_login", "error");
		enSetting.addLanguageAttribute("processed", "processed");
		enSetting.addLanguageAttribute("changes_submitted", "Changes submitted");
		languages.add(enSetting);
	}

	@Override
	public String getLabel() {
		return CLIENT_LABEL;
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
	public String getConnectorKind() {
		return BugzillaCorePlugin.REPOSITORY_KIND;
	}

	@Override
	public AbstractTask createTask(String repositoryUrl, String id, String summary) {
		BugzillaTask task = new BugzillaTask(repositoryUrl, id, summary);
		task.setCreationDate(new Date());
		return task;
	}

	@Override
	public void updateTaskFromTaskData(TaskRepository repository, AbstractTask repositoryTask,
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

			// Creation Date
			String createdString = taskData.getCreated();
			if (createdString != null && createdString.length() > 0) {
				Date dateCreated = taskData.getAttributeFactory().getDateForAttributeType(
						RepositoryTaskAttribute.DATE_CREATION, taskData.getCreated());
				if (dateCreated != null) {
					bugzillaTask.setCreationDate(dateCreated);
				}
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
			String url = getTaskUrl(repository.getUrl(), taskData.getId());
			if (url != null) {
				bugzillaTask.setUrl(url);
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
	public boolean updateTaskFromQueryHit(TaskRepository repository, AbstractTask existingTask, AbstractTask newTask) {
//		// these properties are not provided by Bugzilla queries
//		newTask.setCompleted(existingTask.isCompleted());
//		//	newTask.setCompletionDate(existingTask.getCompletionDate());
//
//		// Owner attribute not previously 
//		if (hasTaskPropertyChanged(existingTask.getOwner(), newTask.getOwner())) {
//			existingTask.setOwner(newTask.getOwner());
//		}
//
//		boolean changed = super.updateTaskFromQueryHit(repository, existingTask, newTask);
//
//		if (existingTask instanceof BugzillaTask && newTask instanceof BugzillaTask) {
//			BugzillaTask existingBugzillaTask = (BugzillaTask) existingTask;
//			BugzillaTask newBugzillaTask = (BugzillaTask) newTask;
//
//			if (hasTaskPropertyChanged(existingBugzillaTask.getSeverity(), newBugzillaTask.getSeverity())) {
//				existingBugzillaTask.setSeverity(newBugzillaTask.getSeverity());
//				changed = true;
//			}
//			if (hasTaskPropertyChanged(existingBugzillaTask.getProduct(), newBugzillaTask.getProduct())) {
//				existingBugzillaTask.setProduct(newBugzillaTask.getProduct());
//				changed = true;
//			}
//		}
		return false;
	}

	@Override
	public boolean markStaleTasks(TaskRepository repository, Set<AbstractTask> tasks, IProgressMonitor monitor)
			throws CoreException {
		try {
			
			monitor.beginTask("Checking for changed tasks", IProgressMonitor.UNKNOWN);

			if (repository.getSynchronizationTimeStamp() == null) {
				for (AbstractTask task : tasks) {
					task.setStale(true);
				}
				return true;
			}

			String dateString = repository.getSynchronizationTimeStamp();
			if (dateString == null) {
				dateString = "";
			}

			String urlQueryBase = repository.getUrl() + CHANGED_BUGS_CGI_QUERY
					+ URLEncoder.encode(dateString, repository.getCharacterEncoding()) + CHANGED_BUGS_CGI_ENDDATE;

			String urlQueryString = urlQueryBase + BUG_ID;

			// Need to replace this with query that would return list of tasks since last sync
			// the trouble is that bugzilla only have 1 hour granularity for "changed since" query
			// so, we can't say that no tasks has changed in repository

			Set<AbstractTask> changedTasks = new HashSet<AbstractTask>();
			Iterator<AbstractTask> itr = tasks.iterator();
			while (itr.hasNext()) {
				AbstractTask task = itr.next();
				String newurlQueryString = URLEncoder.encode(task.getTaskId() + ",", repository.getCharacterEncoding());
				urlQueryString += newurlQueryString;
			}
			
			queryForChanged(repository, changedTasks, urlQueryString);
			
			for (AbstractTask task : tasks) {
				if (changedTasks.contains(task)) {
					task.setStale(true);
				}
			}


			// FIXME check if new tasks were added
			//return changedTasks.isEmpty();
			return true;
		} catch (UnsupportedEncodingException e) {
			// XXX throw CoreException instead?
			StatusHandler.fail(e, "Repository configured with unsupported encoding: "
					+ repository.getCharacterEncoding() + "\n\n Unable to determine changed tasks.", true);
			return false;
		} finally {
			monitor.done();
		}
	}
	
	private void queryForChanged(final TaskRepository repository, Set<AbstractTask> changedTasks, String urlQueryString)
			throws UnsupportedEncodingException, CoreException {
		QueryHitCollector collector = new QueryHitCollector(new ITaskFactory() {

			public AbstractTask createTask(RepositoryTaskData taskData, IProgressMonitor monitor) {
				// do not construct actual task objects here as query shouldn't result in new tasks
				return taskList.getTask(taskData.getRepositoryUrl(), taskData.getId());
			}
		});

		BugzillaRepositoryQuery query = new BugzillaRepositoryQuery(repository.getUrl(), urlQueryString, "");

		performQuery(query, repository, new NullProgressMonitor(), collector);
		changedTasks.addAll(collector.getTasks());
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
			IProgressMonitor monitor, ITaskCollector resultCollector) {
		try {
			monitor.beginTask("Running query", IProgressMonitor.UNKNOWN);
			BugzillaClient client = getClientManager().getClient(repository);
			boolean hitsReceived = client.getSearchHits(query, resultCollector);
			if (!hitsReceived) {
				// XXX: HACK in case of ip change bugzilla can return 0 hits
				// due to invalid authorization token, forcing relogin fixes
				client.logout();
				client.getSearchHits(query, resultCollector);
			}

			return Status.OK_STATUS;
		} catch (UnrecognizedReponseException e) {
			return new Status(IStatus.ERROR, BugzillaCorePlugin.PLUGIN_ID, Status.INFO,
					"Unrecognized response from server", e);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, BugzillaCorePlugin.PLUGIN_ID, Status.ERROR,
					"Check repository configuration: " + e.getMessage(), e);
		} catch (CoreException e) {
			return e.getStatus();
		} finally {
			monitor.done();
		}
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
		int anchorIndex = url.lastIndexOf("#");
		String bugUrl = url;
		if (anchorIndex != -1) {
			bugUrl = url.substring(0, anchorIndex);
		}

		int index = bugUrl.indexOf(IBugzillaConstants.URL_GET_SHOW_BUG);
		return index == -1 ? null : bugUrl.substring(index + IBugzillaConstants.URL_GET_SHOW_BUG.length());
	}

	@Override
	public String getTaskUrl(String repositoryUrl, String taskId) {
		try {
			return BugzillaClient.getBugUrlWithoutLogin(repositoryUrl, taskId);
		} catch (Exception ex) {
			StatusHandler.fail(ex, "Error constructing task url for " + repositoryUrl + "  id:" + taskId, false);
		}
		return null;
	}

	@Override
	public void updateTaskFromRepository(TaskRepository repository, AbstractTask repositoryTask,
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

	public boolean isRepositoryConfigurationStale(TaskRepository repository) throws CoreException {
		if (super.isRepositoryConfigurationStale(repository)) {
			boolean result = true;
			try {
				BugzillaClient client = getClientManager().getClient(repository);
				if (client != null) {
					String timestamp = client.getConfigurationTimestamp();
					if (timestamp != null) {
						String oldTimestamp = repository.getProperty(IBugzillaConstants.PROPERTY_CONFIGTIMESTAMP);
						if (oldTimestamp != null) {
							result = !timestamp.equals(oldTimestamp);
						}
						repository.setProperty(IBugzillaConstants.PROPERTY_CONFIGTIMESTAMP, timestamp);
					}
				}
			} catch (MalformedURLException e) {
				StatusHandler.fail(e, "Error retrieving configuration timestamp for " + repository.getUrl(), false);
			}
			return result;
		}
		return false;
	}

	public void updateAttributeOptions(TaskRepository taskRepository, RepositoryTaskData existingReport)
			throws CoreException {
		String product = existingReport.getAttributeValue(BugzillaReportElement.PRODUCT.getKeyString());
		for (RepositoryTaskAttribute attribute : existingReport.getAttributes()) {
			BugzillaReportElement element = BugzillaReportElement.valueOf(attribute.getId().trim().toUpperCase(
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
		if (optionValues.size() > 0) {
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
		if (optionValues.size() == 1) {
			a.setValue(optionValues.get(0));
		}

		newTaskData.addAttribute(BugzillaReportElement.COMPONENT.getKeyString(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.REP_PLATFORM);
		optionValues = repositoryConfiguration.getPlatforms();
		for (String option : optionValues) {
			a.addOption(option, option);
		}
		if (optionValues.size() > 0) {
			// bug 159397 choose first platform: All
			a.setValue(optionValues.get(0));
		}

		newTaskData.addAttribute(BugzillaReportElement.REP_PLATFORM.getKeyString(), a);
		// attributes.put(a.getName(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.OP_SYS);
		optionValues = repositoryConfiguration.getOSs();
		for (String option : optionValues) {
			a.addOption(option, option);
		}
		if (optionValues.size() > 0) {
			// bug 159397 change to choose first op_sys All
			a.setValue(optionValues.get(0));
		}

		newTaskData.addAttribute(BugzillaReportElement.OP_SYS.getKeyString(), a);
		// attributes.put(a.getName(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.PRIORITY);
		optionValues = repositoryConfiguration.getPriorities();
		for (String option : optionValues) {
			a.addOption(option, option);
		}
		if (optionValues.size() > 0) {
			a.setValue(optionValues.get((optionValues.size() / 2))); // choose middle priority
		}

		newTaskData.addAttribute(BugzillaReportElement.PRIORITY.getKeyString(), a);
		// attributes.put(a.getName(), a);

		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.BUG_SEVERITY);
		optionValues = repositoryConfiguration.getSeverities();
		for (String option : optionValues) {
			a.addOption(option, option);
		}
		if (optionValues.size() > 0) {
			a.setValue(optionValues.get((optionValues.size() / 2))); // choose middle severity
		}

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
		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.DEPENDSON);
		a.setValue("");
		a.setReadOnly(false);
		newTaskData.addAttribute(BugzillaReportElement.DEPENDSON.getKeyString(), a);
		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.BLOCKED);
		a.setValue("");
		a.setReadOnly(false);
		newTaskData.addAttribute(BugzillaReportElement.BLOCKED.getKeyString(), a);
		a = BugzillaClient.makeNewAttribute(BugzillaReportElement.NEWCC);
		a.setValue("");
		a.setReadOnly(false);
		newTaskData.addAttribute(BugzillaReportElement.NEWCC.getKeyString(), a);
	}

	public static int getBugId(String taskId) throws CoreException {
		try {
			return Integer.parseInt(taskId);
		} catch (NumberFormatException e) {
			throw new CoreException(new Status(IStatus.ERROR, BugzillaCorePlugin.PLUGIN_ID, 0, "Invalid bug id: "
					+ taskId, e));
		}
	}

	public void addLanguageSetting(BugzillaLanguageSettings language) {
		if (!languages.contains(language)) {
			this.languages.add(language);
		}
	}

	public Set<BugzillaLanguageSettings> getLanguageSettings() {
		return languages;
	}

	/** returns default language if language not found */
	public BugzillaLanguageSettings getLanguageSetting(String label) {
		for (BugzillaLanguageSettings language : getLanguageSettings()) {
			if (language.getLanguageName().equals(label)) {
				return language;
			}
		}
		return BugzillaCorePlugin.getDefault().getLanguageSetting(IBugzillaConstants.DEFAULT_LANG);
	}

}
