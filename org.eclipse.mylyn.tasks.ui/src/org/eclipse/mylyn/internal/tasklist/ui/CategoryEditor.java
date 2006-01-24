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

package org.eclipse.mylar.internal.tasklist.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylar.tasklist.MylarTaskListPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.EditorPart;

/**
 * @author Ken Sueda
 */
public class CategoryEditor extends EditorPart {

	private CategoryEditorInput input = null;

	private boolean isDirty = false;

	private Text description = null;

	@Override
	public void doSave(IProgressMonitor monitor) {
		input.setCategoryName(description.getText());
		isDirty = false;
		MylarTaskListPlugin.getTaskListManager().notifyListUpdated();
//		if (TaskListView.getDefault() != null)
//			TaskListView.getDefault().notifyTaskDataChanged(null);
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {
	}

	@SuppressWarnings("deprecation")
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		this.input = (CategoryEditorInput) input;
		setPartName(input.getName());
		setTitleToolTip(input.getToolTipText());
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		ScrolledForm sform = toolkit.createScrolledForm(parent);
		sform.getBody().setLayout(new TableWrapLayout());
		Composite editorComposite = sform.getBody();

		createSummarySection(editorComposite, toolkit);
	}

	@Override
	public void setFocus() {
	}

	private void createSummarySection(Composite parent, FormToolkit toolkit) {
		Section summarySection = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		summarySection.setText("Category Summary");
		summarySection.setLayout(new TableWrapLayout());
		summarySection.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		Composite summaryContainer = toolkit.createComposite(summarySection);
		summarySection.setClient(summaryContainer);
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		summaryContainer.setLayout(layout);

		Label label = toolkit.createLabel(summaryContainer, "Description: ", SWT.NULL);
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		description = toolkit.createText(summaryContainer, input.getCategoryName(), SWT.BORDER);
		description.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		description.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				markDirty();
			}
		});
	}

	private void markDirty() {
		isDirty = true;
		firePropertyChange(PROP_DIRTY);
	}
}
