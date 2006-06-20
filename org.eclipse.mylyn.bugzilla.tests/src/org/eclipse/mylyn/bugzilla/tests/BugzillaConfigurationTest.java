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

import junit.framework.TestCase;

import org.eclipse.mylar.internal.bugzilla.core.BugzillaPlugin;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.internal.bugzilla.core.RepositoryConfiguration;
import org.eclipse.mylar.internal.bugzilla.core.RepositoryConfigurationFactory;
import org.eclipse.mylar.provisional.tasklist.TaskRepository;

public class BugzillaConfigurationTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	public void test222RDFProductConfig() throws Exception {
		RepositoryConfigurationFactory factory = RepositoryConfigurationFactory.getInstance();
		TaskRepository repository = new TaskRepository(BugzillaPlugin.REPOSITORY_KIND, IBugzillaConstants.TEST_BUGZILLA_222_URL);
		RepositoryConfiguration config = factory.getConfiguration(repository.getUrl(), null, repository.getUserName(), repository.getPassword(), null);
		assertNotNull(config);
		assertEquals("2.22", config.getInstallVersion());
		assertEquals(7, config.getStatusValues().size());
		assertEquals(8, config.getResolutions().size());
		assertEquals(4, config.getPlatforms().size());
		assertEquals(5, config.getOSs().size());
		assertEquals(5, config.getPriorities().size());
		assertEquals(7, config.getSeverities().size());
		assertEquals(3, config.getProducts().size());
		assertEquals(4, config.getOpenStatusValues().size());
		assertEquals(1, config.getComponents("TestProduct").size());
		assertEquals(1, config.getVersions("TestProduct").size());
		assertEquals(0, config.getTargetMilestones("TestProduct").size());
	}
		
	public void test2201RDFProductConfig() throws Exception {
		RepositoryConfigurationFactory factory = RepositoryConfigurationFactory.getInstance();
		TaskRepository repository = new TaskRepository(BugzillaPlugin.REPOSITORY_KIND, IBugzillaConstants.TEST_BUGZILLA_2201_URL);
		RepositoryConfiguration config = factory.getConfiguration(repository.getUrl(), null, repository.getUserName(), repository.getPassword(), null);
		assertNotNull(config);
		assertEquals("2.20.1", config.getInstallVersion());
		assertEquals(7, config.getStatusValues().size());
		assertEquals(8, config.getResolutions().size());
		assertEquals(4, config.getPlatforms().size());
		assertEquals(5, config.getOSs().size());
		assertEquals(5, config.getPriorities().size());
		assertEquals(7, config.getSeverities().size());
		assertEquals(1, config.getProducts().size());
		assertEquals(4, config.getOpenStatusValues().size());
		assertEquals(2, config.getComponents("TestProduct").size());
		assertEquals(1, config.getVersions("TestProduct").size());	
		// assertEquals(1, config.getTargetMilestones("TestProduct").size());
	}
	
	public void test220RDFProductConfig() throws Exception {
		RepositoryConfigurationFactory factory = RepositoryConfigurationFactory.getInstance();
		TaskRepository repository = new TaskRepository(BugzillaPlugin.REPOSITORY_KIND, IBugzillaConstants.TEST_BUGZILLA_220_URL);
		RepositoryConfiguration config = factory.getConfiguration(repository.getUrl(), null, repository.getUserName(), repository.getPassword(), null);
		assertNotNull(config);
		assertEquals("2.20", config.getInstallVersion());
		assertEquals(7, config.getStatusValues().size());
		assertEquals(8, config.getResolutions().size());
		assertEquals(4, config.getPlatforms().size());
		assertEquals(5, config.getOSs().size());
		assertEquals(5, config.getPriorities().size());
		assertEquals(7, config.getSeverities().size());
		assertEquals(2, config.getProducts().size());
		assertEquals(4, config.getOpenStatusValues().size());
		assertEquals(2, config.getComponents("TestProduct").size());
		assertEquals(1, config.getVersions("TestProduct").size());
		// assertEquals(1, config.getTargetMilestones("TestProduct").size());
	}
	
	public void test218RDFProductConfig() throws Exception {
		RepositoryConfigurationFactory factory = RepositoryConfigurationFactory.getInstance();
		TaskRepository repository = new TaskRepository(BugzillaPlugin.REPOSITORY_KIND, IBugzillaConstants.TEST_BUGZILLA_218_URL);
		RepositoryConfiguration config = factory.getConfiguration(repository.getUrl(), null, repository.getUserName(), repository.getPassword(), null);
		assertNotNull(config);
		assertEquals("2.18.5", config.getInstallVersion());
		assertEquals(7, config.getStatusValues().size());
		assertEquals(8, config.getResolutions().size());
		assertEquals(8, config.getPlatforms().size());
		assertEquals(36, config.getOSs().size());
		assertEquals(5, config.getPriorities().size());
		assertEquals(7, config.getSeverities().size());
		assertEquals(1, config.getProducts().size());
		assertEquals(4, config.getOpenStatusValues().size());
		assertEquals(1, config.getComponents("TestProduct").size());
		assertEquals(1, config.getVersions("TestProduct").size());
		// assertEquals(1, config.getTargetMilestones("TestProduct").size());
	}
	
	public void testEclipseRDFProductConfig() throws Exception {
		RepositoryConfigurationFactory factory = RepositoryConfigurationFactory.getInstance();
		TaskRepository repository = new TaskRepository(BugzillaPlugin.REPOSITORY_KIND, IBugzillaConstants.ECLIPSE_BUGZILLA_URL);
		RepositoryConfiguration config = factory.getConfiguration(repository.getUrl(), null, repository.getUserName(), repository.getPassword(), null);
		assertNotNull(config);
		assertEquals("2.20.1", config.getInstallVersion());
		assertEquals(7, config.getStatusValues().size());
		assertEquals(8, config.getResolutions().size());
		assertEquals(6, config.getPlatforms().size());
		assertEquals(28, config.getOSs().size());
		assertEquals(5, config.getPriorities().size());
		assertEquals(7, config.getSeverities().size());
		assertEquals(52, config.getProducts().size());
		assertEquals(4, config.getOpenStatusValues().size());
		assertEquals(10, config.getComponents("Mylar").size());
		assertEquals(21, config.getKeywords().size());
		// assertEquals(10, config.getComponents("Hyades").size());
		// assertEquals(1, config.getTargetMilestones("TestProduct").size());
	}
	
	public void testRepositoryConfigurationCachePersistance() throws Exception {
		RepositoryConfiguration configuration1 = new RepositoryConfiguration();
		configuration1.setRepositoryUrl("url1");
		configuration1.addProduct("Test Product 1");
		assertEquals(1, configuration1.getProducts().size());

		RepositoryConfiguration configuration2 = new RepositoryConfiguration();
		configuration1.setRepositoryUrl("url2");
		configuration2.addProduct("Test Product 2");
		assertEquals(1, configuration2.getProducts().size());
		
		BugzillaPlugin.addRepositoryConfiguration(configuration1);
		BugzillaPlugin.addRepositoryConfiguration(configuration2);
		BugzillaPlugin.writeRepositoryConfigFile();
		BugzillaPlugin.getDefault().removeConfiguration(configuration1);
		BugzillaPlugin.getDefault().removeConfiguration(configuration2);
		assertNull(BugzillaPlugin.getRepositoryConfiguration(configuration1.getRepositoryUrl()));
		assertNull(BugzillaPlugin.getRepositoryConfiguration(configuration2.getRepositoryUrl()));
		BugzillaPlugin.readRepositoryConfigurationFile();
		assertNotNull(BugzillaPlugin.getRepositoryConfiguration(configuration1.getRepositoryUrl()));
		assertNotNull(BugzillaPlugin.getRepositoryConfiguration(configuration2.getRepositoryUrl()));
		RepositoryConfiguration testLoadedConfig = BugzillaPlugin.getRepositoryConfiguration(configuration1.getRepositoryUrl());
		assertEquals(1, testLoadedConfig.getProducts().size());
		assertEquals(configuration1.getProducts().get(0), testLoadedConfig.getProducts().get(0));
	}
}
