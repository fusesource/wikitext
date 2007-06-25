/*******************************************************************************
 * Copyright (c) 2003 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.tasks.ui.search;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.ITaskCollector;
import org.eclipse.mylyn.tasks.core.ITaskFactory;
import org.eclipse.mylyn.tasks.core.QueryHitCollector;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskList;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.PlatformUI;

/**
 * Used for returning results from Eclipse Search view. Collects results of a
 * repository search.
 * 
 * @author Rob Elves
 * @since 2.0
 */
public class SearchHitCollector implements ISearchQuery, ITaskCollector {

	private static final String LABEL_MAX_HITS_REACHED = "Max allowed number of hits returned exceeded. Some hits may not be displayed. Please narrow query scope.";

	protected Set<AbstractTask> taskResults = new HashSet<AbstractTask>();

	/** The number of matches found */
	private int matchCount;

	/** The string to display to the user while querying */
	private static final String STARTING = "querying the server";

	/** The string to display to the user when we have 1 match */
	private static final String MATCH = "1 match";

	/** The string to display to the user when we have multiple or no matches */
	private static final String MATCHES = "{0} matches";

	/** The string to display to the user when the query is done */
	private static final String DONE = "done";

	private final TaskList taskList;

	private final ITaskFactory taskFactory;

	private static final String QUERYING_REPOSITORY = "Querying Repository...";
	
	private String type;

	private TaskRepository repository;

	private AbstractRepositoryQuery repositoryQuery;

	private RepositorySearchResult searchResult;

	private IProgressMonitor monitor;

	public SearchHitCollector(TaskList tasklist, TaskRepository repository, AbstractRepositoryQuery repositoryQuery, ITaskFactory taskFactory) {
		this.taskList = tasklist;
		this.repository = repository;
		this.repositoryQuery = repositoryQuery;
		this.searchResult = new RepositorySearchResult(this);
		this.taskFactory = taskFactory;
	}

	public void aboutToStart(int startMatchCount) {
		taskResults.clear();
		matchCount = startMatchCount;
		monitor.setTaskName(STARTING);

		searchResult.removeAll();
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				NewSearchUI.activateSearchResultView();
			}
		});
	}

	public void accept(AbstractTask task) {
		if (task == null) {
			throw new IllegalArgumentException();
		}

		AbstractTask hitTask = taskList.getTask(task.getHandleIdentifier());
		if (hitTask == null) {
			hitTask = task;
		}

		
		if (!monitor.isCanceled()) {
			monitor.subTask(getFormattedMatchesString(searchResult.getMatchCount()));
			monitor.worked(1);
		}
		
		taskResults.add(hitTask);	
		this.searchResult.addMatch(new Match(hitTask, 0, 0));
	}

	public void accept(RepositoryTaskData taskData) throws CoreException {
		if (taskData == null) {
			throw new IllegalArgumentException();
		}
		
		AbstractTask task = taskFactory.createTask(taskData, new SubProgressMonitor(monitor, 1));
		if (task != null) {
			
			if (!monitor.isCanceled()) {
				monitor.subTask(getFormattedMatchesString(searchResult.getMatchCount()));
				monitor.worked(1);
			}
			
			taskResults.add(task);			
			this.searchResult.addMatch(new Match(task, 0, 0));
		}
	}

	public String getLabel() {
		return QUERYING_REPOSITORY;
	}
	
	public String getTypeLabel() {
		return type;
	}

	public void setTypeLabel(String type) {
		this.type = type;
	}
	
	public boolean canRerun() {
		return true;
	}

	public boolean canRunInBackground() {
		return true;
	}

	public ISearchResult getSearchResult() {
		if (searchResult.getMatchCount() >= QueryHitCollector.MAX_HITS) {
			StatusHandler.displayStatus("Maximum hits reached", RepositoryStatus.createStatus(repository.getUrl(),
					IStatus.WARNING, TasksUiPlugin.ID_PLUGIN, LABEL_MAX_HITS_REACHED));
		}
		return searchResult;
	}

	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		this.monitor = monitor;
		
		try {
			monitor.beginTask(QUERYING_REPOSITORY, IProgressMonitor.UNKNOWN);
			
			aboutToStart(0);

			if (monitor.isCanceled()) {
				throw new OperationCanceledException("Search cancelled");
			}
			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
					repositoryQuery.getRepositoryKind());
			if (connector != null) {
				IStatus status = connector.performQuery(repositoryQuery, repository, monitor, this);
				if (!status.isOK()) {
					StatusHandler.displayStatus("Search failed", status);
				}
			} else {
				return new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, IStatus.OK,
						"repository connector could not be found", null);
			}

			return Status.OK_STATUS;
		} finally {
			done();
		}
	}

	public void done() {
		if (monitor != null && !monitor.isCanceled()) {
			// if the operation is cancelled, finish with the data that we
			// already have
			String matchesString = getFormattedMatchesString(matchCount);
			monitor.setTaskName(MessageFormat.format(DONE, new Object[] { matchesString }));
			monitor.done();
		}

		// Cut no longer used references because the collector might be re-used
		monitor = null;
	}
	
	protected String getFormattedMatchesString(int count) {
		if (count == 1) {
			return MATCH;
		}
		Object[] messageFormatArgs = { new Integer(count) };
		return MessageFormat.format(MATCHES, messageFormatArgs);
	}

	public Set<AbstractTask> getTasks() {
		return taskResults;
	}

	public AbstractRepositoryQuery getRepositoryQuery() {
		return repositoryQuery;
	}	

}
