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

package org.eclipse.mylar.tasks.ui.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Rob Elves
 */
public class RepositoryTextViewer extends SourceViewer {

	private TaskRepository repository;

	public RepositoryTextViewer(IVerticalRuler vertRuler, IOverviewRuler overRuler, TaskRepository repository,
			Composite composite, int style) {
		super(composite, vertRuler, overRuler, true, style);

		this.repository = repository;

	}

	public RepositoryTextViewer(TaskRepository repository, Composite composite, int style) {// FormEditor
		super(composite, null, style);
		this.repository = repository;
	}

	@Override
	public void setDocument(IDocument doc) {
		if (doc != null && this.getAnnotationModel() != null) {
			this.getAnnotationModel().connect(doc);
			super.setDocument(doc, this.getAnnotationModel());
		} else {
			super.setDocument(doc);
		}
	}

	public TaskRepository getRepository() {
		return repository;
	}

	public void setRepository(TaskRepository repository) {
		this.repository = repository;
	}

}
