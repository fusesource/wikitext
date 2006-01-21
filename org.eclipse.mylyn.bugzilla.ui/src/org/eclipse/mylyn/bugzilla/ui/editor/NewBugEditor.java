/*******************************************************************************
 * Copyright (c) 2003 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylar.bugzilla.ui.editor;

import java.util.Iterator;

import javax.security.auth.login.LoginException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylar.bugzilla.core.Attribute;
import org.eclipse.mylar.bugzilla.core.BugReportPostHandler;
import org.eclipse.mylar.bugzilla.core.BugzillaException;
import org.eclipse.mylar.bugzilla.core.BugzillaPlugin;
import org.eclipse.mylar.bugzilla.core.IBugzillaBug;
import org.eclipse.mylar.bugzilla.core.NewBugModel;
import org.eclipse.mylar.bugzilla.core.PossibleBugzillaFailureException;
import org.eclipse.mylar.bugzilla.ui.OfflineView;
import org.eclipse.mylar.bugzilla.ui.WebBrowserDialog;
import org.eclipse.mylar.bugzilla.ui.actions.RefreshBugzillaReportsAction;
import org.eclipse.mylar.core.util.MylarStatusHandler;
import org.eclipse.mylar.tasklist.TaskRepository;
import org.eclipse.mylar.tasklist.ui.views.TaskListView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;


/**
 * An editor used to view a locally created bug that does not yet exist on a
 * server. It uses a <code>NewBugModel</code> object to store the data.
 */
public class NewBugEditor extends AbstractBugEditor {
	
	protected NewBugModel bug;
	protected Text descriptionText;
	protected String newSummary = "";
	protected String newDescription = "";
	
	/**
	 * Creates a new <code>NewBugEditor</code>.
	 */
	public NewBugEditor(TaskRepository repository) {
		super.repository = repository;
	}

	@Override
	public IBugzillaBug getBug() {
		return bug;
	}

	@Override
	protected void addKeywordsList(String keywords, Composite attributesComposite) {
		// Since NewBugModels have no keywords, there is no
		// GUI for them.
	}

	@Override
	protected void createDescriptionLayout() {
		
		// Description Area
		Composite descriptionComposite = new Composite(infoArea, SWT.NONE);
		GridLayout descriptionLayout = new GridLayout();
		descriptionLayout.numColumns = 4;
		descriptionComposite.setLayout(descriptionLayout);
		descriptionComposite.setBackground(background);
		GridData descriptionData = new GridData(GridData.FILL_BOTH);
		descriptionData.horizontalSpan = 1;
		descriptionData.grabExcessVerticalSpace = false;
		descriptionComposite.setLayoutData(descriptionData);
		//	End Description Area
		
		Composite descriptionTitleComposite =
			new Composite(descriptionComposite, SWT.NONE);
		GridLayout descriptionTitleLayout = new GridLayout();
		descriptionTitleLayout.horizontalSpacing = 0;
		descriptionTitleLayout.marginWidth = 0;
		descriptionTitleComposite.setLayout(descriptionTitleLayout);
		descriptionTitleComposite.setBackground(background);
		GridData descriptionTitleData =
			new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		descriptionTitleData.horizontalSpan = 4;
		descriptionTitleData.grabExcessVerticalSpace = false;
		descriptionTitleComposite.setLayoutData(descriptionTitleData);
		newLayout(descriptionTitleComposite, 4, "Description:", HEADER).addListener(SWT.FocusIn, new DescriptionListener());
		
		descriptionText = 
			new Text(descriptionComposite,
				SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		descriptionText.setFont(COMMENT_FONT);
		GridData descriptionTextData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		descriptionTextData.horizontalSpan = 4;
		descriptionTextData.widthHint = DESCRIPTION_WIDTH;
		descriptionTextData.heightHint = DESCRIPTION_HEIGHT;
		descriptionText.setLayoutData(descriptionTextData);
		descriptionText.setText(bug.getDescription());
		descriptionText.addListener(SWT.KeyUp, new Listener() {
			public void handleEvent(Event event) {
				String sel = descriptionText.getText() + event.character;
				if (!(newDescription.equals(sel))) {
					newDescription = sel;
					changeDirtyStatus(true);
				}
			}
		});
		descriptionText.addListener(SWT.FocusIn, new DescriptionListener());

        super.descriptionTextBox = descriptionText;
        
		this.createSeparatorSpace(descriptionComposite);
}

	@Override
	protected void createCommentLayout() {
		// Since NewBugModels have no comments, there is no
		// GUI for them.
	}

	@Override
	protected void addRadioButtons(Composite buttonComposite) {
		// Since NewBugModels have no special submitting actions,
		// no radio buttons are required.
	}

	@Override
	protected String getTitleString() {
		return bug.getLabel();
	}
	
	@Override
	protected void submitBug() {
		final BugReportPostHandler form = new BugReportPostHandler(); 
		form.setPrefix(BugReportPostHandler.FORM_PREFIX_BUG_218);
		form.setPrefix2(BugReportPostHandler.FORM_PREFIX_BUG_220);
		
		form.setPostfix(BugReportPostHandler.FORM_POSTFIX_216);
		form.setPostfix2(BugReportPostHandler.FORM_POSTFIX_218);
		updateBug();

		setURL(form, "post_bug.cgi");
		// go through all of the attributes and add them to the bug post
		Iterator<Attribute> itr = bug.getAttributes().iterator();
		while (itr.hasNext()) {
			Attribute a = itr.next();
			if (a != null && a.getParameterName() != null
					&& a.getParameterName().compareTo("") != 0
					&& !a.isHidden()) {
				String key = a.getName();
				String value = null;

				// get the values from the attribute
				if (key.equalsIgnoreCase("OS")) {
					value = a.getValue();
				} else if (key.equalsIgnoreCase("Version")) {
					value = a.getValue();
				} else if (key.equalsIgnoreCase("Severity")) {
					value = a.getValue();
				} else if (key.equalsIgnoreCase("Platform")) {
					value = a.getValue();
				} else if (key.equalsIgnoreCase("Component")) {
					value = a.getValue();
				} else if (key.equalsIgnoreCase("Priority")) {
					value = a.getValue();
				} else if (key.equalsIgnoreCase("URL")) {
					value = a.getValue();
				} else if (key.equalsIgnoreCase("Assign To") || key.equalsIgnoreCase("Assigned To")) {
					value = a.getValue();
				}

				// add the attribute to the bug post
				if (value == null)
					value = "";

				form.add(a.getParameterName(), value);
			} else if (a != null && a.getParameterName() != null
					&& a.getParameterName().compareTo("") != 0
					&& a.isHidden()) {
				// we have a hidden attribute, add it to the posting
				form.add(a.getParameterName(), a.getValue());

			}

		}

		// set the summary, and description

		// add the summary to the bug post
		form.add("short_desc", bug.getSummary());

		// format the description of the bug so that it is roughly in 80
		// character lines
		bug.setDescription(formatText(bug.getDescription()));

		if (bug.getDescription().length() != 0) {
			// add the new comment to the bug post if there is some text in
			// it
			form.add("comment", bug.getDescription());
		}

		
		final WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			protected void execute(final IProgressMonitor monitor) throws CoreException {
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable(){
					public void run() {
//						 update the bug on the server
						try {
							String id = form.post();

							// If the bug was successfully sent...
							if (id != null && NewBugEditor.this != null && !NewBugEditor.this.isDisposed()) {
								changeDirtyStatus(false);
								BugzillaPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(NewBugEditor.this, false);
							}
							OfflineView.removeReport(bug);
						} catch (BugzillaException e) {
							MessageDialog
									.openError(
											null,
											"I/O Error",
											"Bugzilla could not post your bug.");
							BugzillaPlugin.log(e);
						} catch (PossibleBugzillaFailureException e) {
							WebBrowserDialog
							.openAcceptAgreement(
									null,
									"Possible Bugzilla Client Failure",
									"Bugzilla may not have posted your bug.\n" + e.getMessage(), form.getError());
							BugzillaPlugin.log(e);
						}catch (LoginException e) {
							e.printStackTrace();
							// if we had an error with logging in, display an error
							MessageDialog.openError(
											null,
											"Posting Error",
											"Bugzilla could not post your bug since your login name or password is incorrect."
													+ "\nPlease check your settings in the bugzilla preferences. ");
						}
					}
				});
			}
		};
		
		Job job = new Job("Submitting New Bug"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try{
					op.run(monitor);
				} catch (Exception e){
					MylarStatusHandler.log(e, "Failed to submit bug");
					return new Status(Status.ERROR, "org.eclipse.mylar.bugzilla.ui", Status.ERROR, "Failed to submit bug", e);
				}
				
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable(){
					public void run() {
						if(TaskListView.getDefault() != null && 
								TaskListView.getDefault().getViewer() != null){
							new RefreshBugzillaReportsAction().run();
						}
					}
				});
				return Status.OK_STATUS;
			}
			
		};
		
		job.schedule();
	}

	@Override
	protected void updateBug() {
		// go through all of the attributes and update the main values to the new ones
		for (Iterator<Attribute> it = bug.getAttributes().iterator(); it.hasNext(); ) {
			Attribute a = it.next();
			a.setValue(a.getNewValue());
		}
		
		// Update some other fields as well.
		bug.setSummary(newSummary);
		bug.setDescription(newDescription);
	}

	@Override
	protected void restoreBug() {
		// go through all of the attributes and restore the new values to the main ones
		for (Iterator<Attribute> it = bug.getAttributes().iterator(); it.hasNext(); ) {
			Attribute a = it.next();
			a.setNewValue(a.getValue());
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof NewBugEditorInput))
			throw new PartInitException("Invalid Input: Must be NewBugEditorInput");
		NewBugEditorInput ei = (NewBugEditorInput) input;
		setSite(site);
		setInput(input);
		bugzillaInput = ei;
		model = BugzillaOutlineNode.parseBugReport(bugzillaInput.getBug());
		bug = ei.getBug();
		newSummary = bug.getSummary();
		newDescription = bug.getDescription();
		restoreBug();
		isDirty = false;
		updateEditorTitle();
	}

	/**
	 * A listener for selection of the description textbox.
	 */
	protected class DescriptionListener implements Listener {
		public void handleEvent(Event event) {
			fireSelectionChanged(new SelectionChangedEvent(selectionProvider, new StructuredSelection(new BugzillaReportSelection(bug.getId(), bug.getRepository(), "New Description", false, bug.getSummary()))));
		}
	}
	
	@Override
	public void handleSummaryEvent() {
		String sel = summaryText.getText();
		if (!(newSummary.equals(sel))) {
			newSummary = sel;
			changeDirtyStatus(true);
		}
	}

	@Override
	protected void addCCList(String value, Composite attributesComposite) {
		// do nothing here right now
	}
	
}
