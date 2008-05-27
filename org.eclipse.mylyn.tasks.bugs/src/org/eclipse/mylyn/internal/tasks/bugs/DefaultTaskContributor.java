/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.bugs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.internal.tasks.bugs.wizards.FeatureStatus;
import org.eclipse.mylyn.tasks.bugs.AbstractTaskContributor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.osgi.framework.Bundle;

/**
 * @author Steffen Pingel
 */
public class DefaultTaskContributor extends AbstractTaskContributor {

	@Override
	public Map<String, String> getAttributes(IStatus status) {
		Map<String, String> attributes = new HashMap<String, String>();
		if (status instanceof FeatureStatus) {
			StringBuilder sb = new StringBuilder();
			sb.append("\n\n\n");
			sb.append("-- Installed Plug-ins --\n");
			IBundleGroup bundleGroup = ((FeatureStatus) status).getBundleGroup();

			sb.append(bundleGroup.getIdentifier());
			sb.append(" ");
			sb.append(bundleGroup.getVersion());

			Bundle[] bundles = bundleGroup.getBundles();
			if (bundles != null) {
				for (Bundle bundle : bundles) {
					sb.append(bundle.getBundleId());
				}
			}
			attributes.put(IRepositoryConstants.DESCRIPTION, sb.toString());

		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("\n\n-- Error Details --\n");
			if (status.getException() != null) {
				sb.append("\nStack Trace:\n");
				StringWriter writer = new StringWriter();
				status.getException().printStackTrace(new PrintWriter(writer));
				sb.append(writer.getBuffer());
			}
			attributes.put(IRepositoryConstants.DESCRIPTION, sb.toString());
		}
		return attributes;
	}

	@Override
	public String getEditorId(IStatus status) {
		return TaskEditor.ID_EDITOR;
	}

}
