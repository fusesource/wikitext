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
package org.eclipse.mylar.internal.bugzilla.ui.wizard;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaQueryHit;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaReportElement;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaRepositoryQuery;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaTask;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.internal.bugzilla.ui.BugzillaUiPlugin;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.UIJob;

/**
 * @author Shawn Minto
 * @author Rob Elves
 * @author Mik Kersten
 * @author Eugene Kuleshov
 * @author Willian Mitsuda
 * 
 * Product selection page of new bug wizard
 */
public class BugzillaProductPage extends WizardPage {

	private static final String OPTION_ALL = "All";

	// A Map from Java's OS and Platform to Buzilla's
	private Map<String, String> java2buzillaOSMap = new HashMap<String, String>();

	private Map<String, String> java2buzillaPlatformMap = new HashMap<String, String>();

	private static final String NEW_BUGZILLA_TASK_ERROR_TITLE = "New Bugzilla Task Error";

	private static final String DESCRIPTION = "Pick a product to open the new bug editor.\n"
			+ "Press the Update button if the product is not in the list.";

	private static final String LABEL_UPDATE = "Update Products from Repository";

	/** The list of products to submit a bug report for */
	private List<String> products = null;

	/**
	 * Reference to the bug wizard which created this page so we can create the
	 * second page
	 */
	private NewBugzillaTaskWizard bugWizard;

	/** The instance of the workbench */
	protected IWorkbench workbench;

	/**
	 * Handle product selection
	 */
	private FilteredTree productList;

	private final TaskRepository repository;

	protected IPreferenceStore prefs = BugzillaUiPlugin.getDefault().getPreferenceStore();

	/**
	 * Constructor for BugzillaProductPage
	 * 
	 * @param workbench
	 *            The instance of the workbench
	 * @param bugWiz
	 *            The bug wizard which created this page
	 * @param repository
	 *            The repository the data is coming from
	 * @param selection
	 */
	public BugzillaProductPage(IWorkbench workbench, NewBugzillaTaskWizard bugWiz, TaskRepository repository) {
		super("Page1");
		setTitle(IBugzillaConstants.TITLE_NEW_BUG);
		setDescription(DESCRIPTION);
		this.workbench = workbench;
		this.bugWizard = bugWiz;
		this.repository = repository;
		setImageDescriptor(BugzillaUiPlugin.imageDescriptorFromPlugin("org.eclipse.mylar.bugzilla.ui",
				"icons/wizban/bug-wizard.gif"));

		java2buzillaPlatformMap.put("x86", "PC");
		java2buzillaPlatformMap.put("x86_64", "PC");
		java2buzillaPlatformMap.put("ia64", "PC");
		java2buzillaPlatformMap.put("ia64_32", "PC");
		java2buzillaPlatformMap.put("sparc", "Sun");
		java2buzillaPlatformMap.put("ppc", "Power");

		java2buzillaOSMap.put("aix", "AIX");
		java2buzillaOSMap.put("hpux", "HP-UX");
		java2buzillaOSMap.put("linux", "Linux");
		java2buzillaOSMap.put("macosx", "MacOS X");
		java2buzillaOSMap.put("qnx", "QNX-Photon");
		java2buzillaOSMap.put("solaris", "Solaris");
		java2buzillaOSMap.put("win32", "Windows XP");
	}

	public void createControl(Composite parent) {
		// create the composite to hold the widgets
		Composite composite = new Composite(parent, SWT.NULL);

		// create the desired layout for this wizard page
		composite.setLayout(new GridLayout());

		// create the list of bug reports
		productList = new FilteredTree(composite, SWT.SINGLE | SWT.BORDER, new PatternFilter());
		productList.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).hint(
				SWT.DEFAULT, 200).create());
		final TreeViewer productViewer = productList.getViewer();
		productViewer.setLabelProvider(new LabelProvider());
		productViewer.setContentProvider(new ITreeContentProvider() {

			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof Collection) {
					return ((Collection<?>) parentElement).toArray();
				}
				return null;
			}

			public Object getParent(Object element) {
				return null;
			}

			public boolean hasChildren(Object element) {
				return false;
			}

			public Object[] getElements(Object inputElement) {
				return getChildren(inputElement);
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		initProducts();
		productViewer.setInput(products);
		productViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				// Initialize a variable with the no error status
				Status status = new Status(IStatus.OK, BugzillaUiPlugin.PLUGIN_ID, 0, "", null);
				if (productViewer.getSelection().isEmpty()) {
					status = new Status(IStatus.ERROR, BugzillaUiPlugin.PLUGIN_ID, 0, "You must select a product", null);
				}

				// Show the most serious error
				applyToStatusLine(status);
				isPageComplete();
				getWizard().getContainer().updateButtons();
			}

		});

		// HACK: waiting on delayed refresh of filtered tree
		final String[] selectedProducts = getSelectedProducts();
		if (selectedProducts.length > 0) {
			new UIJob("") {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					productViewer.setSelection(new StructuredSelection(selectedProducts), true);
					productViewer.getControl().setFocus();
					return Status.OK_STATUS;
				}
			}.schedule(300L);
		} else {
			productList.setFocus();
		}

		Button updateButton = new Button(composite, SWT.LEFT | SWT.PUSH);
		updateButton.setText(LABEL_UPDATE);
		updateButton.setLayoutData(new GridData());

		updateButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					final AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager()
							.getRepositoryConnector(repository.getKind());

					getContainer().run(true, false, new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException,
								InterruptedException {
							monitor.beginTask("Updating repository report options...", IProgressMonitor.UNKNOWN);
							try {
								connector.updateAttributes(repository, monitor);
							} catch (CoreException ce) {
								if (ce.getStatus().getException() instanceof GeneralSecurityException) {
									MylarStatusHandler.fail(ce,
											"Bugzilla could not log you in to get the information you requested since login name or password is incorrect.\n"
													+ "Please ensure proper configuration in "
													+ TasksUiPlugin.LABEL_VIEW_REPOSITORIES + ". ", true);
								} else if (ce.getStatus().getException() instanceof IOException) {
									MylarStatusHandler.fail(ce,
											"Connection Error, please ensure proper configuration in "
													+ TasksUiPlugin.LABEL_VIEW_REPOSITORIES + ".", true);
								} else {
									MylarStatusHandler.fail(ce, "Error updating repository attributes for "
											+ repository.getUrl(), true);
								}
								return;
							}
							BugzillaUiPlugin.updateQueryOptions(repository, monitor);

							products = new ArrayList<String>();
							for (String product : BugzillaUiPlugin.getQueryOptions(IBugzillaConstants.VALUES_PRODUCT,
									null, repository.getUrl())) {
								products.add(product);
							}
						}
					});
					productViewer.setInput(products);
				} catch (InvocationTargetException ex) {
					MessageDialog.openError(null, "Error updating product list", "Error reported:\n"
							+ ex.getCause().getMessage());
				} catch (InterruptedException ex) {
					// Was cancelled...
				}
			}
		});

		// set the composite as the control for this page
		setControl(composite);

		isPageComplete();
		getWizard().getContainer().updateButtons();
	}

	private void initProducts() {
		// try to get the list of products from the server
		try {
			products = BugzillaCorePlugin.getRepositoryConfiguration(repository, false).getProducts();

		} catch (final CoreException e) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), NEW_BUGZILLA_TASK_ERROR_TITLE,
							"Unable to get products. Ensure proper repository configuration in "
									+ TasksUiPlugin.LABEL_VIEW_REPOSITORIES + ".\n\n");
					// MylarStatusHandler.log(e, "Failed to retrieve products
					// from server");
				}
			});
		}
	}

	private String[] getSelectedProducts() {
		IStructuredSelection selection = getSelection();
		if (selection == null) {
			return new String[0];
		}

		ArrayList<String> products = new ArrayList<String>();

		Object element = selection.getFirstElement();
		if (element instanceof BugzillaTask) {
			BugzillaTask bugzillaTask = (BugzillaTask) element;			
			if (bugzillaTask.getProduct() != null) {
				products.add(bugzillaTask.getProduct());
			}
		} else {
			BugzillaRepositoryQuery query = null;
			if (element instanceof BugzillaRepositoryQuery) {
				query = (BugzillaRepositoryQuery) element;

			} else if (element instanceof BugzillaQueryHit) {
				BugzillaQueryHit hit = (BugzillaQueryHit) element;
				if (hit.getParent() != null && hit.getParent() instanceof BugzillaRepositoryQuery) {
					query = (BugzillaRepositoryQuery) hit.getParent();
				}
			}

			if (query != null) {
				String queryUrl = query.getUrl();
				queryUrl = queryUrl.substring(queryUrl.indexOf("?") + 1);
				String[] options = queryUrl.split("&");

				for (String option : options) {
					String key = option.substring(0, option.indexOf("="));
					if ("product".equals(key)) {
						try {
							products.add(URLDecoder.decode(option.substring(option.indexOf("=") + 1), repository
									.getCharacterEncoding()));
							// TODO: list box only accepts a single selection so
							// we break on first found
							break;
						} catch (UnsupportedEncodingException ex) {
							// ignore
						}
					}
				}
			} else {
				if (element instanceof IAdaptable) {
					IAdaptable adaptable = (IAdaptable) element;
					ITask task = (ITask) adaptable.getAdapter(ITask.class);
					if (task instanceof BugzillaTask) {
						BugzillaTask bugzillaTask = (BugzillaTask) task;
						if (bugzillaTask.getProduct() != null) {
							products.add(bugzillaTask.getProduct());
						}
					}
				}
			}
		}

		return products.toArray(new String[products.size()]);
	}

	private IStructuredSelection getSelection() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ISelection selection = window.getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection) {
			return (IStructuredSelection) selection;
		}
		return null;
	}

	/**
	 * Applies the status to the status line of a dialog page.
	 * 
	 * @param status
	 *            The status to apply to the status line
	 */
	protected void applyToStatusLine(IStatus status) {
		String message = status.getMessage();
		if (message.length() == 0)
			message = null;
		switch (status.getSeverity()) {
		case IStatus.OK:
			setErrorMessage(null);
			setMessage(message);
			break;
		case IStatus.WARNING:
			setErrorMessage(null);
			setMessage(message, WizardPage.WARNING);
			break;
		case IStatus.INFO:
			setErrorMessage(null);
			setMessage(message, WizardPage.INFORMATION);
			break;
		default:
			setErrorMessage(null);
			setMessage(message, WizardPage.ERROR);
			break;
		}
	}

	/**
	 * Save the currently selected product to the taskData when next is clicked
	 */
	public void saveDataToModel() throws CoreException {
		RepositoryTaskData model = bugWizard.taskData;
		model.setAttributeValue(BugzillaReportElement.PRODUCT.getKeyString(),
				(String) ((IStructuredSelection) productList.getViewer().getSelection()).getFirstElement());
		BugzillaRepositoryConnector.setupNewBugAttributes(repository, model);
		setPlatformOptions(model);
	}

	@Override
	public boolean isPageComplete() {
		bugWizard.completed = !productList.getViewer().getSelection().isEmpty();
		return bugWizard.completed;
	}

	public void setPlatformOptions(RepositoryTaskData newBugModel) {
		try {

			// Get OS Lookup Map
			// Check that the result is in Values, if it is not, set it to other
			RepositoryTaskAttribute opSysAttribute = newBugModel.getAttribute(BugzillaReportElement.OP_SYS
					.getKeyString());
			RepositoryTaskAttribute platformAttribute = newBugModel.getAttribute(BugzillaReportElement.REP_PLATFORM
					.getKeyString());

			String OS = Platform.getOS();
			String platform = Platform.getOSArch();

			String bugzillaOS = null; // Bugzilla String for OS
			String bugzillaPlatform = null; // Bugzilla String for Platform

			if (java2buzillaOSMap != null && java2buzillaOSMap.containsKey(OS) && opSysAttribute != null
					&& opSysAttribute.getOptions() != null) {
				bugzillaOS = java2buzillaOSMap.get(OS);
				if (opSysAttribute != null && opSysAttribute.getOptionParameter(bugzillaOS) == null) {
					// If the OS we found is not in the list of available
					// options, set bugzillaOS
					// to null, and just use "other"
					bugzillaOS = null;
				}
			} else {
				// If we have a strangeOS, then just set buzillaOS to null, and
				// use "other"
				bugzillaOS = null;
			}

			if (platform != null && java2buzillaPlatformMap.containsKey(platform)) {
				bugzillaPlatform = java2buzillaPlatformMap.get(platform);
				if (platformAttribute != null && platformAttribute.getOptionParameter(bugzillaPlatform) == null) {
					// If the platform we found is not int the list of available
					// optinos, set the
					// Bugzilla Platform to null, and juse use "other"
					bugzillaPlatform = null;
				}
			} else {
				// If we have a strange platform, then just set bugzillaPatforrm
				// to null, and use "other"
				bugzillaPlatform = null;
			}

			// Set the OS and the Platform in the taskData
			if (bugzillaOS != null && opSysAttribute != null) {
				opSysAttribute.setValue(bugzillaOS);
			} else if (opSysAttribute != null && opSysAttribute.getOptionParameter(OPTION_ALL) != null) {
				opSysAttribute.setValue(OPTION_ALL);
			}

			if (bugzillaPlatform != null && platformAttribute != null) {
				platformAttribute.setValue(bugzillaPlatform);
			} else if (platformAttribute != null && platformAttribute.getOptionParameter(OPTION_ALL) != null) {
				opSysAttribute.setValue(OPTION_ALL);
			}

		} catch (Exception e) {
			MylarStatusHandler.fail(e, "could not set platform options", false);
		}
	}

	// @Override
	// public IWizardPage getNextPage() {
	// // save the product information to the taskData
	// saveDataToModel();
	// NewBugzillaTaskWizard wizard = (NewBugzillaTaskWizard) getWizard();
	// NewBugzillaTaskData taskData = wizard.model;
	//
	// // try to get the attributes from the bugzilla server
	// try {
	// if (!taskData.hasParsedAttributes() ||
	// !taskData.getProduct().equals(prevProduct)) {
	// BugzillaRepositoryUtil.setupNewBugAttributes(repository.getUrl(),
	// MylarTaskListPlugin.getDefault()
	// .getProxySettings(), repository.getUserName(), repository.getPassword(),
	// taskData, repository
	// .getCharacterEncoding());
	// taskData.setParsedAttributesStatus(true);
	// }
	//
	// // if (prevProduct == null) {
	// // bugWizard.setAttributePage(new WizardAttributesPage(workbench));
	// // bugWizard.addPage(bugWizard.getAttributePage());
	// // } else {
	// // // selected product has changed
	// // // will createControl again with new attributes in taskData
	// // bugWizard.getAttributePage().setControl(null);
	// // }
	//
	// } catch (final Exception e) {
	// e.printStackTrace();
	// PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
	// public void run() {
	// MessageDialog.openError(Display.getDefault().getActiveShell(),
	// NEW_BUGZILLA_TASK_ERROR_TITLE, e
	// .getLocalizedMessage()
	// + " Ensure proper repository configuration in " +
	// TaskRepositoriesView.NAME + ".");
	// }
	// });
	// // MylarStatusHandler.fail(e, e.getLocalizedMessage()+" Ensure
	// // proper repository configuration in
	// // "+TaskRepositoriesView.NAME+".", true);
	// // BugzillaPlugin.getDefault().logAndShowExceptionDetailsDialog(e,
	// // "occurred.", "Bugzilla Error");
	// }
	// return super.getNextPage();
	// }

}
