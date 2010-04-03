/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.views;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.mylyn.internal.commons.ui.NotificationPopupColors;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonColors;
import org.eclipse.mylyn.internal.provisional.commons.ui.GradientCanvas;
import org.eclipse.mylyn.internal.provisional.commons.ui.SearchHistoryPopUpDialog;
import org.eclipse.mylyn.internal.tasks.ui.search.SearchUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

public class TaskListSearchHistoryPopupDialog extends SearchHistoryPopUpDialog {

	private static NotificationPopupColors colors;

	private LocalResourceManager resourceManager;

	public TaskListSearchHistoryPopupDialog(Shell parent, int side) {
		super(parent, side);
	}

	@Override
	protected void createAdditionalSearchRegion(Composite composite) {
		if (!SearchUtil.supportsTaskSearch()) {
			return;
		}

		resourceManager = new LocalResourceManager(JFaceResources.getResources());
		colors = new NotificationPopupColors(composite.getDisplay(), resourceManager);

		GradientCanvas gradient = new GradientCanvas(composite, SWT.NONE);

		gradient.setBackgroundGradient(new Color[] { colors.getGradientBegin(), colors.getGradientEnd() },
				new int[] { 100 }, true);

		GridLayout headLayout = new GridLayout();
		headLayout.marginHeight = 5;
		headLayout.marginWidth = 5;
		headLayout.horizontalSpacing = 0;
		headLayout.verticalSpacing = 0;
		gradient.setLayout(headLayout);
		gradient.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

		gradient.setSeparatorVisible(true);
		gradient.setSeparatorAlignment(SWT.TOP);

		Composite editContainer = new Composite(gradient, SWT.NONE);
		GridLayout editLayout = new GridLayout();
		editLayout.marginHeight = 0;
		editLayout.marginWidth = 0;
		editContainer.setLayout(editLayout);
		editContainer.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, true));

		ImageHyperlink advancedSearchButton = new ImageHyperlink(editContainer, SWT.NONE);
		advancedSearchButton.setUnderlined(true);
		advancedSearchButton.setForeground(CommonColors.HYPERLINK_WIDGET);
		advancedSearchButton.setText(TaskListFilteredTree.LABEL_SEARCH);
		advancedSearchButton.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				SearchUtil.openSearchDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			}
		});
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING).applyTo(advancedSearchButton);
	}
}
