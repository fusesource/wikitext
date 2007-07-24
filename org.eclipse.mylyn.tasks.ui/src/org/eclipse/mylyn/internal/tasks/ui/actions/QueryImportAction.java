/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.mylyn.internal.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

/**
 * Makes able to select an exported query file and import it back to the system.
 * @author Jevgeni Holodkov
 */
public class QueryImportAction extends Action implements IViewActionDelegate {

	protected ISelection selection;
	
	public void init(IViewPart view) {
		// ignore
	}

	public void run(IAction action) {
		run();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// ignore
	}

	
	public void run() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		FileDialog dialog = new FileDialog(shell);
		dialog.setFilterExtensions(new String[] { "*" + ITasksUiConstants.FILE_EXTENSION });
		
		String path = dialog.open();
		if (path != null) {
			File file = new File(path);
			if (file.isFile()) {
				List<AbstractRepositoryQuery> queries = TasksUiPlugin.getTaskListManager()
						.getTaskListWriter()
						.readQueries(file);
				
				if (queries.size() > 0) {
					List<AbstractRepositoryQuery> badQueries = TasksUiPlugin.getTaskListManager().insertQueries(queries);
					
					// notify user about importing
					String message = "The following queries were imported successfully: ";
					for(AbstractRepositoryQuery imported : queries) {
						if(!badQueries.contains(imported)) {
							message += "\n" + imported.getHandleIdentifier();
						}
					}
					
					if(badQueries.size() > 0) {
						message += "\n\n These queries were not imported, since their repository was not found: ";
						for (AbstractRepositoryQuery bad : badQueries) {
							message += "\n" + bad.getHandleIdentifier();
						}
					}
					
					MessageDialog.openInformation(shell, "Query Export Completed", message);
				} else {
					MessageDialog.openError(shell, "Query Export Error",
							"The specified file is not an exported query. Please, check that you have provided the correct file.");
					return;
				}
			}
		}
		 return;
	}

}
