/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.jira;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.mylar.internal.core.util.MylarStatusHandler;
import org.eclipse.mylar.internal.jira.ui.wizards.AddExistingJiraTaskWizard;
import org.eclipse.mylar.internal.jira.ui.wizards.JiraRepositorySettingsPage;
import org.eclipse.mylar.internal.jira.ui.wizards.NewJiraQueryWizard;
import org.eclipse.mylar.internal.tasklist.ui.TaskListUiUtil;
import org.eclipse.mylar.internal.tasklist.ui.views.RetrieveTitleFromUrlJob;
import org.eclipse.mylar.internal.tasklist.ui.views.TaskListView;
import org.eclipse.mylar.internal.tasklist.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.mylar.provisional.tasklist.AbstractRepositoryConnector;
import org.eclipse.mylar.provisional.tasklist.AbstractRepositoryQuery;
import org.eclipse.mylar.provisional.tasklist.AbstractRepositoryTask;
import org.eclipse.mylar.provisional.tasklist.ITask;
import org.eclipse.mylar.provisional.tasklist.MylarTaskListPlugin;
import org.eclipse.mylar.provisional.tasklist.TaskRepository;
import org.eclipse.swt.widgets.Display;

/**
 * This plugin is a task repository client for Jira. A single kind of repository
 * client may have multiple repositories of that kind.
 * 
 * @author Mik Kersten
 * @author Wesley Coelho (initial integration patch)
 */
public class JiraRepositoryClient extends AbstractRepositoryConnector {

	private static final String LABEL_JOB_SYNCHRONIZE = "Jira Synchronize";
	
	/** Name initially given to new tasks. Public for testing */
	public static final String NEW_TASK_DESC = "New Task";

	public String getLabel() {
		return MylarJiraPlugin.JIRA_CLIENT_LABEL;
	}

	public String getKind() {
		return MylarJiraPlugin.JIRA_REPOSITORY_KIND;
	}

	public String toString() {
		return getLabel();
	}

	public ITask createTaskFromExistingId(TaskRepository repository, String id) {
		return null;
//		String url = repository.getUrl() + MylarJiraPlugin.ISSUE_URL_PREFIX + id;
//		String handle = AbstractRepositoryTask.getHandle(repository.getUrl().toExternalForm(), id);
//		JiraTask newTask = new JiraTask(handle, NEW_TASK_DESC, true);
//		MylarTaskListPlugin.getTaskListManager().getTaskList().addTaskToArchive(newTask);
//		retrieveTaskDescription(newTask);
//		return newTask;
	} 

	public AbstractRepositorySettingsPage getSettingsPage() {
		return new JiraRepositorySettingsPage();
	}

	public IWizard getQueryWizard(TaskRepository repository) {
		return new NewJiraQueryWizard(repository);
	}

	public IWizard getAddExistingTaskWizard(TaskRepository repository) {
		return new AddExistingJiraTaskWizard(repository);
	}

	@Override
	public void synchronize() {
		Job synchronizeJob = new Job(LABEL_JOB_SYNCHRONIZE) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				refreshFilters();
				return Status.OK_STATUS;
			}
		};
		synchronizeJob.schedule();
	}

	@Override
	public Job synchronize(ITask task, boolean forceUpdate, IJobChangeListener listener) {
		// Sync for individual tasks not implemented
		return new Job(LABEL_JOB_SYNCHRONIZE) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				refreshFilters();
				return Status.OK_STATUS;
			}

		};
	}

	@Override
	public void openEditQueryDialog(AbstractRepositoryQuery query) {
		JiraFilter filter = (JiraFilter) query;
		String title = "Filter: " + filter.getDescription();
		TaskListUiUtil.openUrl(title, title, filter.getQueryUrl());
	}

	public void refreshFilters() {
		for (AbstractRepositoryQuery query : MylarTaskListPlugin.getTaskListManager().getTaskList().getQueries()) {
			if (query instanceof JiraFilter) {
				((JiraFilter) query).refreshHits();
			}
		}
	}

	/**
	 * Attempts to set the task pageTitle to the title from the specified url
	 */
	protected void retrieveTaskDescription(final ITask jiraTask) {

		try {
			RetrieveTitleFromUrlJob job = new RetrieveTitleFromUrlJob(jiraTask.getUrl()) {

				@Override
				protected void setTitle(final String pageTitle) {
					jiraTask.setDescription(pageTitle);

					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							if (TaskListView.getDefault() != null)
								TaskListView.getDefault().refreshAndFocus();
						}
					});
				}

			};

			job.schedule();

		} catch (RuntimeException e) {
			MylarStatusHandler.fail(e, "could not open task web page", false);
		}
	}

	@Override
	public void synchronize(AbstractRepositoryQuery repositoryQuery) {
		if (repositoryQuery instanceof JiraFilter) {
			((JiraFilter) repositoryQuery).refreshHits();
		}
	}

	public void requestRefresh(AbstractRepositoryTask task) {
		// Task refresh not implemented.
	}

	@Override
	public boolean canCreateTaskFromId() {
		return false;
	}

	@Override
	public boolean canCreateNewTask() {
		return false;
	}

	@Override
	public IWizard getNewTaskWizard(TaskRepository taskRepository) {
		return null;
	} 
}
