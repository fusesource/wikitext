/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.trac.ui.editor;

import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.editors.AbstractRenderingEngine;
import org.eclipse.mylyn.tasks.ui.editors.AbstractRepositoryTaskEditor;
import org.eclipse.ui.forms.editor.FormEditor;

/**
 * @author Steffen Pingel
 * @author Xiaoyang Guan (Wiki HTML preview)
 */
public class TracTaskEditor extends AbstractRepositoryTaskEditor {

	private TracRenderingEngine renderingEngine = new TracRenderingEngine();

	public TracTaskEditor(FormEditor editor) {
		super(editor);
	}

	@Override
	protected void validateInput() {
	}

	@Override
	protected AbstractRenderingEngine getRenderingEngine() {
		return renderingEngine;
	}

	public TaskRepository getRepository() {
		return repository;
	}

}
