/*******************************************************************************
 * Copyright (c) 2007 - 2007 CodeGear and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.internal.xplanner.ui.wizard;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.mylyn.internal.xplanner.core.XPlannerCorePlugin;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Wizard page that allows the user to select a query of tasks from the XPlanner server.
 * 
 * @author Ravi Kumar
 * @author Helen Bershadskaya
 */
public class XPlannerQuerySelectionWizardPage extends AbstractXPlannerQueryWizardPage {

	private static final String DESCRIPTION = Messages.XPlannerQuerySelectionWizardPage_SELECT_QUERY_TYPE;

	private Button buttonCustom;

	private XPlannerCustomQueryPage xplannerCustomQueryPage = null;

	public XPlannerQuerySelectionWizardPage(TaskRepository repository) {
		this(repository, null);
	}

	public XPlannerQuerySelectionWizardPage(TaskRepository repository, IRepositoryQuery existingQuery) {
		super(repository, existingQuery);
		setDescription(DESCRIPTION);
	}

	public void createControl(Composite parent) {
		final Composite innerComposite = new Composite(parent, SWT.NONE);
		innerComposite.setLayoutData(new GridData());
		GridLayout gl = new GridLayout();
		gl.numColumns = 2;
		innerComposite.setLayout(gl);

		buttonCustom = new Button(innerComposite, SWT.RADIO);
		buttonCustom.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		buttonCustom.setText(Messages.XPlannerQuerySelectionWizardPage_CREATE_QUERY_USING_FORM);
		buttonCustom.setSelection(true);

		setControl(innerComposite);
	}

	@Override
	public boolean canFlipToNextPage() {
		return xplannerCustomQueryPage != null;
	}

	@Override
	public IWizardPage getNextPage() {
		if (xplannerCustomQueryPage == null) {

			xplannerCustomQueryPage = new XPlannerCustomQueryPage(getRepository(), getExistingQuery());
			xplannerCustomQueryPage.setWizard(getWizard());
		}

		return xplannerCustomQueryPage;
	}

	@Override
	public IRepositoryQuery getQuery() {
		if (xplannerCustomQueryPage != null) {
			IRepositoryQuery query = xplannerCustomQueryPage.getQuery();
			if (query.getConnectorKind().equals(XPlannerCorePlugin.CONNECTOR_KIND)) {
				setExistingQuery(query);
			}
		}

		return getExistingQuery();
	}

	@Override
	protected String getHelpContextId() {
		return null;
	}

	@Override
	public String getQueryTitle() {
		return null;
	}

	@Override
	public void applyTo(IRepositoryQuery query) {
		throw new UnsupportedOperationException();
	}

}
