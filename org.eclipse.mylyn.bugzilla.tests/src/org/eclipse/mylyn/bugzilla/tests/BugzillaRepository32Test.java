/*******************************************************************************
 * Copyright (c) 2004, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.bugzilla.tests;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.bugzilla.tests.support.BugzillaFixture;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaAttribute;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.sync.SubmitTaskJob;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITask.SynchronizationState;
import org.eclipse.mylyn.tasks.core.data.ITaskDataWorkingCopy;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.sync.SubmitJob;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tests.util.TestUtil.PrivilegeLevel;

public class BugzillaRepository32Test extends AbstractBugzillaTest {

	public void testBugUpdate() throws Exception {
		TaskData taskData = BugzillaFixture.current().createTask(PrivilegeLevel.USER, null, null);
		assertNotNull(taskData);
		assertNotNull(taskData.getRoot().getAttribute("token"));

		//remove the token (i.e. unpatched Bugzilla 3.2.2)
		taskData.getRoot().removeAttribute("token");

		TaskAttribute attrPriority = taskData.getRoot().getAttribute("priority");
		boolean p1 = false;
		if (attrPriority.getValue().equals("P1")) {
			p1 = true;
			attrPriority.setValue("P2");
		} else {
			attrPriority.setValue("P1");
		}

		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		changed.add(attrPriority);
		BugzillaFixture.current().submitTask(taskData, client);

		taskData = BugzillaFixture.current().getTask(taskData.getTaskId(), client);
		assertEquals(!p1, taskData.getRoot().getMappedAttribute(TaskAttribute.PRIORITY).getValue().equals("P1"));

	}

	public void testSecondSubmit() throws Exception {
		init322();
		String taskNumber = "1";
		RepositoryQuery query = new RepositoryQuery("bugzilla", "blah");
		query.setRepositoryUrl(BugzillaTestConstants.TEST_BUGZILLA_322_URL);
		query.setUrl("?short_desc_type=allwordssubstr&short_desc=&product=TestProduct&long_desc_type=allwordssubstr&long_desc=&order=Importance&ctype=rdf");
		TasksUiInternal.getTaskList().addQuery(query);
		TasksUiInternal.synchronizeQuery(connector, query, null, true);

		ITask task = TasksUiInternal.getTask(BugzillaTestConstants.TEST_BUGZILLA_322_URL, taskNumber, "");
		assertNotNull(task);
		ITaskDataWorkingCopy taskDataState = TasksUi.getTaskDataManager().getWorkingCopy(task);//TasksUiPlugin.getTaskDataManager().getTaskData(task);
		assertNotNull(taskDataState);
		TaskDataModel model = new TaskDataModel(repository, task, taskDataState);

		TaskData taskData = model.getTaskData();
		//remove the token (i.e. unpatched Bugzilla 3.2.2)
		//taskData.getRoot().removeAttribute("token");

		TaskAttribute attrPriority = taskData.getRoot().getAttribute("priority");
		boolean p1 = false;
		if (attrPriority.getValue().equals("P1")) {
			p1 = true;
			attrPriority.setValue("P2");
		} else {
			attrPriority.setValue("P1");
		}

		model.attributeChanged(attrPriority);
		model.save(new NullProgressMonitor());
		submit(task, model);

		TasksUiInternal.synchronizeRepository(repository, false);

		task = TasksUiPlugin.getTaskList().getTask(BugzillaTestConstants.TEST_BUGZILLA_322_URL, taskNumber);
		assertNotNull(task);
		assertEquals(!p1, task.getPriority().equals("P1"));

		// Attempt 2

		taskDataState = TasksUi.getTaskDataManager().getWorkingCopy(task);//TasksUiPlugin.getTaskDataManager().getTaskData(task);
		assertNotNull(taskDataState);
		model = new TaskDataModel(repository, task, taskDataState);

		taskData = model.getTaskData();
		//remove the token (i.e. unpatched Bugzilla 3.2.2)
		//taskData.getRoot().removeAttribute("token");

		attrPriority = taskData.getRoot().getAttribute("priority");
		p1 = false;
		if (attrPriority.getValue().equals("P1")) {
			p1 = true;
			attrPriority.setValue("P2");
		} else {
			attrPriority.setValue("P1");
		}

		model.attributeChanged(attrPriority);
		model.save(new NullProgressMonitor());
		connector.getClientManager().repositoryRemoved(repository);
		submit(task, model);

		TasksUiInternal.synchronizeRepository(repository, false);

		task = TasksUiPlugin.getTaskList().getTask(BugzillaTestConstants.TEST_BUGZILLA_322_URL, taskNumber);
		assertNotNull(task);
		assertEquals(!p1, task.getPriority().equals("P1"));

	}

	protected void submit(ITask task, TaskDataModel model) throws Exception {

		SubmitJob submitJob = TasksUiInternal.getJobFactory().createSubmitTaskJob(connector, model.getTaskRepository(),
				task, model.getTaskData(), model.getChangedOldAttributes());
		Method runMethod = SubmitTaskJob.class.getDeclaredMethod("run", IProgressMonitor.class);
		runMethod.setAccessible(true);
		runMethod.invoke(submitJob, new NullProgressMonitor());

	}

	@SuppressWarnings("null")
	public void testFlags() throws Exception {
		String taskNumber = "10";
		TaskData taskData = BugzillaFixture.current().getTask(taskNumber, client);
		assertNotNull(taskData);
//		TaskMapper mapper = new TaskMapper(taskData);
		ITask task = TasksUi.getRepositoryModel().createTask(repository, taskData.getTaskId());

		Collection<TaskAttribute> a = taskData.getRoot().getAttributes().values();
		TaskAttribute flagA = null;
		TaskAttribute flagB = null;
		TaskAttribute flagC = null;
		TaskAttribute flagD = null;
		TaskAttribute stateA = null;
		TaskAttribute stateB = null;
		TaskAttribute stateC = null;
		TaskAttribute stateD = null;
		for (TaskAttribute taskAttribute : a) {
			if (taskAttribute.getId().startsWith("task.common.kind.flag")) {
				TaskAttribute state = taskAttribute.getAttribute("state");
				if (state.getMetaData().getLabel().equals("BugFlag1")) {
					flagA = taskAttribute;
					stateA = state;
				} else if (state.getMetaData().getLabel().equals("BugFlag2")) {
					flagB = taskAttribute;
					stateB = state;
				} else if (state.getMetaData().getLabel().equals("BugFlag3")) {
					flagC = taskAttribute;
					stateC = state;
				} else if (state.getMetaData().getLabel().equals("BugFlag4")) {
					flagD = taskAttribute;
					stateD = state;
				}
			}
		}
		assertNotNull(flagA);
		assertNotNull(flagB);
		assertNotNull(flagC);
		assertNotNull(flagD);
		assertNotNull(stateA);
		assertNotNull(stateB);
		assertNotNull(stateC);
		assertNotNull(stateD);
		assertEquals("flagA is set(wrong precondidion)", " ", stateA.getValue());
		assertEquals("flagB is set(wrong precondidion)", " ", stateB.getValue());
		assertEquals("flagC is set(wrong precondidion)", " ", stateC.getValue());
		assertEquals("flagD is set(wrong precondidion)", " ", stateD.getValue());
		assertEquals("task.common.kind.flag_type1", flagA.getId());
		assertEquals("task.common.kind.flag_type2", flagB.getId());
		assertEquals("task.common.kind.flag_type5", flagC.getId());
		assertEquals("task.common.kind.flag_type6", flagD.getId());
		Map<String, String> optionA = stateA.getOptions();
		Map<String, String> optionB = stateB.getOptions();
		Map<String, String> optionC = stateC.getOptions();
		Map<String, String> optionD = stateD.getOptions();
		assertEquals(true, optionA.containsKey(""));
		assertEquals(false, optionA.containsKey("?"));
		assertEquals(true, optionA.containsKey("+"));
		assertEquals(true, optionA.containsKey("-"));
		assertEquals(true, optionB.containsKey(""));
		assertEquals(true, optionB.containsKey("?"));
		assertEquals(true, optionB.containsKey("+"));
		assertEquals(true, optionB.containsKey("-"));
		assertEquals(true, optionC.containsKey(""));
		assertEquals(true, optionC.containsKey("?"));
		assertEquals(true, optionC.containsKey("+"));
		assertEquals(true, optionC.containsKey("-"));
		assertEquals(true, optionD.containsKey(""));
		assertEquals(true, optionD.containsKey("?"));
		assertEquals(true, optionD.containsKey("+"));
		assertEquals(true, optionD.containsKey("-"));
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		stateA.setValue("+");
		stateB.setValue("?");
		stateC.setValue("?");
		stateD.setValue("?");
		TaskAttribute requesteeD = flagD.getAttribute("requestee");
		requesteeD.setValue("guest@mylyn.eclipse.org");
		changed.add(flagA);
		changed.add(flagB);
		changed.add(flagC);
		changed.add(flagD);

		BugzillaFixture.current().submitTask(taskData, client);
		task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		taskData = TasksUiPlugin.getTaskDataManager().getTaskData(task);
		assertNotNull(taskData);
		a = taskData.getRoot().getAttributes().values();
		flagA = null;
		flagB = null;
		flagC = null;
		TaskAttribute flagC2 = null;
		flagD = null;
		stateA = null;
		stateB = null;
		stateC = null;
		TaskAttribute stateC2 = null;
		stateD = null;
		for (TaskAttribute taskAttribute : a) {
			if (taskAttribute.getId().startsWith("task.common.kind.flag")) {
				TaskAttribute state = taskAttribute.getAttribute("state");
				if (state.getMetaData().getLabel().equals("BugFlag1")) {
					flagA = taskAttribute;
					stateA = state;
				} else if (state.getMetaData().getLabel().equals("BugFlag2")) {
					flagB = taskAttribute;
					stateB = state;
				} else if (state.getMetaData().getLabel().equals("BugFlag3")) {
					if (flagC == null) {
						flagC = taskAttribute;
						stateC = state;
					} else {
						flagC2 = taskAttribute;
						stateC2 = state;
					}
				} else if (state.getMetaData().getLabel().equals("BugFlag4")) {
					flagD = taskAttribute;
					stateD = state;
				}
			}
		}
		assertNotNull(flagA);
		assertNotNull(flagB);
		assertNotNull(flagC);
		assertNotNull(flagC2);
		assertNotNull(flagD);
		assertNotNull(stateA);
		assertNotNull(stateB);
		assertNotNull(stateC);
		assertNotNull(stateC2);
		assertNotNull(stateD);
		assertEquals("+", stateA.getValue());
		assertEquals("?", stateB.getValue());
		assertEquals("?", stateC.getValue());
		assertEquals(" ", stateC2.getValue());
		assertEquals("?", stateD.getValue());
		requesteeD = flagD.getAttribute("requestee");
		assertNotNull(requesteeD);
		assertEquals("guest@mylyn.eclipse.org", requesteeD.getValue());
		stateA.setValue(" ");
		stateB.setValue(" ");
		stateC.setValue(" ");
		stateD.setValue(" ");
		changed.add(flagA);
		changed.add(flagB);
		changed.add(flagC);
		changed.add(flagD);

		BugzillaFixture.current().submitTask(taskData, client);
		task = generateLocalTaskAndDownload(taskNumber);
		assertNotNull(task);
		taskData = TasksUiPlugin.getTaskDataManager().getTaskData(task);
		assertNotNull(taskData);
		a = taskData.getRoot().getAttributes().values();
		flagA = null;
		flagB = null;
		flagC = null;
		flagC2 = null;
		flagD = null;
		stateA = null;
		stateB = null;
		stateC = null;
		stateC2 = null;
		stateD = null;
		for (TaskAttribute taskAttribute : a) {
			if (taskAttribute.getId().startsWith("task.common.kind.flag")) {
				TaskAttribute state = taskAttribute.getAttribute("state");
				if (state.getMetaData().getLabel().equals("BugFlag1")) {
					flagA = taskAttribute;
					stateA = state;
				} else if (state.getMetaData().getLabel().equals("BugFlag2")) {
					flagB = taskAttribute;
					stateB = state;
				} else if (state.getMetaData().getLabel().equals("BugFlag3")) {
					if (flagC == null) {
						flagC = taskAttribute;
						stateC = state;
					} else {
						flagC2 = taskAttribute;
						stateC2 = state;
					}
				} else if (state.getMetaData().getLabel().equals("BugFlag4")) {
					flagD = taskAttribute;
					stateD = state;
				}
			}
		}
		assertNotNull(flagA);
		assertNotNull(flagB);
		assertNotNull(flagC);
		assertNull(flagC2);
		assertNotNull(flagD);
		assertNotNull(stateA);
		assertNotNull(stateB);
		assertNotNull(stateC);
		assertNull(stateC2);
		assertNotNull(stateD);
		assertEquals(" ", stateA.getValue());
		assertEquals(" ", stateB.getValue());
		assertEquals(" ", stateC.getValue());
		assertEquals(" ", stateD.getValue());
		requesteeD = flagD.getAttribute("requestee");
		assertNotNull(requesteeD);
		assertEquals("", requesteeD.getValue());
	}

	public void testCustomAttributes() throws Exception {
		String taskNumber = "1";
		TaskData taskData = BugzillaFixture.current().getTask(taskNumber, client);
		assertNotNull(taskData);
		TaskMapper mapper = new TaskMapper(taskData);
		ITask task = TasksUi.getRepositoryModel().createTask(repository, taskData.getTaskId());
		assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());
		assertEquals(taskNumber, taskData.getTaskId());

//		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//		assertEquals(format1.parse("2009-09-16 14:11"), mapper.getCreationDate());

		AuthenticationCredentials credentials = repository.getCredentials(AuthenticationType.REPOSITORY);
		assertNotNull("credentials are null", credentials);
		assertNotNull("Repositor User not set", credentials.getUserName());
		assertNotNull("no password for Repository", credentials.getPassword());

		TaskAttribute colorAttribute = mapper.getTaskData().getRoot().getAttribute("cf_multiselect");
		assertNotNull("TaskAttribute Color did not exists", colorAttribute);
		List<String> theColors = colorAttribute.getValues();
		assertNotNull(theColors);
		assertFalse("no colors set", theColors.isEmpty());

		boolean red = false;
		boolean green = false;
		boolean yellow = false;
		boolean blue = false;

		for (Object element : theColors) {
			String string = (String) element;

			if (!red && string.compareTo("Red") == 0) {
				red = true;
			} else if (!green && string.compareTo("Green") == 0) {
				green = true;
			} else if (!yellow && string.compareTo("Yellow") == 0) {
				yellow = true;
			} else if (!blue && string.compareTo("Blue") == 0) {
				blue = true;
			}
		}
		changeCollorAndSubmit(task, taskData, colorAttribute, red, green, yellow, blue);
		taskData = BugzillaFixture.current().getTask(taskNumber, client);
		assertNotNull(taskData);
		mapper = new TaskMapper(taskData);
		task = TasksUi.getRepositoryModel().createTask(repository, taskData.getTaskId());

		colorAttribute = mapper.getTaskData().getRoot().getAttribute("cf_multiselect");
		assertNotNull("TaskAttribute Color did not exists", colorAttribute);
		theColors = colorAttribute.getValues();
		assertNotNull(theColors);
		assertFalse("no colors set", theColors.isEmpty());
		boolean red_new = false;
		boolean green_new = false;
		boolean yellow_new = false;
		boolean blue_new = false;

		for (Object element : theColors) {
			String string = (String) element;

			if (!red_new && string.compareTo("Red") == 0) {
				red_new = true;
			} else if (!green_new && string.compareTo("Green") == 0) {
				green_new = true;
			} else if (!yellow_new && string.compareTo("Yellow") == 0) {
				yellow_new = true;
			} else if (!blue_new && string.compareTo("Blue") == 0) {
				blue_new = true;
			}
		}
		assertTrue("wrong change",
				(!red && green && !yellow && !blue && red_new && green_new && !yellow_new && !blue_new)
						|| (red && green && !yellow && !blue && !red_new && green_new && !yellow_new && !blue_new));
		changeCollorAndSubmit(task, taskData, colorAttribute, red_new, green_new, yellow_new, blue_new);
		assertEquals(SynchronizationState.SYNCHRONIZED, task.getSynchronizationState());

	}

	private void changeCollorAndSubmit(ITask task, TaskData taskData, TaskAttribute colorAttribute, boolean red,
			boolean green, boolean yellow, boolean blue) throws Exception {
		if (!red && green && !yellow && !blue) {
			List<String> newValue = new ArrayList<String>(2);
			newValue.add("Red");
			newValue.add("Green");
			colorAttribute.setValues(newValue);
			Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
			changed.add(colorAttribute);
			// Submit changes
			BugzillaFixture.current().submitTask(taskData, client);
		} else if (red && green && !yellow && !blue) {
			List<String> newValue = new ArrayList<String>(2);
			newValue.add("Green");
			colorAttribute.setValues(newValue);
			Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
			changed.add(colorAttribute);
			// Submit changes
			BugzillaFixture.current().submitTask(taskData, client);
		}
	}

	public void testCustomAttributesNewTask() throws Exception {
		TaskData taskData = BugzillaFixture.current().createTask(PrivilegeLevel.USER, null, null);
		assertNotNull(taskData);
		assertNotNull(taskData.getRoot().getAttribute("token"));
		TaskAttribute productAttribute = taskData.getRoot().getAttribute(BugzillaAttribute.PRODUCT.getKey());
		assertNotNull(productAttribute);
		assertEquals("ManualTest" + "", productAttribute.getValue());
		TaskAttribute cfAttribute1 = taskData.getRoot().getAttribute("cf_freetext");
		assertNotNull(cfAttribute1);
		TaskAttribute cfAttribute2 = taskData.getRoot().getAttribute("cf_dropdown");
		assertNotNull(cfAttribute2);
		TaskAttribute cfAttribute3 = taskData.getRoot().getAttribute("cf_largetextbox");
		assertNotNull(cfAttribute3);
		TaskAttribute cfAttribute4 = taskData.getRoot().getAttribute("cf_multiselect");
		assertNotNull(cfAttribute4);
		TaskAttribute cfAttribute5 = taskData.getRoot().getAttribute("cf_datetime");
		assertNotNull(cfAttribute5);
		TaskAttribute cfAttribute6 = taskData.getRoot().getAttribute("cf_bugid");
		assertNotNull(cfAttribute6);
	}

	public void testLeadingZeros() throws Exception {
		String taskNumber = "0010";
		TaskData taskData = BugzillaFixture.current().getTask(taskNumber, client);
		assertNotNull(taskData);
//		ITask task = TasksUi.getRepositoryModel().createTask(repository, taskData.getTaskId());
		assertNotNull(taskData);
		TaskAttribute idAttribute = taskData.getRoot().getAttribute(BugzillaAttribute.BUG_ID.getKey());
		assertNotNull(idAttribute);
		assertEquals("10", idAttribute.getValue());
	}
}
