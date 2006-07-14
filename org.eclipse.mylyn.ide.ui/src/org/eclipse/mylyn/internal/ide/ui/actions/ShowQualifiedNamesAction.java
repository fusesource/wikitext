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

package org.eclipse.mylar.internal.ide.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.mylar.context.ui.ContextUiPlugin;
import org.eclipse.mylar.internal.context.ui.MylarImages;
import org.eclipse.mylar.internal.ide.ui.views.ActiveSearchView;

/**
 * @author Mik Kersten
 */
public class ShowQualifiedNamesAction extends Action {

	public static final String LABEL = "Qualify Member Names";

	public static final String ID = "org.eclipse.mylar.ui.views.elements.qualify";

	private ActiveSearchView view;

	public ShowQualifiedNamesAction(ActiveSearchView view) {
		super(LABEL, Action.AS_CHECK_BOX);
		this.view = view;
		setId(ID);
		setText(LABEL);
		setToolTipText(LABEL);
		setImageDescriptor(MylarImages.QUALIFY_NAMES);
		update(ContextUiPlugin.getDefault().getPreferenceStore().getBoolean(ID));
	}

	public void update(boolean on) {
		view.setQualifiedNameMode(on);
		setChecked(on);
		ContextUiPlugin.getDefault().getPreferenceStore().setValue(ID, on);
	}

	@Override
	public void run() {
		update(!ContextUiPlugin.getDefault().getPreferenceStore().getBoolean(ID));
	}
}