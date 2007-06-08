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

package org.eclipse.mylyn.internal.java.ui.junit;

import java.util.Set;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.mylyn.internal.java.JavaUiBridgePlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * @author Mik Kersten
 */
class TaskContextJUnitMainTab extends JUnitLaunchConfigurationTab {

	private static final String DESCRIPTION = "Subclasses of TestCase in the active task context automatically added to suite.";

	private boolean isPdeMode = false;

	public TaskContextJUnitMainTab(boolean isPdeMode) {
		this.isPdeMode = isPdeMode;
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		GridLayout topLayout = new GridLayout();
		topLayout.numColumns = 3;
		comp.setLayout(topLayout);

		new Label(comp, SWT.NONE);
		Label label = new Label(comp, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		label.setText(DESCRIPTION);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		if (!isPdeMode) {
			Set<IType> types = InteractionContextTestUtil.getTestCasesInContext();
			if (!types.isEmpty()) {
				IType firstType = types.iterator().next();
				String projectName = firstType.getJavaProject().getElementName();
				config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
			}
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		// ignore
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		// ignore
	}

	@Override
	public String getName() {
		return "JUnit";
	}

	@Override
	public Image getImage() {
		return JavaUiBridgePlugin.getImageDescriptor("icons/etool16/junit-tab.gif").createImage();
	}
}