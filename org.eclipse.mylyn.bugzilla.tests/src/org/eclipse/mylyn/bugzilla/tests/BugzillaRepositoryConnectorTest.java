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

package org.eclipse.mylyn.bugzilla.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaClient;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaReportElement;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryQuery;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaTask;
import org.eclipse.mylyn.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylyn.internal.bugzilla.core.RepositoryConfiguration;
import org.eclipse.mylyn.internal.tasks.core.RepositoryTaskHandleUtil;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.LocalAttachment;
import org.eclipse.mylyn.tasks.core.RepositoryAttachment;
import org.eclipse.mylyn.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.AbstractTask.RepositoryTaskSyncState;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.ui.search.RepositorySearchResult;
import org.eclipse.mylyn.tasks.ui.search.SearchHitCollector;

/**
 * @author Mik Kersten
 * @author Rob Elves
 * @author Nathan Hapke
 */
public class BugzillaRepositoryConnectorTest extends AbstractBugzillaTest {

	public void testFocedQuerySynchronization() throws CoreException {
		init222();
		TasksUiPlugin.getSynchronizationManager().setForceSyncExec(true);
		TasksUiPlugin.getDefault().getTaskDataManager().clear();
		assertEquals(0, taskList.getAllTasks().size());
		BugzillaRepositoryQuery bugQuery = new BugzillaRepositoryQuery(
				IBugzillaConstants.TEST_BUGZILLA_222_URL,
				"http://mylyn.eclipse.org/bugs222/buglist.cgi?short_desc_type=allwordssubstr&short_desc=&product=Read+Only+Test+Cases&long_desc_type=allwordssubstr&long_desc=&bug_status=NEW&order=Importance",
				"testFocedQuerySynchronization");

		taskList.addQuery(bugQuery);

		TasksUiPlugin.getSynchronizationManager().synchronize(connector, bugQuery, null, false);

		assertEquals(1, bugQuery.getHits().size());
		AbstractTask hit = (AbstractTask) bugQuery.getHits().toArray()[0];
		assertTrue(TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(hit.getHandleIdentifier()) != null);
		TasksUiPlugin.getDefault().getTaskDataManager().remove(hit.getHandleIdentifier());

		TasksUiPlugin.getSynchronizationManager().synchronize(connector, bugQuery, null, true);
		assertEquals(1, bugQuery.getHits().size());
		hit = (AbstractTask) bugQuery.getHits().toArray()[0];
		assertTrue(TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(hit.getHandleIdentifier()) != null);

	}

	public void testCreateTaskFromExistingId() throws Exception {
		init222();
		try {
			connector.createTaskFromExistingId(repository, "-123", new NullProgressMonitor());
			fail();
		} catch (CoreException ce) {

		}

		BugzillaTask task = generateLocalTaskAndDownload("1");
		assertNotNull(task);
		assertNotNull(TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(task.getHandleIdentifier()));
		assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, task.getSyncState());

		BugzillaTask retrievedTask = (BugzillaTask) taskList.getTask(task.getHandleIdentifier());
		assertNotNull(retrievedTask);
		assertEquals(task.getHandleIdentifier(), retrievedTask.getHandleIdentifier());
	}

	public void testAnonymousRepositoryAccess() throws Exception {
		init218();
		assertNotNull(repository);
		repository.setAuthenticationCredentials("", "");
		// test anonymous task retrieval
		BugzillaTask task = this.generateLocalTaskAndDownload("2");
		assertNotNull(task);

		// // test anonymous query (note that this demonstrates query via
		// eclipse search (ui)
		// SearchHitCollector collector = new SearchHitCollector(taskList);
		// collector.setProgressMonitor(new NullProgressMonitor());
		// BugzillaSearchOperation operation = new BugzillaSearchOperation(
		// repository,
		// "http://mylyn.eclipse.org/bugs218/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=search-match-test&product=TestProduct&long_desc_type=substring&long_desc=&bug_file_loc_type=allwordssubstr&bug_file_loc=&deadlinefrom=&deadlineto=&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&emailassigned_to1=1&emailtype1=substring&email1=&emailassigned_to2=1&emailreporter2=1&emailcc2=1&emailtype2=substring&email2=&bugidtype=include&bug_id=&votes=&chfieldfrom=&chfieldto=Now&chfieldvalue=&cmdtype=doit&order=Reuse+same+sort+as+last+time&field0-0-0=noop&type0-0-0=noop&value0-0-0=",
		// null, collector, "-1");
		//		
		String queryUrl = "http://mylyn.eclipse.org/bugs218/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=search-match-test&product=TestProduct&long_desc_type=substring&long_desc=&bug_file_loc_type=allwordssubstr&bug_file_loc=&deadlinefrom=&deadlineto=&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&emailassigned_to1=1&emailtype1=substring&email1=&emailassigned_to2=1&emailreporter2=1&emailcc2=1&emailtype2=substring&email2=&bugidtype=include&bug_id=&votes=&chfieldfrom=&chfieldto=Now&chfieldvalue=&cmdtype=doit&order=Reuse+same+sort+as+last+time&field0-0-0=noop&type0-0-0=noop&value0-0-0=";
		BugzillaRepositoryQuery bugzillaQuery = new BugzillaRepositoryQuery(repository.getUrl(), queryUrl, "search");

		SearchHitCollector collector = new SearchHitCollector(taskList, repository, bugzillaQuery, taskFactory);
		RepositorySearchResult result = (RepositorySearchResult) collector.getSearchResult();

		// operation.run(new NullProgressMonitor());
		// BugzillaSearchQuery searchQuery = new BugzillaSearchQuery(collector);
		collector.run(new NullProgressMonitor());
		assertEquals(2, result.getElements().length);

		for (AbstractTask hit : collector.getTaskHits()) {
			assertTrue(hit.getSummary().contains("search-match-test"));
		}

		// test anonymous update of configuration
		RepositoryConfiguration config = BugzillaCorePlugin.getRepositoryConfiguration(repository, false);
		assertNotNull(config);
		assertTrue(config.getComponents().size() > 0);
	}

	public void testUpdate() throws Exception {
		init222();
		String taskNumber = "3";
		TasksUiPlugin.getDefault().getTaskDataManager().clear();
		assertEquals(0, TasksUiPlugin.getTaskListManager().getTaskList().getAllTasks().size());
		BugzillaTask task = this.generateLocalTaskAndDownload(taskNumber);
		assertEquals("search-match-test 2", task.getSummary());
		assertEquals("TestProduct", task.getProduct());
		assertEquals("P1", task.getPriority());
		assertEquals("blocker", task.getSeverity());
		assertEquals("nhapke@cs.ubc.ca", task.getOwner());
		// assertEquals("2007-04-18 14:21:40",
		// task.getCompletionDate().toString());
		assertFalse(task.isCompleted());
		assertEquals("http://mylyn.eclipse.org/bugs222/show_bug.cgi?id=3", task.getTaskUrl());
	}

	public void testUpdateWithSubTasks() throws Exception {
		init222();
		String taskNumber = "23";
		TasksUiPlugin.getDefault().getTaskDataManager().clear();
		assertEquals(0, TasksUiPlugin.getTaskListManager().getTaskList().getAllTasks().size());
		BugzillaTask task = this.generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task.getChildren());
		assertEquals(2, task.getChildren().size());
	}

	public void testContextAttachFailure() throws Exception {
		init218();
		BugzillaTask task = this.generateLocalTaskAndDownload("3");
		assertNotNull(task);
		assertNotNull(TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(task.getHandleIdentifier()));
		TasksUiPlugin.getTaskListManager().activateTask(task);
		File sourceContextFile = ContextCorePlugin.getContextManager().getFileForContext(task.getHandleIdentifier());
		assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, task.getSyncState());
		sourceContextFile.createNewFile();
		sourceContextFile.deleteOnExit();
		repository.setAuthenticationCredentials("wrong", "wrong");
		try {
			connector.attachContext(repository, task, "", new NullProgressMonitor());
		} catch (CoreException e) {
			assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, task.getSyncState());
			return;
		}
		fail("Should have failed due to invalid userid and password.");
	}

	public void testSynchronize() throws CoreException {
		init222();

		TasksUiPlugin.getDefault().getTaskDataManager().clear();

		// Get the task
		BugzillaTask task = generateLocalTaskAndDownload("3");

		RepositoryTaskData taskData = TasksUiPlugin.getDefault().getTaskDataManager().getEditableCopy(
				task.getHandleIdentifier());
		assertNotNull(taskData);

		TasksUiPlugin.getTaskListManager().getTaskList().moveToContainer(TasksUiPlugin.getTaskListManager().getTaskList().getDefaultCategory(), task);
		int numComments = taskData.getComments().size();

		// Modify it
		String newCommentText = "BugzillaRepositoryClientTest.testSynchronize(): " + (new Date()).toString();
		taskData.setNewComment(newCommentText);
		Set<RepositoryTaskAttribute> changed = new HashSet<RepositoryTaskAttribute>();
		changed.add(taskData.getAttribute(RepositoryTaskAttribute.COMMENT_NEW));
		TasksUiPlugin.getDefault().getTaskDataManager().saveEdits(task.getHandleIdentifier(), changed);

		// Submit changes
		submit(task, taskData);

		TasksUiPlugin.getSynchronizationManager().synchronize(connector, task, true, null);
		// After submit task should be in SYNCHRONIZED state
		assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, task.getSyncState());
		RepositoryTaskData taskData2 = TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(
				task.getHandleIdentifier());
		assertFalse(taskData2.getLastModified().equals(taskData.getLastModified()));
		// Still not read
		assertFalse(taskData2.getLastModified().equals(task.getLastSyncDateStamp()));
		TasksUiPlugin.getSynchronizationManager().setTaskRead(task, true);
		assertEquals(taskData2.getLastModified(), task.getLastSyncDateStamp());
		assertTrue(taskData2.getComments().size() > numComments);

		// Has no outgoing changes or conflicts yet needs synch
		// because task doesn't have bug report (new query hit)
		// Result: retrieved with no incoming status
		// task.setSyncState(RepositoryTaskSyncState.SYNCHRONIZED);
		TasksUiPlugin.getDefault().getTaskDataManager().remove(task.getHandleIdentifier());
		TasksUiPlugin.getSynchronizationManager().synchronize(connector, task, false, null);
		assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, task.getSyncState());
		RepositoryTaskData bugReport2 = null;
		bugReport2 = TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(task.getHandleIdentifier());
		assertNotNull(bugReport2);
		assertEquals(task.getTaskId(), bugReport2.getId());

		assertEquals(newCommentText, bugReport2.getComments().get(numComments).getText());
		// TODO: Test that comment was appended
		// ArrayList<Comment> comments = task.getTaskData().getComments();
		// assertNotNull(comments);
		// assertTrue(comments.size() > 0);
		// Comment lastComment = comments.get(comments.size() - 1);
		// assertEquals(newCommentText, lastComment.getText());

	}

	public void testUniqueQueryHitObjects() {
		init222();
		BugzillaRepositoryQuery query1 = new BugzillaRepositoryQuery(IBugzillaConstants.TEST_BUGZILLA_222_URL,
				"queryurl", "description1");
		BugzillaTask query1Hit = new BugzillaTask(IBugzillaConstants.TEST_BUGZILLA_222_URL, "1", "description1");
		taskList.addQuery(query1);
		taskList.addTask(query1Hit, query1);
		
		BugzillaRepositoryQuery query2 = new BugzillaRepositoryQuery(IBugzillaConstants.TEST_BUGZILLA_222_URL,
				"queryurl2", "description2");
		BugzillaTask query2Hit = new BugzillaTask(IBugzillaConstants.TEST_BUGZILLA_222_URL, "1", "description2");
		taskList.addQuery(query2);
		taskList.addTask(query2Hit, query1);
		
		assertEquals(2, taskList.getQueries().size());
		assertEquals(1, taskList.getAllTasks().size());
		for (AbstractTask hit : query1.getHits()) {
			for (AbstractTask hit2 : query2.getHits()) {
				assertTrue(hit.getClass().equals(hit2.getClass()));
			}
		}

		taskList.deleteQuery(query1);
		taskList.deleteQuery(query2);
		assertEquals(1, taskList.getAllTasks().size());
	}

	public void testAttachToExistingReport() throws Exception {
		init222();
		String taskNumber = "33";
		BugzillaTask task = generateLocalTaskAndDownload(taskNumber);
		RepositoryTaskData taskData = TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(
				task.getHandleIdentifier());

		assertNotNull(task);
		assertNotNull(taskData);
		TasksUiPlugin.getSynchronizationManager().setTaskRead(task, true);
		assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, task.getSyncState());
		assertEquals(taskNumber, taskData.getId());
		int numAttached = taskData.getAttachments().size();
		String fileName = "test-attach-" + System.currentTimeMillis() + ".txt";

		assertNotNull(repository.getUserName());
		assertNotNull(repository.getPassword());

		/* Initialize a local attachment */
		LocalAttachment attachment = new LocalAttachment();
		attachment.setDescription("Test attachment " + new Date());
		attachment.setContentType("text/plain");
		attachment.setPatch(false);
		attachment.setReport(taskData);
		attachment.setComment("Automated JUnit attachment test"); // optional

		/* Test attempt to upload a non-existent file */
		attachment.setFilePath("/this/is/not/a/real-file");
		attachment.setFile(new File(attachment.getFilePath()));
		attachment.setFilename("real-file");
		// IAttachmentHandler attachmentHandler =
		// connector.getAttachmentHandler();
		BugzillaClient client = connector.getClientManager().getClient(repository);
		try {
			client.postAttachment(attachment.getReport().getId(), attachment.getComment(), attachment);
			fail();
		} catch (Exception e) {
		}
		// attachmentHandler.uploadAttachment(repository, task, comment,
		// summary, file, contentType, isPatch, proxySettings)
		// assertFalse(attachmentHandler.uploadAttachment(attachment,
		// repository.getUserName(), repository.getPassword(),
		// Proxy.NO_PROXY));
		assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, task.getSyncState());
		task = (BugzillaTask) connector.createTaskFromExistingId(repository, taskNumber, new NullProgressMonitor());
		TasksUiPlugin.getSynchronizationManager().synchronize(connector, task, true, null);

		assertEquals(numAttached, taskData.getAttachments().size());

		/* Test attempt to upload an empty file */
		File attachFile = new File(fileName);
		attachment.setFilePath(attachFile.getAbsolutePath());
		BufferedWriter write = new BufferedWriter(new FileWriter(attachFile));
		attachFile = new File(attachment.getFilePath());
		attachment.setFile(attachFile);
		attachment.setFilename(attachFile.getName());
		// assertFalse(attachmentHandler.uploadAttachment(attachment,
		// repository.getUserName(), repository.getPassword(),
		// Proxy.NO_PROXY));
		try {
			client.postAttachment(attachment.getReport().getId(), attachment.getComment(), attachment);
			fail();
		} catch (Exception e) {
		}
		task = (BugzillaTask) connector.createTaskFromExistingId(repository, taskNumber, new NullProgressMonitor());
		TasksUiPlugin.getSynchronizationManager().synchronize(connector, task, true, null);
		taskData = TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(task.getHandleIdentifier());
		assertEquals(numAttached, taskData.getAttachments().size());

		/* Test uploading a proper file */
		write.write("test file");
		write.close();
		attachment.setFilePath(attachFile.getAbsolutePath());
		// assertTrue(attachmentHandler.uploadAttachment(attachment,
		// repository.getUserName(), repository.getPassword(),
		// Proxy.NO_PROXY));
		File fileToAttach = new File(attachment.getFilePath());
		assertTrue(fileToAttach.exists());
		attachment.setFile(fileToAttach);
		attachment.setFilename(fileToAttach.getName());
		client.postAttachment(attachment.getReport().getId(), attachment.getComment(), attachment);

		task = (BugzillaTask) connector.createTaskFromExistingId(repository, taskNumber, new NullProgressMonitor());
		TasksUiPlugin.getSynchronizationManager().synchronize(connector, task, true, null);
		taskData = TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(task.getHandleIdentifier());
		assertEquals(numAttached + 1, taskData.getAttachments().size());

		// use assertion to track clean-up
		assertTrue(attachFile.delete());
	}

	public void testSynchChangedReports() throws Exception {

		init222();
		String taskID = "4";
		BugzillaTask task4 = generateLocalTaskAndDownload(taskID);
		RepositoryTaskData taskData4 = TasksUiPlugin.getDefault().getTaskDataManager().getEditableCopy(
				task4.getHandleIdentifier());
		assertNotNull(task4);
		assertNotNull(taskData4);
		assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, task4.getSyncState());
		assertEquals(taskID, taskData4.getId());

		BugzillaTask task5 = generateLocalTaskAndDownload("5");
		RepositoryTaskData taskData5 = TasksUiPlugin.getDefault().getTaskDataManager().getEditableCopy(
				task5.getHandleIdentifier());
		assertNotNull(task5);
		assertNotNull(taskData5);
		assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, task5.getSyncState());
		assertEquals("5", taskData5.getId());

		Set<AbstractTask> tasks = new HashSet<AbstractTask>();
		tasks.add(task4);
		tasks.add(task5);

		// Precondition for test passing is that task5's modification data is
		// AFTER
		// task4's

		TasksUiPlugin.getRepositoryManager().setSyncTime(repository, task5.getLastSyncDateStamp(),
				TasksUiPlugin.getDefault().getRepositoriesFilePath());

		Set<AbstractTask> changedTasks = connector.getChangedSinceLastSync(repository, tasks, new NullProgressMonitor());
		// Always last known changed returned
		assertEquals(1, changedTasks.size());

		String priority4 = null;
		if (task4.getPriority().equals("P1")) {
			priority4 = "P2";
			taskData4.setAttributeValue(BugzillaReportElement.PRIORITY.getKeyString(), priority4);
		} else {
			priority4 = "P1";
			taskData4.setAttributeValue(BugzillaReportElement.PRIORITY.getKeyString(), priority4);
		}

		String priority5 = null;
		if (task5.getPriority().equals("P1")) {
			priority5 = "P2";
			taskData5.setAttributeValue(BugzillaReportElement.PRIORITY.getKeyString(), priority5);
		} else {
			priority5 = "P1";
			taskData5.setAttributeValue(BugzillaReportElement.PRIORITY.getKeyString(), priority5);
		}

		assertNotNull(repository.getUserName());
		assertNotNull(repository.getPassword());

		submit(task4, taskData4);
		submit(task5, taskData5);

		changedTasks = connector.getChangedSinceLastSync(repository, tasks, new NullProgressMonitor());

		assertEquals("Changed reports expected ", 2, changedTasks.size());
		assertTrue(tasks.containsAll(changedTasks));

		TasksUiPlugin.getSynchronizationManager().synchronize(connector, changedTasks, true, null);

		for (AbstractTask task : changedTasks) {
			if (task.getTaskId() == "4") {
				assertEquals(priority4, task4.getPriority());
			}
			if (task.getTaskId() == "5") {
				assertEquals(priority5, task5.getPriority());
			}
		}
	}

	public void testIncomingWhenOfflineDeleted() throws Exception {

		init222();
		BugzillaTask task7 = generateLocalTaskAndDownload("7");
		RepositoryTaskData recentTaskData = TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(
				task7.getHandleIdentifier());
		assertNotNull(recentTaskData);
		TasksUiPlugin.getSynchronizationManager().setTaskRead(task7, true);
		assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, task7.getSyncState());
		assertEquals("7", recentTaskData.getId());

		Set<AbstractTask> tasks = new HashSet<AbstractTask>();
		tasks.add(task7);

		TasksUiPlugin.getRepositoryManager().setSyncTime(repository, task7.getLastSyncDateStamp(),
				TasksUiPlugin.getDefault().getRepositoriesFilePath());

		assertNotNull(TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(
				RepositoryTaskHandleUtil.getHandle(IBugzillaConstants.TEST_BUGZILLA_222_URL, "7")));
		ArrayList<String> taskDataList = new ArrayList<String>();
		taskDataList.add(task7.getHandleIdentifier());
		TasksUiPlugin.getDefault().getTaskDataManager().remove(taskDataList);

		assertNull(TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(
				RepositoryTaskHandleUtil.getHandle(IBugzillaConstants.TEST_BUGZILLA_222_URL, "7")));

		assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, task7.getSyncState());
		assertNotNull(task7.getLastSyncDateStamp());
		// Task no longer stored offline
		// make an external change
		assertNotNull(repository.getUserName());
		assertNotNull(repository.getPassword());

		String priority = null;
		if (task7.getPriority().equals("P1")) {
			priority = "P2";
			recentTaskData.setAttributeValue(BugzillaReportElement.PRIORITY.getKeyString(), priority);
		} else {
			priority = "P1";
			recentTaskData.setAttributeValue(BugzillaReportElement.PRIORITY.getKeyString(), priority);
		}
		TasksUiPlugin.getDefault().getTaskDataManager().clear(); // NO TASK
		// DATA
		connector.getTaskDataHandler().postTaskData(repository, recentTaskData, new NullProgressMonitor());
		TasksUiPlugin.getSynchronizationManager().synchronizeChanged(connector, repository);
		assertEquals(RepositoryTaskSyncState.INCOMING, task7.getSyncState());
	}

	public void testTimeTracker222() throws Exception {
		init222();
		timeTracker(15, true);
	}

	// We'll skip these two for now and just test 222 and 218 since
	// they are the most common. If problems arise we can re-enable.
	// public void testTimeTracker2201() throws Exception {
	// init2201();
	// timeTracker(22, true);
	// }
	//
	// public void testTimeTracker220() throws Exception {
	// init220();
	// timeTracker(8, true);
	// }

	public void testTimeTracker218() throws Exception {
		init218();
		timeTracker(20, false);
	}

	/**
	 * @param enableDeadline
	 *            bugzilla 218 doesn't support deadlines
	 */
	protected void timeTracker(int taskid, boolean enableDeadline) throws Exception {
		BugzillaTask bugtask = generateLocalTaskAndDownload("" + taskid);
		RepositoryTaskData bugtaskdata = TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(
				bugtask.getHandleIdentifier());
		assertNotNull(bugtaskdata);
		assertEquals(taskid + "", bugtaskdata.getId());
		assertEquals(RepositoryTaskSyncState.SYNCHRONIZED, bugtask.getSyncState());

		Set<AbstractTask> tasks = new HashSet<AbstractTask>();
		tasks.add(bugtask);

		// synchAndAssertState(tasks, RepositoryTaskSyncState.SYNCHRONIZED);

		TasksUiPlugin.getRepositoryManager().setSyncTime(repository, bugtask.getLastSyncDateStamp(),
				TasksUiPlugin.getDefault().getRepositoriesFilePath());
		// connector.synchronizeChanged(repository);

		// Set<AbstractTask> changedTasks =
		// connector.getOfflineTaskHandler().getChangedSinceLastSync(repository,
		// tasks);
		// assertEquals(1, changedTasks.size());

		assertNotNull(repository.getUserName());
		assertNotNull(repository.getPassword());

		float estimatedTime, remainingTime, actualTime, addTime;
		String deadline = null;

		estimatedTime = Float.parseFloat(bugtaskdata.getAttributeValue(BugzillaReportElement.ESTIMATED_TIME
				.getKeyString()));
		remainingTime = Float.parseFloat(bugtaskdata.getAttributeValue(BugzillaReportElement.REMAINING_TIME
				.getKeyString()));
		actualTime = Float.parseFloat(bugtaskdata.getAttributeValue(BugzillaReportElement.ACTUAL_TIME.getKeyString()));
		if (enableDeadline)
			deadline = bugtaskdata.getAttributeValue(BugzillaReportElement.DEADLINE.getKeyString());

		estimatedTime += 2;
		remainingTime += 1.5;
		addTime = 0.75f;
		if (enableDeadline)
			deadline = generateNewDay();

		bugtaskdata.setAttributeValue(BugzillaReportElement.ESTIMATED_TIME.getKeyString(), "" + estimatedTime);
		bugtaskdata.setAttributeValue(BugzillaReportElement.REMAINING_TIME.getKeyString(), "" + remainingTime);
		bugtaskdata.setAttributeValue(BugzillaReportElement.WORK_TIME.getKeyString(), "" + addTime);
		if (enableDeadline)
			bugtaskdata.setAttributeValue(BugzillaReportElement.DEADLINE.getKeyString(), deadline);

		for (AbstractTask task : tasks) {
			RepositoryTaskData taskData = TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(
					task.getHandleIdentifier());
			taskData.setAttributeValue(BugzillaReportElement.ADD_COMMENT.getKeyString(), "New Estimate: "
					+ estimatedTime + "\nNew Remaining: " + remainingTime + "\nAdd: " + addTime);
			submit(task, taskData);
		}

		synchAndAssertState(tasks, RepositoryTaskSyncState.SYNCHRONIZED);

		bugtaskdata = TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(bugtask.getHandleIdentifier());

		assertEquals(estimatedTime, Float.parseFloat(bugtaskdata.getAttributeValue(BugzillaReportElement.ESTIMATED_TIME
				.getKeyString())));
		assertEquals(remainingTime, Float.parseFloat(bugtaskdata.getAttributeValue(BugzillaReportElement.REMAINING_TIME
				.getKeyString())));
		assertEquals(actualTime + addTime, Float.parseFloat(bugtaskdata
				.getAttributeValue(BugzillaReportElement.ACTUAL_TIME.getKeyString())));
		if (enableDeadline)
			assertEquals(deadline, bugtaskdata.getAttributeValue(BugzillaReportElement.DEADLINE.getKeyString()));

	}

	private String generateNewDay() {
		int year = 2006;
		int month = (int) (Math.random() * 12 + 1);
		int day = (int) (Math.random() * 28 + 1);
		return "" + year + "-" + ((month <= 9) ? "0" : "") + month + "-" + ((day <= 9) ? "0" : "") + day;
	}

	/**
	 * Ensure obsoletes and patches are marked as such by the parser.
	 */
	public void testAttachmentAttributes() throws Exception {
		init222();
		int bugId = 19;
		String taskNumber = "" + bugId;
		BugzillaTask task = (BugzillaTask) generateLocalTaskAndDownload(taskNumber);

		// TasksUiPlugin.getSynchronizationManager().synchronize(connector,
		// task, true, null);

		assertNotNull(task);

		boolean isPatch[] = { false, true, false, false, false, false, false, true, false, false };
		boolean isObsolete[] = { false, true, false, true, false, false, false, false, false, false };

		RepositoryTaskData taskData = TasksUiPlugin.getDefault().getTaskDataManager().getNewTaskData(
				task.getHandleIdentifier());

		Iterator<RepositoryAttachment> iter = taskData.getAttachments().iterator();

		int index = 0;
		while (iter.hasNext()) {
			assertTrue(validateAttachmentAttributes(iter.next(), isPatch[index], isObsolete[index]));
			index++;
		}
	}

	private boolean validateAttachmentAttributes(RepositoryAttachment att, boolean isPatch, boolean isObsolete) {
		return (att.isPatch() == isPatch) && (att.isObsolete() == isObsolete);
	}

// public void testSimpleLoad() throws Exception {
// repository = new TaskRepository(DEFAULT_KIND,
// IBugzillaConstants.ECLIPSE_BUGZILLA_URL);
// //Credentials credentials = MylarTestUtils.readCredentials();
// //repository.setAuthenticationCredentials(credentials.username,
// credentials.password);
//
// //repository.setTimeZoneId("Canada/Eastern");
// assertNotNull(manager);
// manager.addRepository(repository,
// TasksUiPlugin.getDefault().getRepositoriesFilePath());
//
// taskList = TasksUiPlugin.getTaskListManager().getTaskList();
//
// AbstractRepositoryConnector abstractRepositoryConnector =
// manager.getRepositoryConnector(DEFAULT_KIND);
//
// assertEquals(abstractRepositoryConnector.getRepositoryType(), DEFAULT_KIND);
//
// connector = (BugzillaRepositoryConnector) abstractRepositoryConnector;
//		
// long start = System.currentTimeMillis();
// BugzillaTask task = null;
// for(int x = 1; x < 5; x++) {
// if(task != null)
// taskList.deleteTask(task);
//			
// task = this.generateLocalTaskAndDownload("154100");
// assertNotNull(task);
// }
// System.err.println("Total: "+((System.currentTimeMillis() - start)/1000));
// }

	// class MockBugzillaReportSubmitForm extends BugzillaReportSubmitForm {
	//
	// public MockBugzillaReportSubmitForm(String encoding_utf_8) {
	// super(encoding_utf_8);
	// }
	//
	// @Override
	// public String submitReportToRepository() throws BugzillaException,
	// LoginException,
	// PossibleBugzillaFailureException {
	// return "test-submit";
	// }
	//
	// }
}