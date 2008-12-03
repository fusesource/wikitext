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

package org.eclipse.mylyn.internal.java.ui.search;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.mylyn.internal.java.ui.JavaStructureBridge;

/**
 * @author Mik Kersten
 */
public class JavaWriteAccessProvider extends AbstractJavaRelationProvider {

	public static final String ID = ID_GENERIC + ".writeaccess"; //$NON-NLS-1$

	public static final String NAME = "written by"; //$NON-NLS-1$

	public JavaWriteAccessProvider() {
		super(JavaStructureBridge.CONTENT_TYPE, ID);
	}

	@Override
	protected boolean acceptElement(IJavaElement javaElement) {
		return javaElement instanceof IField;
	}

	@Override
	protected String getSourceId() {
		return ID;
	}

	@Override
	public String getName() {
		return NAME;
	}
}
