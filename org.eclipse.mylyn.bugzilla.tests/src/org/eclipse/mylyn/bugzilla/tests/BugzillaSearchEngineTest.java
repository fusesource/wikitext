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

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaRepositoryQuery;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.QueryHitCollector;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;

/**
 * @author Rob Elves
 */
public class BugzillaSearchEngineTest extends TestCase {

	private static final String MAX_HITS = "100";
	private static final String QUERY_NAME = "Query Page Name";
	private static final String BUG_DESC_SUBSTRING_SEARCH = "/buglist.cgi?short_desc_type=allwordssubstr&short_desc=";//search-match-test&amp";
	private static final String SEARCH_DESCRIPTION = "search-match-test";
	private static final int NUM_EXPECTED_HITS = 2;
	private static final int NUM_REPOSITORIES = 0;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TasksUiPlugin.getRepositoryManager().clearRepositories(TasksUiPlugin.getDefault().getRepositoriesFilePath());
		assertEquals(NUM_REPOSITORIES, TasksUiPlugin.getRepositoryManager().getRepositories(BugzillaCorePlugin.REPOSITORY_KIND).size());
	}

	@Override
	protected void tearDown() throws Exception {
		TasksUiPlugin.getRepositoryManager().clearRepositories(TasksUiPlugin.getDefault().getRepositoriesFilePath());
		super.tearDown();
	}

//	public void testSearching216() throws Exception {
//		TaskRepository repository = new TaskRepository(BugzillaPlugin.REPOSITORY_KIND, 
//				IBugzillaConstants.TEST_BUGZILLA_216_URL, IBugzillaConstants.BugzillaServerVersion.SERVER_216.toString());
//		MylarTaskListPlugin.getRepositoryManager().addRepository(repository);
//		List<AbstractQueryHit> hits = runQuery(IBugzillaConstants.TEST_BUGZILLA_216_URL, SEARCH_DESCRIPTION);
//		assertEquals(NUM_EXPECTED_HITS, hits.size());		
//	}
	
	public void testSearching218() throws Exception {
		TaskRepository repository = new TaskRepository(BugzillaCorePlugin.REPOSITORY_KIND,
				IBugzillaConstants.TEST_BUGZILLA_218_URL, IBugzillaConstants.BugzillaServerVersion.SERVER_218.toString());
		TasksUiPlugin.getRepositoryManager().addRepository(repository, TasksUiPlugin.getDefault().getRepositoriesFilePath());
		assertEquals(NUM_EXPECTED_HITS, runQuery(IBugzillaConstants.TEST_BUGZILLA_218_URL, SEARCH_DESCRIPTION).size());		
	}
		
	public void testSearching220() throws Exception {
		TaskRepository repository = new TaskRepository(BugzillaCorePlugin.REPOSITORY_KIND,
				IBugzillaConstants.TEST_BUGZILLA_220_URL, IBugzillaConstants.BugzillaServerVersion.SERVER_220.toString());
		TasksUiPlugin.getRepositoryManager().addRepository(repository, TasksUiPlugin.getDefault().getRepositoriesFilePath());
		assertEquals(NUM_EXPECTED_HITS, runQuery(IBugzillaConstants.TEST_BUGZILLA_220_URL, SEARCH_DESCRIPTION).size());		
	}
		
	public void testSearching2201() throws Exception {
		TaskRepository repository = new TaskRepository(BugzillaCorePlugin.REPOSITORY_KIND,
				IBugzillaConstants.TEST_BUGZILLA_2201_URL, IBugzillaConstants.BugzillaServerVersion.SERVER_220.toString());
		TasksUiPlugin.getRepositoryManager().addRepository(repository, TasksUiPlugin.getDefault().getRepositoriesFilePath());		
		assertEquals(NUM_EXPECTED_HITS, runQuery(IBugzillaConstants.TEST_BUGZILLA_2201_URL, SEARCH_DESCRIPTION).size());		
	}
	
	public void testSearching222() throws Exception {
		TaskRepository repository = new TaskRepository(BugzillaCorePlugin.REPOSITORY_KIND, 
				IBugzillaConstants.TEST_BUGZILLA_222_URL, IBugzillaConstants.BugzillaServerVersion.SERVER_222.toString());
		TasksUiPlugin.getRepositoryManager().addRepository(repository, TasksUiPlugin.getDefault().getRepositoriesFilePath());		
		List<AbstractQueryHit> hits = runQuery(IBugzillaConstants.TEST_BUGZILLA_222_URL, SEARCH_DESCRIPTION);
		assertEquals(NUM_EXPECTED_HITS, hits.size());		
	}
		
	private List<AbstractQueryHit> runQuery(String repositoryURL, String SearchString) throws Exception {		
		TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(BugzillaCorePlugin.REPOSITORY_KIND, repositoryURL);
		assertNotNull(repository);		
		
		final BugzillaRepositoryQuery repositoryQuery = new BugzillaRepositoryQuery(
				repository.getUrl(), 
				repository.getUrl() + BUG_DESC_SUBSTRING_SEARCH+SearchString, 
				QUERY_NAME, 
				MAX_HITS, TasksUiPlugin.getTaskListManager().getTaskList());
		
		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(BugzillaCorePlugin.REPOSITORY_KIND);
		
		QueryHitCollector collector = new QueryHitCollector(TasksUiPlugin.getTaskListManager().getTaskList());
		
		connector.performQuery(repositoryQuery, repository, new NullProgressMonitor(), collector);
		
		//results.addAll(connector.performQuery(repositoryQuery, new NullProgressMonitor(), new MultiStatus(TasksUiPlugin.PLUGIN_ID, IStatus.OK, "Query result", null)));
		return collector.getHits();			
	}
	
	
	
}
