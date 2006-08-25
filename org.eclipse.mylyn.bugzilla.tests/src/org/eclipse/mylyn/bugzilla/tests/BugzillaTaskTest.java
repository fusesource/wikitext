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

import java.util.Date;

import junit.framework.TestCase;

import org.eclipse.mylar.internal.bugzilla.core.BugzillaAttributeFactory;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaReportElement;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.internal.bugzilla.ui.tasklist.BugzillaOfflineTaskHandler;
import org.eclipse.mylar.internal.bugzilla.ui.tasklist.BugzillaTask;
import org.eclipse.mylar.tasks.core.TaskComment;
import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;

/**
 * @author Mik Kersten
 */
public class BugzillaTaskTest extends TestCase {

	private BugzillaAttributeFactory attributeFactory = new BugzillaAttributeFactory();

	private BugzillaOfflineTaskHandler offlineHandler = new BugzillaOfflineTaskHandler();

	@Override
	protected void setUp() throws Exception {
		// ignore
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		// ignore
		super.tearDown();
	}

	public void testCompletionDate() throws Exception {
		BugzillaTask task = new BugzillaTask("bug-1", "description", true);
		RepositoryTaskData report = new RepositoryTaskData(new BugzillaAttributeFactory(),
				BugzillaCorePlugin.REPOSITORY_KIND, IBugzillaConstants.ECLIPSE_BUGZILLA_URL, "1");
		task.setTaskData(report);
		assertNull(task.getCompletionDate());

		Date now = new Date();
		String nowTimeStamp = BugzillaOfflineTaskHandler.comment_creation_ts_format.format(now);

		TaskComment taskComment = new TaskComment(new BugzillaAttributeFactory(), report, 1);
		RepositoryTaskAttribute attribute = attributeFactory.createAttribute(BugzillaReportElement.BUG_WHEN
				.getKeyString());
		attribute.setValue(nowTimeStamp);
		taskComment.addAttribute(BugzillaReportElement.BUG_WHEN.getKeyString(), attribute);
		report.addComment(taskComment);
		assertNull(task.getCompletionDate());

		RepositoryTaskAttribute resolvedAttribute = attributeFactory.createAttribute(BugzillaReportElement.BUG_STATUS
				.getKeyString());
		resolvedAttribute.setValue(IBugzillaConstants.VALUE_STATUS_RESOLVED);
		report.addAttribute(BugzillaReportElement.BUG_STATUS.getKeyString(), resolvedAttribute);
		assertNotNull(task.getCompletionDate());
		assertEquals(offlineHandler
				.getDateForAttributeType(BugzillaReportElement.BUG_WHEN.getKeyString(), nowTimeStamp), task
				.getCompletionDate());

	}

}
