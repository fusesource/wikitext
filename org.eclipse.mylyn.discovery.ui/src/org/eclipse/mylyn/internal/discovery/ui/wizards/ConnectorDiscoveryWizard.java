/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.internal.discovery.ui.wizards;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.internal.discovery.core.model.ConnectorDescriptorKind;
import org.eclipse.mylyn.internal.discovery.ui.DiscoveryUi;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonsUiUtil;
import org.eclipse.mylyn.internal.provisional.commons.ui.ICoreRunnable;

import com.ibm.icu.text.MessageFormat;

/**
 * A wizard for performing discovery of connectors and selecting connectors to install. When finish is pressed, selected
 * connectors are downloaded and installed.
 * 
 * @see InstallConnectorsJob
 * @see ConnectorDiscoveryWizardMainPage
 * 
 * @author David Green
 */
public class ConnectorDiscoveryWizard extends Wizard {

	private ConnectorDiscoveryWizardMainPage mainPage;

	private final Map<ConnectorDescriptorKind, Boolean> connectorDescriptorKindToVisibility = new HashMap<ConnectorDescriptorKind, Boolean>();
	{
		for (ConnectorDescriptorKind kind : ConnectorDescriptorKind.values()) {
			connectorDescriptorKindToVisibility.put(kind, true);
		}
	}

	private boolean showConnectorDescriptorKindFilter = true;

	private boolean showConnectorDescriptorTextFilter = true;

	public ConnectorDiscoveryWizard() {
		setWindowTitle("Connector Discovery");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(mainPage = new ConnectorDiscoveryWizardMainPage());
	}

	@Override
	public boolean performFinish() {
		try {
			ICoreRunnable job = new InstallConnectorsJob(mainPage.getInstallableConnectors());
			if (getContainer() != null) {
				CommonsUiUtil.run(getContainer(), job);
			} else {
				CommonsUiUtil.busyCursorWhile(job);
			}
		} catch (CoreException e) {
			IStatus status = new Status(IStatus.ERROR, DiscoveryUi.BUNDLE_ID, MessageFormat.format(
					"Problems occurred while performing installation: {0}", new Object[] { e.getMessage() }), e); //$NON-NLS-1$
			DiscoveryUi.logAndDisplayStatus("Cannot complete installation", status);
		} catch (OperationCanceledException e) {
			// canceled
		}
		return true;
	}

	/**
	 * configure the page to show or hide connector descriptors of the given kind
	 * 
	 * @see #connectorDescriptorKindVisibilityUpdated()
	 */
	public void setVisibility(ConnectorDescriptorKind kind, boolean visible) {
		if (kind == null) {
			throw new IllegalArgumentException();
		}
		connectorDescriptorKindToVisibility.put(kind, visible);
	}

	/**
	 * indicate if the given kind of connector is currently visible in the wizard
	 * 
	 * @see #setVisibility(ConnectorDescriptorKind, boolean)
	 */
	public boolean isVisible(ConnectorDescriptorKind kind) {
		if (kind == null) {
			throw new IllegalArgumentException();
		}
		return connectorDescriptorKindToVisibility.get(kind);
	}

	/**
	 * indicate if the connector descriptor filters should be shown in the UI. Changing this setting only has an effect
	 * before the UI is presented.
	 */
	public boolean isShowConnectorDescriptorKindFilter() {
		return showConnectorDescriptorKindFilter;
	}

	/**
	 * indicate if the connector descriptor filters should be shown in the UI. Changing this setting only has an effect
	 * before the UI is presented.
	 */
	public void setShowConnectorDescriptorKindFilter(boolean showConnectorDescriptorKindFilter) {
		this.showConnectorDescriptorKindFilter = showConnectorDescriptorKindFilter;
	}

	/**
	 * indicate if a text field should be provided to allow the user to filter connector descriptors
	 */
	public boolean isShowConnectorDescriptorTextFilter() {
		return showConnectorDescriptorTextFilter;
	}

	/**
	 * indicate if a text field should be provided to allow the user to filter connector descriptors
	 */
	public void setShowConnectorDescriptorTextFilter(boolean showConnectorDescriptorTextFilter) {
		this.showConnectorDescriptorTextFilter = showConnectorDescriptorTextFilter;
	}

}
