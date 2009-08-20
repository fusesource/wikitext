/*******************************************************************************
 * Copyright (c) 2009 Hiroyuki Inaba and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Hiroyuki Inaba - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.provisional.commons.ui;

import java.lang.reflect.Field;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * A {@link FilteredTree} that uses the new look on Eclipse 3.5 and later.
 * 
 * @author Hiroyuki Inaba
 */
// TODO e3.5 remove this class and replace with FilteredTree
public class EnhancedFilteredTree extends FilteredTree {

	protected boolean useNewLook;

	public EnhancedFilteredTree(Composite parent, int treeStyle, PatternFilter filter, boolean useNewLook) {
		super(parent, treeStyle, filter);
	}

	public EnhancedFilteredTree(Composite parent, int treeStyle, PatternFilter filter) {
		super(parent, treeStyle, filter);
	}

	public EnhancedFilteredTree(Composite parent) {
		super(parent);
	}

	@Override
	protected void createControl(Composite parent, int treeStyle) {
		useNewLook = setNewLook(this);
		super.createControl(parent, treeStyle);
	}

	public static boolean setNewLook(FilteredTree tree) {
		try {
			Field newStyleField = FilteredTree.class.getDeclaredField("useNewLook"); //$NON-NLS-1$
			newStyleField.setAccessible(true);
			newStyleField.setBoolean(tree, true);
			return newStyleField.getBoolean(tree);
		} catch (Exception e) {
			// ignore
		}
		return false;
	}

}
