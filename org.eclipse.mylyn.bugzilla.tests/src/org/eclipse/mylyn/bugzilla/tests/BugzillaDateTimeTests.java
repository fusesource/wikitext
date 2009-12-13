/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.bugzilla.tests;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.mylyn.bugzilla.tests.support.BugzillaFixture;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaAttribute;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaAttributeMapper;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaClient;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tests.util.TestUtil.PrivilegeLevel;

/**
 * @author Frank Becker
 * @author Robert Elves
 */
public class BugzillaDateTimeTests extends AbstractBugzillaTest {

	private TaskRepository repository;

	private BugzillaRepositoryConnector connector;

	@SuppressWarnings("unused")
	private BugzillaClient client;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		client = BugzillaFixture.current().client(PrivilegeLevel.USER);
		repository = BugzillaFixture.current().repository();
		connector = BugzillaFixture.current().connector();
	}

	public void testDateFormatParsing() {
		TimeZone defaultTimeZone = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

			TaskData taskData = new TaskData(new BugzillaAttributeMapper(repository, connector), "bugzilla", "repourl",
					"1");
			TaskAttribute attribute = taskData.getRoot().createAttribute(BugzillaAttribute.CREATION_TS.getKey());
			attribute.setValue("2006-05-08 15:04 PST");
			Date date = taskData.getAttributeMapper().getDateValue(attribute);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			assertEquals(2006, calendar.get(Calendar.YEAR));
			assertEquals(4, calendar.get(Calendar.MONTH));
			assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
			assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
			assertEquals(4, calendar.get(Calendar.MINUTE));
			assertEquals(0, calendar.get(Calendar.SECOND));

			attribute.setValue("2006-05-08 15:04:11 PST");
			date = taskData.getAttributeMapper().getDateValue(attribute);
			calendar = Calendar.getInstance();
			calendar.setTime(date);
			assertEquals(2006, calendar.get(Calendar.YEAR));
			assertEquals(4, calendar.get(Calendar.MONTH));
			assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
			assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
			assertEquals(4, calendar.get(Calendar.MINUTE));
			assertEquals(11, calendar.get(Calendar.SECOND));

			attribute.setValue("2006-05-08 15:04:11 -0800");
			date = taskData.getAttributeMapper().getDateValue(attribute);
			calendar = Calendar.getInstance();
			calendar.setTime(date);
			assertEquals(2006, calendar.get(Calendar.YEAR));
			assertEquals(4, calendar.get(Calendar.MONTH));
			assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
			assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
			assertEquals(4, calendar.get(Calendar.MINUTE));
			assertEquals(11, calendar.get(Calendar.SECOND));

			attribute.setValue("2006-05-08 15:04 -0800");
			date = taskData.getAttributeMapper().getDateValue(attribute);
			calendar = Calendar.getInstance();
			calendar.setTime(date);
			assertEquals(2006, calendar.get(Calendar.YEAR));
			assertEquals(4, calendar.get(Calendar.MONTH));
			assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
			assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
			assertEquals(4, calendar.get(Calendar.MINUTE));
			assertEquals(0, calendar.get(Calendar.SECOND));

			attribute.setValue("2006-05-08 15:04:11");
			date = taskData.getAttributeMapper().getDateValue(attribute);
			calendar = Calendar.getInstance();
			calendar.setTime(date);
			assertEquals(2006, calendar.get(Calendar.YEAR));
			assertEquals(4, calendar.get(Calendar.MONTH));
			assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
			assertEquals(15, calendar.get(Calendar.HOUR_OF_DAY));
			assertEquals(4, calendar.get(Calendar.MINUTE));
			assertEquals(11, calendar.get(Calendar.SECOND));

			attribute.setValue("2006-05-08 15:04");
			date = taskData.getAttributeMapper().getDateValue(attribute);
			calendar = Calendar.getInstance();
			calendar.setTime(date);
			assertEquals(2006, calendar.get(Calendar.YEAR));
			assertEquals(4, calendar.get(Calendar.MONTH));
			assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
			assertEquals(15, calendar.get(Calendar.HOUR_OF_DAY));
			assertEquals(4, calendar.get(Calendar.MINUTE));
			assertEquals(0, calendar.get(Calendar.SECOND));

			attribute.setValue("2006-05-08");
			date = taskData.getAttributeMapper().getDateValue(attribute);
			calendar = Calendar.getInstance();
			calendar.setTime(date);
			assertEquals(2006, calendar.get(Calendar.YEAR));
			assertEquals(4, calendar.get(Calendar.MONTH));
			assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
			assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
			assertEquals(0, calendar.get(Calendar.MINUTE));
			assertEquals(0, calendar.get(Calendar.SECOND));
		} finally {
			TimeZone.setDefault(defaultTimeZone);
		}
	}
}
