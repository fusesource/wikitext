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

package org.eclipse.mylar.bugzilla.tests;

import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;

import org.eclipse.mylar.internal.bugzilla.core.BugzillaAttributeFactory;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaReportElement;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaTask;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaTaskDataHandler;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;
import org.eclipse.mylar.tasks.core.Task;
import org.eclipse.mylar.tasks.core.TaskComment;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * @author Mik Kersten
 */
public class BugzillaTaskTest extends TestCase {

	private BugzillaAttributeFactory attributeFactory = new BugzillaAttributeFactory();


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		new BugzillaTaskDataHandler((BugzillaRepositoryConnector)TasksUiPlugin.getRepositoryManager().getRepositoryConnector(BugzillaCorePlugin.REPOSITORY_KIND));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testCompletionDate() throws Exception {
		BugzillaTask task = new BugzillaTask("repo", "1", "summary", true);
		RepositoryTaskData taskData = new RepositoryTaskData(new BugzillaAttributeFactory(),
				BugzillaCorePlugin.REPOSITORY_KIND, IBugzillaConstants.ECLIPSE_BUGZILLA_URL, "1", Task.DEFAULT_TASK_KIND);
				
		//XXX rewrite test
		
		assertNull(task.getCompletionDate());

		Date now = new Date();
		String nowTimeStamp = new SimpleDateFormat(BugzillaAttributeFactory.comment_creation_ts_format).format(now);

		TaskComment taskComment = new TaskComment(new BugzillaAttributeFactory(), 1);
		RepositoryTaskAttribute attribute = attributeFactory.createAttribute(BugzillaReportElement.BUG_WHEN
				.getKeyString());
		attribute.setValue(nowTimeStamp);
		taskComment.addAttribute(BugzillaReportElement.BUG_WHEN.getKeyString(), attribute);
		taskData.addComment(taskComment);
		assertNull(task.getCompletionDate());

		RepositoryTaskAttribute resolvedAttribute = attributeFactory.createAttribute(BugzillaReportElement.BUG_STATUS
				.getKeyString());
		resolvedAttribute.setValue(IBugzillaConstants.VALUE_STATUS_RESOLVED);
		taskData.addAttribute(BugzillaReportElement.BUG_STATUS.getKeyString(), resolvedAttribute);
		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(BugzillaCorePlugin.REPOSITORY_KIND);
		connector.updateTaskFromTaskData(new TaskRepository(BugzillaCorePlugin.REPOSITORY_KIND, "http://eclipse.org"), task, taskData);
		assertNotNull(task.getCompletionDate());
		assertEquals(taskData.getAttributeFactory()
				.getDateForAttributeType(BugzillaReportElement.BUG_WHEN.getKeyString(), nowTimeStamp), task
				.getCompletionDate());

	}

}
