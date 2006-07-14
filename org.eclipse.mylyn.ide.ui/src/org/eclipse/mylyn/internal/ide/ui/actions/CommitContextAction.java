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

package org.eclipse.mylar.internal.ide.ui.actions;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.mylar.internal.ide.MylarIdePlugin;
import org.eclipse.mylar.internal.ide.team.TeamRespositoriesManager;
import org.eclipse.mylar.internal.tasks.ui.ui.views.TaskListView;
import org.eclipse.mylar.provisional.ide.team.TeamRepositoryProvider;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * @author Mik Kersten
 */
public class CommitContextAction implements IViewActionDelegate {

	public void init(IViewPart view) {
		// TODO Auto-generated method stub
	}

	public void run(IAction action) {
		ITask task = TaskListView.getFromActivePerspective().getSelectedTask();
		IResource[] resources = MylarIdePlugin.getDefault().getChangeSetManager().getResources(task);
		if (resources == null || resources.length == 0) {
			MessageDialog.openInformation(null, "Mylar Information",
					"There are no interesting resources in the corresponding change set.\nRefer to Synchronize view.");
			return;
		}

		List<TeamRepositoryProvider> providers = TeamRespositoriesManager.getInstance().getProviders();
		for (Iterator iter = providers.iterator(); iter.hasNext();) {
			try {
				TeamRepositoryProvider provider = (TeamRepositoryProvider) iter.next();
				if (provider.hasOutgoingChanges(resources)) {
					provider.commit(resources);
				}
			} catch (Exception e) {
			}
		}

	}

	public void selectionChanged(IAction action, ISelection selection) {
		// ignore
	}
}
