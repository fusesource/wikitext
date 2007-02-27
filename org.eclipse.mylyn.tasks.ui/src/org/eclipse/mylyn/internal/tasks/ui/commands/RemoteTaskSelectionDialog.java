/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.ui.commands;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.internal.tasks.ui.views.TaskElementLabelProvider;
import org.eclipse.mylar.internal.tasks.ui.views.TaskRepositoryLabelProvider;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.AbstractTaskContainer;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.TaskList;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.core.TaskRepositoryFilter;
import org.eclipse.mylar.tasks.core.TaskRepositoryManager;
import org.eclipse.mylar.tasks.ui.TaskCommandIds;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * @author Willian Mitsuda
 */
public class RemoteTaskSelectionDialog extends SelectionStatusDialog {

	public RemoteTaskSelectionDialog(Shell parent) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		setStatusLineAboveButtons(true);
	}

	private Text idText;

	private TableViewer tasksViewer;

	private ComboViewer repositoriesViewer;

	private Button addToTaskListCheck;

	private ComboViewer categoryViewer;

	// TODO: copy'n pasted code; make API?
	private List<TaskRepository> getTaskRepositories() {
		List<TaskRepository> repositories = new ArrayList<TaskRepository>();
		TaskRepositoryManager repositoryManager = TasksUiPlugin.getRepositoryManager();
		for (AbstractRepositoryConnector connector : repositoryManager.getRepositoryConnectors()) {
			Set<TaskRepository> connectorRepositories = repositoryManager
					.getRepositories(connector.getRepositoryType());
			for (TaskRepository repository : connectorRepositories) {
				if (TaskRepositoryFilter.CAN_CREATE_TASK_FROM_KEY.accept(repository, connector)) {
					repositories.add(repository);
				}
			}
		}
		return repositories;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);

		Label idLabel = new Label(area, SWT.NULL);
		idLabel.setText("Enter Key/&ID (use comma for multiple): ");
		idText = new Text(area, SWT.BORDER);
		idText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label matchingTasksLabel = new Label(area, SWT.NONE);
		matchingTasksLabel.setText("&Matching tasks:");
		tasksViewer = new TableViewer(area, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tasksViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(400, 200).create());
		tasksViewer.setLabelProvider(new DecoratingLabelProvider(new TaskElementLabelProvider(), PlatformUI
				.getWorkbench().getDecoratorManager().getLabelDecorator()));
		tasksViewer.setContentProvider(new ArrayContentProvider());
		tasksViewer.addFilter(new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (selectedIds == null) {
					return false;
				}

				// Only shows exact task matches
				if (!(element instanceof AbstractRepositoryTask)) {
					return false;
				}
				AbstractRepositoryTask task = (AbstractRepositoryTask) element;
				String taskId = task.getTaskKey();
				for (String id : selectedIds) {
					if (id.equals(taskId)) {
						return true;
					}
				}
				return false;
			}

		});
		tasksViewer.setInput(TasksUiPlugin.getTaskListManager().getTaskList().getAllTasks());
		idText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				computeIds();
				validate();
				tasksViewer.refresh(false);
			}

		});
		tasksViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}

		});
		tasksViewer.addOpenListener(new IOpenListener() {

			public void open(OpenEvent event) {
				if (getOkButton().getEnabled()) {
					okPressed();
				}
			}

		});
		Table table = tasksViewer.getTable();
		table.showSelection();

		Composite repositoriesComposite = new Composite(area, SWT.NONE);
		repositoriesComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		repositoriesComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).create());

		Label repositoriesLabel = new Label(repositoriesComposite, SWT.NONE);
		repositoriesLabel.setText("&Select a task repository:");

		repositoriesViewer = new ComboViewer(repositoriesComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
		repositoriesViewer.setLabelProvider(new TaskRepositoryLabelProvider());
		repositoriesViewer.setContentProvider(new ArrayContentProvider());
		repositoriesViewer.setInput(getTaskRepositories());
		TaskRepository currentRepository = getSelectedRepository();
		if (currentRepository != null) {
			repositoriesViewer.setSelection(new StructuredSelection(currentRepository), true);
		}
		repositoriesViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		repositoriesViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				tasksViewer.setSelection(StructuredSelection.EMPTY);
				validate();
			}

		});

		Button addRepositoryButton = new Button(repositoriesComposite, SWT.NONE);
		addRepositoryButton.setText("&Add...");
		addRepositoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IHandlerService hndSvc = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
				try {
					hndSvc.executeCommand(TaskCommandIds.ADD_TASK_REPOSITORY, null);
					repositoriesViewer.setInput(getTaskRepositories());
				} catch (CommandException ex) {
					MylarStatusHandler.fail(ex, ex.getMessage(), true);
				}
			}
		});

		Composite addToTaskListComposite = new Composite(area, SWT.NONE);
		addToTaskListComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());

		addToTaskListCheck = new Button(addToTaskListComposite, SWT.CHECK);
		addToTaskListCheck.setText("Add to Task &List category:");

		categoryViewer = new ComboViewer(addToTaskListComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
		categoryViewer.setContentProvider(new ArrayContentProvider());
		TaskList taskList = TasksUiPlugin.getTaskListManager().getTaskList();
		LinkedList<AbstractTaskContainer> categories = new LinkedList<AbstractTaskContainer>(taskList
				.getUserCategories());
		categories.addFirst(taskList.getRootCategory());
		categoryViewer.setInput(categories);
		categoryViewer.setLabelProvider(new LabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof AbstractTaskContainer) {
					return ((AbstractTaskContainer) element).getSummary();
				}
				return super.getText(element);
			}

		});
		categoryViewer.setSelection(new StructuredSelection(taskList.getRootCategory()));

		categoryViewer.getControl().setEnabled(addToTaskListCheck.getSelection());
		addToTaskListCheck.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				categoryViewer.getControl().setEnabled(addToTaskListCheck.getSelection());
			}

		});

		idText.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					tasksViewer.getControl().setFocus();
				}
			}

		});

		return area;
	}

	// TODO: the following is a copy-and-paste of SelectRepositoryPage class;
	// make API?
	private TaskRepository getSelectedRepository() {
		IStructuredSelection selection = getSelection();
		if (selection == null) {
			return (TaskRepository) tasksViewer.getElementAt(0);
		}

		Object element = selection.getFirstElement();
		if (element instanceof AbstractRepositoryQuery) {
			AbstractRepositoryQuery query = (AbstractRepositoryQuery) element;
			return getRepository(query.getRepositoryUrl(), query.getRepositoryKind());

		} else if (element instanceof AbstractQueryHit) {
			AbstractQueryHit queryHit = (AbstractQueryHit) element;
			if (queryHit.getParent() != null) {
				return getRepository(queryHit.getRepositoryUrl(), queryHit.getParent().getRepositoryKind());
			} else {
				return TasksUiPlugin.getRepositoryManager().getRepository(queryHit.getRepositoryUrl());
			}
		} else if (element instanceof AbstractRepositoryTask) {
			AbstractRepositoryTask task = (AbstractRepositoryTask) element;
			return getRepository(task.getRepositoryUrl(), task.getRepositoryKind());
		} else if (element instanceof IResource) {
			IResource resource = (IResource) element;
			return TasksUiPlugin.getDefault().getRepositoryForResource(resource, true);
		} else if (element instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) element;
			IResource resource = (IResource) adaptable.getAdapter(IResource.class);
			if (resource != null) {
				return TasksUiPlugin.getDefault().getRepositoryForResource(resource, true);
			} else {
				ITask task = (ITask) adaptable.getAdapter(ITask.class);
				if (task instanceof AbstractRepositoryTask) {
					AbstractRepositoryTask rtask = (AbstractRepositoryTask) task;
					return getRepository(rtask.getRepositoryUrl(), rtask.getRepositoryKind());
				}
			}
		}

		// TODO mapping between LogEntry.pliginId and repositories

		// TODO handle other selection types

		return null;
	}

	private IStructuredSelection getSelection() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ISelection selection = window.getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection) {
			return (IStructuredSelection) selection;
		}
		return null;
	}

	private TaskRepository getRepository(String repositoryUrl, String repositoryKind) {
		return TasksUiPlugin.getRepositoryManager().getRepository(repositoryKind, repositoryUrl);
	}

	private void validate() {
		if (idText.getText().trim().equals("")) {
			updateStatus(new Status(IStatus.ERROR, TasksUiPlugin.PLUGIN_ID, 0, "Enter a valid task ID", null));
			return;
		}
		if (tasksViewer.getSelection().isEmpty() && repositoriesViewer.getSelection().isEmpty()) {
			updateStatus(new Status(IStatus.ERROR, TasksUiPlugin.PLUGIN_ID, 0, "Select a task or repository", null));
			return;
		}
		updateStatus(new Status(IStatus.OK, TasksUiPlugin.PLUGIN_ID, 0, "", null));
	}

	private String[] selectedIds;

	private TaskRepository selectedRepository;

	private AbstractRepositoryTask selectedTask;

	private boolean shouldAddToTaskList;

	private AbstractTaskContainer selectedCategory;

	public String[] getSelectedIds() {
		return selectedIds;
	}

	public TaskRepository getSelectedTaskRepository() {
		return selectedRepository;
	}

	public AbstractRepositoryTask getSelectedTask() {
		return selectedTask;
	}

	public boolean shouldAddToTaskList() {
		return shouldAddToTaskList;
	}

	public AbstractTaskContainer getSelectedCategory() {
		return selectedCategory;
	}

	@Override
	protected void computeResult() {
		computeIds();

		ISelection taskSelection = tasksViewer.getSelection();
		if (!taskSelection.isEmpty()) {
			selectedTask = (AbstractRepositoryTask) ((IStructuredSelection) taskSelection).getFirstElement();
		} else {
			selectedRepository = (TaskRepository) ((IStructuredSelection) repositoriesViewer.getSelection())
					.getFirstElement();
		}
		shouldAddToTaskList = addToTaskListCheck.getSelection();
		if (shouldAddToTaskList) {
			selectedCategory = (AbstractTaskContainer) ((IStructuredSelection) categoryViewer.getSelection())
					.getFirstElement();
		}
	}

	private void computeIds() {
		selectedIds = idText.getText().split(",");
		for (String id : selectedIds) {
			id = id.trim();
		}
	}

}
