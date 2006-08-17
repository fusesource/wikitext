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

package org.eclipse.mylar.internal.tasks.ui.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.TaskCategory;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.actions.ActionFactory;

/**
 * @author Mik Kersten
 * @author Rob Elves
 */
public class SynchronizeSelectedAction extends ActionDelegate implements IViewActionDelegate {

	private Map<AbstractRepositoryConnector, List<AbstractRepositoryQuery>> queriesToSyncMap = new LinkedHashMap<AbstractRepositoryConnector, List<AbstractRepositoryQuery>>();

	private Map<AbstractRepositoryConnector, List<AbstractRepositoryTask>> tasksToSyncMap = new LinkedHashMap<AbstractRepositoryConnector, List<AbstractRepositoryTask>>();

	// private void checkSyncResult(final IJobChangeEvent event, final
	// AbstractRepositoryQuery problemQuery) {
	// if (event.getResult().getException() != null) {
	// PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
	// public void run() {
	// MessageDialog.openError(Display.getDefault().getActiveShell(),
	// TasksUiPlugin.TITLE_DIALOG, event
	// .getResult().getMessage());
	// }
	// });
	// }
	// }

	public void run(IAction action) {

		if (TaskListView.getFromActivePerspective() != null) {

			ISelection selection = TaskListView.getFromActivePerspective().getViewer().getSelection();
			for (Object obj : ((IStructuredSelection) selection).toList()) {
				if (obj instanceof AbstractRepositoryQuery) {
					final AbstractRepositoryQuery repositoryQuery = (AbstractRepositoryQuery) obj;
					AbstractRepositoryConnector client = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
							repositoryQuery.getRepositoryKind());
					if (client != null) {
						List<AbstractRepositoryQuery> queriesToSync = queriesToSyncMap.get(client);
						if (queriesToSync == null) {
							queriesToSync = new ArrayList<AbstractRepositoryQuery>();
							queriesToSyncMap.put(client, queriesToSync);
						}
						queriesToSync.add(repositoryQuery);
					}
				} else if (obj instanceof TaskCategory) {
					TaskCategory cat = (TaskCategory) obj;
					for (ITask task : cat.getChildren()) {
						if (task instanceof AbstractRepositoryTask) {
							AbstractRepositoryConnector client = TasksUiPlugin.getRepositoryManager()
									.getRepositoryConnector(((AbstractRepositoryTask) task).getRepositoryKind());
							addTaskToSync(client, (AbstractRepositoryTask) task);
						}
					}
				} else if (obj instanceof AbstractRepositoryTask) {
					AbstractRepositoryTask repositoryTask = (AbstractRepositoryTask) obj;
					AbstractRepositoryConnector client = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
							repositoryTask.getRepositoryKind());
					addTaskToSync(client, repositoryTask);
				} else if (obj instanceof AbstractQueryHit) {
					AbstractQueryHit hit = (AbstractQueryHit) obj;
					if (hit.getOrCreateCorrespondingTask() != null) {
						AbstractRepositoryConnector client = TasksUiPlugin.getRepositoryManager()
								.getRepositoryConnector(hit.getCorrespondingTask().getRepositoryKind());
						addTaskToSync(client, hit.getCorrespondingTask());
					}
				}
			}

			if (!queriesToSyncMap.isEmpty()) {
				for (AbstractRepositoryConnector connector : queriesToSyncMap.keySet()) {
					List<AbstractRepositoryQuery> queriesToSync = queriesToSyncMap.get(connector);
					for (AbstractRepositoryQuery query : queriesToSync) {
						query.setCurrentlySynchronizing(true);
					}
				}
			}
			if (!tasksToSyncMap.isEmpty()) {
				for (AbstractRepositoryConnector connector : tasksToSyncMap.keySet()) {
					List<AbstractRepositoryTask> tasksToSync = tasksToSyncMap.get(connector);
					for (AbstractRepositoryTask task : tasksToSync) {
						task.setCurrentlySynchronizing(true);
					}
				}
			}
			if (!queriesToSyncMap.isEmpty()) {
				for (AbstractRepositoryConnector connector : queriesToSyncMap.keySet()) {
					List<AbstractRepositoryQuery> queriesToSync = queriesToSyncMap.get(connector);
					if (queriesToSync != null && queriesToSync.size() > 0) {
						connector.synchronize(new HashSet<AbstractRepositoryQuery>(queriesToSync), null, Job.LONG, 0,
								true);
					}
				}
			}
			if (!tasksToSyncMap.isEmpty()) {
				for (AbstractRepositoryConnector connector : tasksToSyncMap.keySet()) {
					List<AbstractRepositoryTask> tasksToSync = tasksToSyncMap.get(connector);
					if (tasksToSync != null && tasksToSync.size() > 0) {
						connector.synchronize(new HashSet<AbstractRepositoryTask>(tasksToSync), true, null);
					}
				}
			}

		}

		queriesToSyncMap.clear();
		tasksToSyncMap.clear();
		
		if (TaskListView.getFromActivePerspective() != null) {
			TaskListView.getFromActivePerspective().getViewer().refresh();
		}		

	}

	private void addTaskToSync(AbstractRepositoryConnector client, AbstractRepositoryTask repositoryTask) {
		if (client != null) {
			List<AbstractRepositoryTask> tasksToSync = tasksToSyncMap.get(client);
			if (tasksToSync == null) {
				tasksToSync = new ArrayList<AbstractRepositoryTask>();
				tasksToSyncMap.put(client, tasksToSync);
			}
			tasksToSync.add(repositoryTask);
		}
	}

	private IAction action;

	@Override
	public void init(IAction action) {
		this.action = action;
	}

	public void init(IViewPart view) {
		IActionBars actionBars = view.getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), action);
		actionBars.updateActionBars();
	}

}
