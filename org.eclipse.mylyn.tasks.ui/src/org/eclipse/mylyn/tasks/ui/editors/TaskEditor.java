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

package org.eclipse.mylar.tasks.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylar.internal.tasks.ui.editors.TaskEditorActionContributor;
import org.eclipse.mylar.internal.tasks.ui.editors.TaskPlanningEditor;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.TasksUiUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.editor.SharedHeaderFormEditor;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * @author Mik Kersten
 * @author Eric Booth (initial prototype)
 * @author Rob Elves
 */
public class TaskEditor extends SharedHeaderFormEditor {

	private static final String ISSUE_WEB_PAGE_LABEL = "Browser";

	protected ITask task;

	private TaskPlanningEditor taskPlanningEditor;

	private Browser webBrowser;

	private TaskEditorInput taskEditorInput;

	private TaskEditorListener partListener;

	private List<IEditorPart> editors = new ArrayList<IEditorPart>();

	private Menu contextMenu;

	private IEditorPart contentOutlineProvider = null;

	private int browserPageIndex = -1;

	public final Object FAMILY_SUBMIT = new Object();

	public TaskEditor() {
		super();
		taskPlanningEditor = new TaskPlanningEditor(this);
		taskPlanningEditor.setParentEditor(this);
	}

	protected void contextMenuAboutToShow(IMenuManager manager) {
		TaskEditorActionContributor contributor = getContributor();
		// IFormPage page = getActivePageInstance();
		if (contributor != null)
			contributor.contextMenuAboutToShow(manager);
	}

	public TaskEditorActionContributor getContributor() {
		return (TaskEditorActionContributor) getEditorSite().getActionBarContributor();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		return getAdapterDelgate(adapter);
	}

	public Object getAdapterDelgate(Class<?> adapter) {
		// TODO: consider adding: IContentOutlinePage.class.equals(adapter) &&
		if (contentOutlineProvider != null) {
			return contentOutlineProvider.getAdapter(adapter);
		} else {
			return super.getAdapter(adapter);
		}
	}

	@Override
	public IEditorPart getActiveEditor() {
		return super.getActiveEditor();
	}

	private int createBrowserPage(final String url) {
		if (!TasksUiPlugin.getDefault().getPreferenceStore().getBoolean(
				TaskListPreferenceConstants.REPORT_DISABLE_INTERNAL)) {
			try {
				webBrowser = new Browser(getContainer(), SWT.NONE);
				int index = addPage(webBrowser);
				setPageText(index, ISSUE_WEB_PAGE_LABEL);

				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

					public void run() {
						webBrowser.setUrl(url);
					}
				});

				boolean openWithBrowser = TasksUiPlugin.getDefault().getPreferenceStore().getBoolean(
						TaskListPreferenceConstants.REPORT_OPEN_INTERNAL);

				if (openWithBrowser) {
					setActivePage(index);
				}
				return index;
			} catch (SWTError e) {
				MylarStatusHandler.fail(e, "Could not create Browser page: " + e.getMessage(), true);
			} catch (RuntimeException e) {
				MylarStatusHandler.fail(e, "could not create issue report page", false);
			}
		}
		return 0;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		for (IFormPage page : getPages()) {
			if (page.isDirty()) {
				page.doSave(monitor);
			}
		}

		editorDirtyStateChanged();
	}

	// see PDEFormEditor
	/* package */@SuppressWarnings("unchecked")
	IFormPage[] getPages() {
		ArrayList formPages = new ArrayList();
		for (int i = 0; i < pages.size(); i++) {
			Object page = pages.get(i);
			if (page instanceof IFormPage)
				formPages.add(page);
		}
		return (IFormPage[]) formPages.toArray(new IFormPage[formPages.size()]);
	}

	/**
	 * Refresh editor with new contents (if any)
	 */
	public void refreshEditorContents() {
		for (IFormPage page : getPages()) {
			if (page instanceof AbstractRepositoryTaskEditor) {
				AbstractRepositoryTaskEditor editor = (AbstractRepositoryTaskEditor) page;
				editor.refreshEditor();
			}
		}
		// if (webBrowser != null) {
		// PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
		//
		// public void run() {
		// refresh to original url?
		// webBrowser.refresh();
		// }
		// });
		// }
	}

	/**
	 * HACK: perform real check
	 */
	private String getUrl() {
		String url = null;
		if (getEditorInput() instanceof RepositoryTaskEditorInput) {
			url = ((RepositoryTaskEditorInput) getEditorInput()).getUrl();
			if (url == null) {
				url = task.getTaskUrl();
			}
		} else if (task != null && task.getTaskUrl().length() > 9) {
			url = task.getTaskUrl();
		}
		return url;
	}

	/**
	 * Saves the multi-page editor's document as another file. Also updates the
	 * text for page 0's tab, and updates this multi-page editor's input to
	 * correspond to the nested editor's.
	 * 
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		IEditorPart editor = getEditor(0);
		if (editor != null) {
			editor.doSaveAs();
			setPageText(0, editor.getTitle());
			setInput(editor.getEditorInput());
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		partListener = new TaskEditorListener();
		site.getPage().addPartListener(partListener);
		super.init(site, input);
		setSite(site);
	}

	public void notifyTaskChanged() {
		if (task != null) {
			TasksUiPlugin.getTaskListManager().getTaskList().notifyLocalInfoChanged(task);
		}
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public boolean isDirty() {
		for (IFormPage page : getPages()) {
			if (page.isDirty()) {
				return true;
			}
		}
		return false;
	}

	private class TaskEditorListener implements IPartListener {

		public void partActivated(IWorkbenchPart part) {
			if (part.equals(TaskEditor.this)) {
				if (taskEditorInput != null) {
					ITask task = taskEditorInput.getTask();
					if (TaskListView.getFromActivePerspective() != null) {
						ITask selected = TaskListView.getFromActivePerspective().getSelectedTask();
						if (selected == null || !selected.equals(task)) {
							TaskListView.getFromActivePerspective().selectedAndFocusTask(task);
						}
					}
				}
			}
		}

		/**
		 * @see org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart)
		 */
		public void partBroughtToTop(IWorkbenchPart part) {
			// don't care about this event
		}

		/**
		 * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
		 */
		public void partClosed(IWorkbenchPart part) {
			// don't care about this event
		}

		/**
		 * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
		 */
		public void partDeactivated(IWorkbenchPart part) {
			// don't care about this event
		}

		/**
		 * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
		 */
		public void partOpened(IWorkbenchPart part) {
			// don't care about this event
		}
	}

	/**
	 * Updates the tab title
	 */
	public void changeTitle() {
		this.setPartName(taskEditorInput.getLabel());
	}

	public void markDirty() {
		firePropertyChange(PROP_DIRTY);
		return;
	}

	// @Override
	// public void setFocus() {
	// if (this.getActivePage() > -1 && this.getActivePage() !=
	// browserPageIndex) {
	// IFormPage page = this.getPages()[this.getActivePage()];
	// if (page != null) {
	// page.setFocus();
	// }
	// } else if(this.getActivePage() == browserPageIndex && webBrowser != null)
	// {
	// webBrowser.setFocus();
	// }
	// }

	public void setFocusOfActivePage() {
		if (this.getActivePage() > -1 && this.getActivePage() != browserPageIndex) {
			IFormPage page = this.getPages()[this.getActivePage()];
			if (page != null) {
				page.setFocus();
			}
		} else if (this.getActivePage() == browserPageIndex && webBrowser != null) {
			webBrowser.setFocus();
		}
	}

	public Browser getWebBrowser() {
		return webBrowser;
	}

	public void revealBrowser() {
		setActivePage(browserPageIndex);
	}

	public void displayInBrowser(String url) {
		if (webBrowser != null) {
			webBrowser.setUrl(url);
			revealBrowser();
		} else {
			TasksUiUtil.openBrowser(url);
		}
	}

	@Override
	protected void pageChange(int newPageIndex) {
		// for (ITaskEditorFactory factory :
		// TasksUiPlugin.getDefault().getTaskEditorFactories()) {
		// for (IEditorPart editor : editors) {
		// factory.notifyEditorActivationChange(editor);
		// }
		// }
		super.pageChange(newPageIndex);
	}

	@Override
	public void dispose() {
		for (IEditorPart part : editors) {
			part.dispose();
		}
		if (taskPlanningEditor != null)
			taskPlanningEditor.dispose();
		if (webBrowser != null) {
			webBrowser.dispose();
		}

		IWorkbench workbench = TasksUiPlugin.getDefault().getWorkbench();
		if (workbench != null && partListener != null) {
			for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
				IWorkbenchPage activePage = window.getActivePage();
				if (activePage != null) {
					activePage.removePartListener(partListener);
				}
			}
		}
		super.dispose();
	}

	public TaskEditorInput getTaskEditorInput() {
		return taskEditorInput;
	}

	@Override
	protected void addPages() {
		try {
			MenuManager manager = new MenuManager();
			IMenuListener listener = new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					contextMenuAboutToShow(manager);
				}
			};
			manager.setRemoveAllWhenShown(true);
			manager.addMenuListener(listener);
			contextMenu = manager.createContextMenu(getContainer());
			getContainer().setMenu(contextMenu);
			int index = -1;
			if (getEditorInput() instanceof TaskEditorInput) {
				addPage(taskPlanningEditor);
				index++;
				taskEditorInput = (TaskEditorInput) getEditorInput();
				task = taskEditorInput.getTask();
				setPartName(taskEditorInput.getLabel());
				setPageImage(0, TasksUiImages.getImage(TasksUiImages.CALENDAR_SMALL));
			}

			int selectedIndex = index;
			for (ITaskEditorFactory factory : TasksUiPlugin.getDefault().getTaskEditorFactories()) {
				if (factory.canCreateEditorFor(task) || factory.canCreateEditorFor(getEditorInput())) {
					try {
						IEditorPart editor = factory.createEditor(this, getEditorInput());
						IEditorInput input = task != null ? factory.createEditorInput(task) : getEditorInput();
						if (editor != null && input != null) {
							FormPage taskEditor = (FormPage) editor;
							editor.init(getEditorSite(), input);
							index = addPage(taskEditor);
							if (input.getImageDescriptor() != null) {
								setPageImage(index, TasksUiImages.getImage(input.getImageDescriptor()));
							}
							if (editor instanceof AbstractRepositoryTaskEditor) {

								((AbstractRepositoryTaskEditor) editor).setParentEditor(this);

								if (getEditorInput() instanceof RepositoryTaskEditorInput) {
									RepositoryTaskEditorInput existingInput = (RepositoryTaskEditorInput) getEditorInput();
									setPartName(existingInput.getName());
								} else if (getEditorInput() instanceof NewTaskEditorInput) {
									String label = ((NewTaskEditorInput) getEditorInput()).getName();
									setPartName(label);
								}
								setPageText(index, factory.getTitle());
								selectedIndex = index;
							}
						}

						// HACK: overwrites if multiple present
						if (factory.providesOutline()) {
							contentOutlineProvider = editor;
						}
					} catch (Exception e) {
						MylarStatusHandler.fail(e, "Could not create editor via factory: " + factory, true);
					}
				}
			}
			String urlToOpen = getUrl();
			if (urlToOpen != null && !urlToOpen.equals("")) {
				browserPageIndex = createBrowserPage(urlToOpen);
				setPageImage(browserPageIndex, TasksUiImages.getImage(TasksUiImages.BROWSER_SMALL));
				if (selectedIndex == 0 && taskEditorInput != null && !taskEditorInput.isNewTask()) {
					selectedIndex = browserPageIndex;
				}
			}

			if (selectedIndex != -1) {
				setActivePage(selectedIndex);
			}

			if (task instanceof AbstractRepositoryTask) {
				setTitleImage(TasksUiImages.getImage(TasksUiImages.TASK_REPOSITORY));
			} else if (getEditorInput() instanceof AbstractTaskEditorInput) {
				this.setTitleImage(TasksUiImages.getImage(TasksUiImages.TASK_REMOTE));
			} else if (getUrl() != null) {
				setTitleImage(TasksUiImages.getImage(TasksUiImages.TASK_WEB));
			}

		} catch (PartInitException e) {
			MylarStatusHandler.fail(e, "failed to create task editor pages", false);
		}
	}

	@Override
	public void setFocus() {
		if (getActivePageInstance() instanceof AbstractNewRepositoryTaskEditor) {
			getActivePageInstance().setFocus();
		} else {
			super.setFocus();
		}
	}

	/**
	 * Update the title of the editor
	 */
	public void updateTitle(String name) {
		// setContentDescription(name);
		setPartName(name);
		setTitleToolTip(name);
		updateFormTitle();
	}

	public void showBusy(boolean busy) {
		if (!this.getHeaderForm().getForm().isDisposed()) {
			this.getHeaderForm().getForm().setBusy(busy);
		}
	}

	public ISelection getSelection() {
		if (getSite() != null && getSite().getSelectionProvider() != null) {
			return getSite().getSelectionProvider().getSelection();
		} else {
			return StructuredSelection.EMPTY;
		}
	}

	@Override
	protected void createHeaderContents(IManagedForm headerForm) {
		getToolkit().decorateFormHeading(headerForm.getForm().getForm());
		headerForm.getForm().setImage(TasksUiImages.getImage(TasksUiImages.TASK));
		updateFormTitle();
	}
	
	protected void updateFormTitle() {
		IEditorInput input = getEditorInput();
		if (input instanceof TaskEditorInput) {
			ITask task = ((TaskEditorInput) input).getTask();
			if (task instanceof AbstractRepositoryTask) {
				setFormHeaderImage(((AbstractRepositoryTask) task).getRepositoryKind());
				setFormHeaderLabel((AbstractRepositoryTask) task);
				return;
			} else {
				getHeaderForm().getForm().setText("Task: " + task.getSummary());
			}
		} else if (input instanceof RepositoryTaskEditorInput) {
			ITask task = ((RepositoryTaskEditorInput) input).getRepositoryTask();
			if (task != null && task instanceof AbstractRepositoryTask) {
				setFormHeaderImage(((AbstractRepositoryTask) task).getRepositoryKind());
				setFormHeaderLabel((AbstractRepositoryTask) task);
				return;
			} else {
				RepositoryTaskData data = ((RepositoryTaskEditorInput) input).getTaskData();
				if (data != null) {
					setFormHeaderImage(data.getRepositoryKind());
					setFormHeaderLabel(data);
				}
			}
		}
	}

	private void setFormHeaderImage(String repositoryKind) {
		ImageDescriptor overlay = TasksUiPlugin.getDefault().getOverlayIcon(repositoryKind);
		ImageDescriptor imageDescriptor = TasksUiImages.createWithOverlay(TasksUiImages.REPOSITORY, overlay, false,
				false);
		getHeaderForm().getForm().setImage(TasksUiImages.getImage(imageDescriptor));
	}

	public Form getTopForm() {
		return this.getHeaderForm().getForm().getForm();
	}

	public void setMessage(String message, int type) {
		this.getHeaderForm().getForm().setMessage(message, type);
	}

	protected IWorkbenchSiteProgressService getProgressService() {
		Object siteService = getEditorSite().getAdapter(IWorkbenchSiteProgressService.class);
		if (siteService != null)
			return (IWorkbenchSiteProgressService) siteService;
		return null;
	}

	private void setFormHeaderLabel(RepositoryTaskData taskData) {

		String kindLabel = taskData.getTaskKind();
		String idLabel = taskData.getId();
		
		if(taskData.isNew()) {
			AbstractRepositoryConnectorUi connectorUi = TasksUiPlugin.getRepositoryUi(taskData.getRepositoryKind());
			if (connectorUi != null) {
				kindLabel = "New " + connectorUi.getTaskKindLabel(null);
			} else {
				kindLabel = "New Task";
			}
			idLabel = "";
		}

		if (idLabel != null) {
			getHeaderForm().getForm().setText(kindLabel + " " + idLabel);
		} else {
			getHeaderForm().getForm().setText(kindLabel);
		}
	}

	private void setFormHeaderLabel(AbstractRepositoryTask repositoryTask) {

		AbstractRepositoryConnectorUi connectorUi = TasksUiPlugin.getRepositoryUi(repositoryTask.getRepositoryKind());
		String kindLabel = "";
		if (connectorUi != null) {
			kindLabel = connectorUi.getTaskKindLabel(repositoryTask);
		}

		String idLabel = repositoryTask.getTaskKey();

		if (idLabel != null) {
			getHeaderForm().getForm().setText(kindLabel + " " + idLabel);
		} else {
			getHeaderForm().getForm().setText(kindLabel);
		}
	}

}
