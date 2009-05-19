/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.provisional.commons.ui;

import java.lang.reflect.Field;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.mylyn.internal.commons.ui.CommonsUiPlugin;
import org.eclipse.mylyn.internal.commons.ui.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * @author Mik Kersten
 */
public abstract class AbstractFilteredTree extends EnhancedFilteredTree {

	private static final int filterWidth = 69;

	public static final String LABEL_FIND = Messages.AbstractFilteredTree_Find;

	private Job refreshJob;

	private AdaptiveRefreshPolicy refreshPolicy;

	private Composite progressComposite;

	private Composite searchComposite;

	private boolean showProgress = false;

	/**
	 * XXX: using reflection to gain access
	 * 
	 * @param parent
	 * @param treeStyle
	 * @param filter
	 */
	public AbstractFilteredTree(Composite parent, int treeStyle, PatternFilter filter) {
		super(parent, treeStyle, filter);
		try {
			// TODO e3.4 override doCreateRefreshJob() instead
			Field refreshField = FilteredTree.class.getDeclaredField("refreshJob"); //$NON-NLS-1$
			refreshField.setAccessible(true);
			refreshJob = (Job) refreshField.get(this);
			refreshPolicy = new AdaptiveRefreshPolicy(refreshJob);
		} catch (Exception e) {
			CommonsUiPlugin.getDefault().getLog().log(
					new Status(IStatus.ERROR, CommonsUiPlugin.ID_PLUGIN, "Could not get refresh job", e)); //$NON-NLS-1$
		}
		setInitialText(""); //$NON-NLS-1$
	}

	@Override
	protected void createControl(Composite parent, int treeStyle) {
		super.createControl(parent, treeStyle);

		// Override superclass layout settings...
		GridLayout layout = (GridLayout) getLayout();
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
	}

	@Override
	protected Control createTreeControl(Composite parent, int style) {
		progressComposite = createProgressComposite(parent);
//		progressComposite.setVisible(false);
//		((GridData) progressComposite.getLayoutData()).exclude = true;

		searchComposite = createSearchComposite(parent);
		if (searchComposite != null) {
			searchComposite.setVisible(false);
			((GridData) searchComposite.getLayoutData()).exclude = true;
		}

		return super.createTreeControl(parent, style);
	}

	@Override
	protected Composite createFilterControls(Composite parent) {
		Composite newParent = parent;

		GridLayout gridLayout = new GridLayout(4, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginTop = 3;
		gridLayout.marginBottom = 3;
		gridLayout.verticalSpacing = 0;
		if (useNewLook) {
			newParent = new Composite(parent.getParent(), SWT.NONE);
			gridLayout.numColumns = 3;
			newParent.setLayout(gridLayout);

			newParent.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER));

			parent.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER));
			gridLayout = new GridLayout(3, false);
			gridLayout.marginWidth = 0;
			gridLayout.marginHeight = 0;
			gridLayout.horizontalSpacing = 0;
			gridLayout.verticalSpacing = 0;
		}
		parent.setLayout(gridLayout);

		if (useNewLook) {
			Label label = new Label(newParent, SWT.NONE);
			label.setText(LABEL_FIND);
			parent.setParent(newParent);
		} else {
			Label label = new Label(parent, SWT.NONE);
			label.setText(LABEL_FIND);
		}

//		// from super
//		createFilterText(parent);
//		createClearText(parent);
//		if (filterToolBar != null) {
//			filterToolBar.update(false);
//			// initially there is no text to clear
//			filterToolBar.getControl().setVisible(false);
//		}
		super.createFilterControls(parent);

		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.minimumWidth = filterWidth;
		filterText.setLayoutData(gd);
		filterText.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.ESC) {
					setFilterText(""); //$NON-NLS-1$
				}
			}
		});

		Composite superComposite;
		if (useNewLook) {
			superComposite = new Composite(newParent, SWT.NONE);
		} else {
			superComposite = new Composite(parent, SWT.NONE);
		}
		GridLayout superLayout = new GridLayout(4, false);
		superLayout.marginWidth = 0;
		superLayout.marginHeight = 0;
		superLayout.horizontalSpacing = 0;
		superLayout.verticalSpacing = 0;
		GridData superLayoutData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		superComposite.setLayout(superLayout);
		superComposite.setLayoutData(superLayoutData);

		Composite workingSetComposite = createActiveWorkingSetComposite(superComposite);
		workingSetComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		Composite activeTaskComposite = createActiveTaskComposite(superComposite);
		activeTaskComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		parent.layout();
		return parent;
	}

	private void createClearText(Composite parent) {
		// only create the button if the text widget doesn't support one
		// natively
		if ((filterText.getStyle() & SWT.CANCEL) == 0) {
			filterToolBar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
			filterToolBar.createControl(parent);

			IAction clearTextAction = new Action("", IAction.AS_PUSH_BUTTON) {//$NON-NLS-1$
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.jface.action.Action#run()
				 */
				@Override
				public void run() {
					clearText();
				}
			};

			clearTextAction.setToolTipText(Messages.AbstractFilteredTree_Clear);
			clearTextAction.setImageDescriptor(CommonImages.FIND_CLEAR);
			clearTextAction.setDisabledImageDescriptor(CommonImages.FIND_CLEAR_DISABLED);
			filterToolBar.add(clearTextAction);
		}
	}

	protected abstract Composite createProgressComposite(Composite container);

	protected abstract Composite createActiveWorkingSetComposite(Composite container);

	protected abstract Composite createActiveTaskComposite(Composite container);

	protected Composite createSearchComposite(Composite container) {
		return null;
	}

	@Override
	protected void textChanged() {
		// this call allows the filtered tree to preserve the selection when the clear button is used.
		// It is necessary to correctly set the private narrowingDown flag in the super class. 
		// Note that the scheduling of the refresh job that is done in the super class will be overridden 
		// by the call to refreshPolicy.textChanged().
		super.textChanged();

		if (refreshPolicy != null) {
			refreshPolicy.textChanged(getFilterString());
		}
		// bug 165353 work-around for premature return at FilteredTree.java:374
		updateToolbar(true);
	}

	@Deprecated
	protected Job getRefreshJob() {
		return refreshJob;
	}

	public AdaptiveRefreshPolicy getRefreshPolicy() {
		return refreshPolicy;
	}

	public boolean isShowProgress() {
		return showProgress;
	}

	public void setShowProgress(boolean showProgress) {
		this.showProgress = showProgress;
		progressComposite.setVisible(showProgress);
		((GridData) progressComposite.getLayoutData()).exclude = !showProgress;
		getParent().getParent().layout(true, true);
	}

	public void setShowSearch(boolean showSearch) {
		if (searchComposite != null) {
			searchComposite.setVisible(showSearch);
			((GridData) searchComposite.getLayoutData()).exclude = !showSearch;
			getParent().getParent().layout(true, true);
		}
	}
}
