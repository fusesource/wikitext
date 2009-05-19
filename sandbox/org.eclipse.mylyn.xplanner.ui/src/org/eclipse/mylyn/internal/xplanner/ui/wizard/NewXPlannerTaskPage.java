/*******************************************************************************
 * Copyright (c) 2007 - 2007 CodeGear and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.internal.xplanner.ui.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.provisional.commons.ui.EnhancedFilteredTree;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.internal.xplanner.core.service.XPlannerClient;
import org.eclipse.mylyn.internal.xplanner.ui.XPlannerClientFacade;
import org.eclipse.mylyn.internal.xplanner.ui.XPlannerUiPlugin;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.xplanner.soap.UserStoryData;

/**
 * Wizard page for web-based new XPlanner task wizard
 * 
 * @author Ravi Kumar
 * @author Helen Bershadskaya
 */
@SuppressWarnings("restriction")
public class NewXPlannerTaskPage extends WizardPage {

	private static final String DESCRIPTION = Messages.NewXPlannerTaskPage_PICK_USER_STORY
			+ Messages.NewXPlannerTaskPage_PRESS_UPDATE_BUTTON;

	private static final String LABEL_UPDATE = Messages.NewXPlannerTaskPage_UPDATE_FROM_REPOSITORY;

	private FilteredTree projectTree;

	private XPlannerClient client;

	private final TaskRepository repository;

	public NewXPlannerTaskPage(TaskRepository repository) {
		super("XPlannerUserStory"); //$NON-NLS-1$
		this.repository = repository;

		setTitle(Messages.NewXPlannerTaskPage_NEW_XPLANNER_TASK);
		setDescription(DESCRIPTION);

		try {
			this.client = XPlannerClientFacade.getDefault().getXPlannerClient(repository);
			setPageComplete(false);
		} catch (CoreException ce) {
			TasksUiInternal.displayStatus(Messages.NewXPlannerTaskPage_REPOSITORY_ERROR, ce.getStatus());
		}
	}

	public void createControl(Composite parent) {
		// create the composite to hold the widgets
		Composite composite = new Composite(parent, SWT.NULL);

		// create the desired layout for this wizard page
		composite.setLayout(new GridLayout());

		// create the list of bug reports
		// TODO e3.5 move to new FilteredTree API
		projectTree = new EnhancedFilteredTree(composite, SWT.SINGLE | SWT.BORDER, new PatternFilter());
		projectTree.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).hint(
				SWT.DEFAULT, 200).create());

		TreeViewer projectsViewer = projectTree.getViewer();
		projectsViewer.setContentProvider(new ProjectsViewerContentProvider(client));
		projectsViewer.setLabelProvider(new ProjectsViewerLabelProvider());
		GridData projectsViewerGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		projectsViewerGridData.heightHint = 100;
		projectsViewerGridData.widthHint = 200;

		projectsViewer.getTree().setLayoutData(projectsViewerGridData);
		projectsViewer.setInput(client);
		projectsViewer.refresh();
		projectsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				validatePage(true);
			}

		});
		projectsViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				if (getWizard().canFinish()) {
					if (getWizard().performFinish()) {
						((WizardDialog) getContainer()).close();
					}
				}
			}
		});

		updateProjectsFromRepository(false);

		Button updateButton = new Button(composite, SWT.LEFT | SWT.PUSH);
		updateButton.setText(LABEL_UPDATE);
		updateButton.setLayoutData(new GridData());

		updateButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				updateProjectsFromRepository(true);
			}
		});

		// set the composite as the control for this page
		setControl(composite);

		isPageComplete();
		getWizard().getContainer().updateButtons();
	}

	public void validatePage(boolean updatePageComplete) {
		String errorMessage = null;

		// need user story to be selected
		if (getSelectedUserStory() == null) {
			errorMessage = Messages.NewXPlannerTaskPage_USER_STORY_NEEDS_TO_BE_SELECTED;
		}

		setErrorMessage(errorMessage);
		if (updatePageComplete) {
			setPageComplete(errorMessage == null);
		}
	}

	@Override
	public boolean isPageComplete() {
		validatePage(false);

		return getErrorMessage() == null;
	}

	private void updateProjectsFromRepository(final boolean force) {
		if (force) { //!client.hasDetails() || force) {
			try {
				final AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
						repository.getConnectorKind());

				getContainer().run(true, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask(Messages.NewXPlannerTaskPage_UPDATING_REPOSITORY, IProgressMonitor.UNKNOWN);
						try {
							connector.updateRepositoryConfiguration(repository, monitor);
						} catch (Exception e) {
							String msg = NLS.bind( //
									Messages.NewXPlannerTaskPage_ERROR_UPDATING_ATTRIBUTES
											+ Messages.NewXPlannerTaskPage_CHECK_REPOSITORY_SETTINGS, //
									e.getMessage());
							showWarning(msg);
							StatusHandler.log(new Status(IStatus.ERROR, XPlannerUiPlugin.ID_PLUGIN, msg, e));
						} finally {
							monitor.done();
						}
					}

					private void showWarning(final String msg) {
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								NewXPlannerTaskPage.this.setErrorMessage(msg);
							}
						});
					}
				});
			} catch (Exception e) {
				return;
			}
		}

		projectTree.getViewer().setInput(client);
	}

	public UserStoryData getSelectedUserStory() {
		UserStoryData selectedUserStory = null;

		IStructuredSelection selection = (IStructuredSelection) projectTree.getViewer().getSelection();
		if (!selection.isEmpty() && selection.getFirstElement() instanceof UserStoryData) {
			selectedUserStory = (UserStoryData) selection.getFirstElement();
		}

		return selectedUserStory;
	}
}
