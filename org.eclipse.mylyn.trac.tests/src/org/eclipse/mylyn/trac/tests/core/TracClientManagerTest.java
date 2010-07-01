/*******************************************************************************
 * Copyright (c) 2006, 2008 Steffen Pingel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Steffen Pingel - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.trac.tests.core;

import java.io.File;
import java.util.Arrays;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.trac.core.TracClientManager;
import org.eclipse.mylyn.internal.trac.core.TracCorePlugin;
import org.eclipse.mylyn.internal.trac.core.client.ITracClient;
import org.eclipse.mylyn.internal.trac.core.client.ITracClient.Version;
import org.eclipse.mylyn.internal.trac.core.model.TracMilestone;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
import org.eclipse.mylyn.trac.tests.support.TracTestConstants;

/**
 * @author Steffen Pingel
 */
public class TracClientManagerTest extends TestCase {

	public void testNullCache() throws Exception {
		TaskRepository taskRepository = new TaskRepository(TracCorePlugin.CONNECTOR_KIND,
				TracTestConstants.TEST_TRAC_010_URL);
		taskRepository.setVersion(Version.TRAC_0_9.name());

		TracClientManager manager = new TracClientManager(null, new TaskRepositoryLocationFactory());
		ITracClient client = manager.getTracClient(taskRepository);
		assertNull(client.getMilestones());

		manager.writeCache();
		assertNull(client.getMilestones());
	}

	public void testReadCache() throws Exception {
		TaskRepository taskRepository = new TaskRepository(TracCorePlugin.CONNECTOR_KIND,
				TracTestConstants.TEST_TRAC_010_URL);
		taskRepository.setVersion(Version.TRAC_0_9.name());

		File file = File.createTempFile("mylyn", null);
		file.deleteOnExit();

		TracClientManager manager = new TracClientManager(file, new TaskRepositoryLocationFactory());
		ITracClient client = manager.getTracClient(taskRepository);
		assertNull(client.getMilestones());
	}

	public void testWriteCache() throws Exception {
		TaskRepository taskRepository = new TaskRepository(TracCorePlugin.CONNECTOR_KIND,
				TracTestConstants.TEST_TRAC_010_URL);
		taskRepository.setVersion(Version.TRAC_0_9.name());
		taskRepository.setCredentials(AuthenticationType.REPOSITORY, null, false);

		File file = File.createTempFile("mylyn", null);
		file.deleteOnExit();

		TracClientManager manager = new TracClientManager(file, new TaskRepositoryLocationFactory());
		ITracClient client = manager.getTracClient(taskRepository);
		assertNull(client.getMilestones());

		client.updateAttributes(new NullProgressMonitor(), false);
		assertTrue(client.getMilestones().length > 0);
		TracMilestone[] milestones = client.getMilestones();

		manager.writeCache();
		manager = new TracClientManager(file, new TaskRepositoryLocationFactory());
		assertEquals(Arrays.asList(milestones), Arrays.asList(client.getMilestones()));
	}

}
