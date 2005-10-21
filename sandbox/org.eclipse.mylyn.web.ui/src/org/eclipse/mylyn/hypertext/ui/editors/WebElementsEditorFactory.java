/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.hypertext.ui.editors;

import org.eclipse.mylar.core.IMylarContext;
import org.eclipse.mylar.tasklist.IContextEditorFactory;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.EditorPart;

public class WebElementsEditorFactory implements IContextEditorFactory {

	public EditorPart createEditor() {
		return new WebElementsEditor();
	}

	public IEditorInput createEditorInput(IMylarContext context) {
		return new WebElementsEditorInput(context);
	}
}
