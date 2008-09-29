/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raphael Ackermann - initial API and implementation
 *     Tasktop Technologies - improvements
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.editors;

import java.util.Iterator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonThemes;
import org.eclipse.mylyn.internal.tasks.ui.editors.RepositoryTextViewerConfiguration.Mode;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.ColumnSpan;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.RowSpan;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.themes.IThemeManager;

/**
 * FIXME comment is out dated
 * 
 * Text viewer generally used for displaying non-editable text. No annotation model or spell checking support. Supports
 * cut/copy/paste/etc..
 * 
 * For viewing and editing text. Spell checking w/ annotations supported One or two max per editor, any more and the
 * spell checker will bring the editor to a grinding halt.
 * 
 * @author Raphael Ackermann (bug 195514)
 * @author Steffen Pingel
 */
public class RichTextAttributeEditor extends AbstractAttributeEditor {

	private RepositoryTextViewer viewer;

	private boolean spellCheckingEnabled;

	private final int style;

	private final TaskRepository taskRepository;

	private Mode mode;

	public RichTextAttributeEditor(TaskDataModel manager, TaskRepository taskRepository, TaskAttribute taskAttribute) {
		this(manager, taskRepository, taskAttribute, SWT.MULTI);
	}

	public RichTextAttributeEditor(TaskDataModel manager, TaskRepository taskRepository, TaskAttribute taskAttribute,
			int style) {
		super(manager, taskAttribute);
		this.taskRepository = taskRepository;
		this.style = style;
		if ((style & SWT.MULTI) != 0) {
			setLayoutHint(new LayoutHint(RowSpan.MULTIPLE, ColumnSpan.MULTIPLE));
		} else {
			setLayoutHint(new LayoutHint(RowSpan.SINGLE, ColumnSpan.MULTIPLE));
		}
		setMode(Mode.DEFAULT);
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		Assert.isNotNull(mode);
		this.mode = mode;
	}

	private void configureAsTextEditor(Document document) {
		AnnotationModel annotationModel = new AnnotationModel();
		viewer.showAnnotations(false);
		viewer.showAnnotationsOverview(false);
		IAnnotationAccess annotationAccess = new DefaultMarkerAnnotationAccess();
		final SourceViewerDecorationSupport support = new SourceViewerDecorationSupport(viewer, null, annotationAccess,
				EditorsUI.getSharedTextColors());
		@SuppressWarnings("unchecked")
		Iterator e = new MarkerAnnotationPreferences().getAnnotationPreferences().iterator();
		while (e.hasNext()) {
			support.setAnnotationPreference((AnnotationPreference) e.next());
		}
		support.install(EditorsUI.getPreferenceStore());
		viewer.getTextWidget().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				support.uninstall();
			}
		});
		viewer.getTextWidget().setIndent(2);
		viewer.setDocument(document, annotationModel);
	}

	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		int style = this.style;
		if (!isReadOnly() && (style & TasksUiInternal.SWT_NO_SCROLL) == 0) {
			style |= SWT.V_SCROLL;
		}
		viewer = new RepositoryTextViewer(taskRepository, parent, SWT.FLAT | SWT.WRAP | style);

		// NOTE: configuration must be applied before the document is set in order for
		// hyper link coloring to work, the Presenter requires the document object up front
		RepositoryTextViewerConfiguration viewerConfig = new RepositoryTextViewerConfiguration(taskRepository,
				spellCheckingEnabled);
		viewerConfig.setMode(getMode());
		viewer.configure(viewerConfig);

		Document document = new Document(getValue());
		if (isReadOnly()) {
			viewer.setEditable(false);
			viewer.setDocument(document);
		} else {
			viewer.setEditable(true);
			configureAsTextEditor(document);
			installListeners(viewer);
			viewer.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		}

		TaskHyperlinkTextPresentationManager hyperlinkTextPresentationManager = new TaskHyperlinkTextPresentationManager();
		if (mode == Mode.TASK_RELATION) {
			hyperlinkTextPresentationManager.setHyperlinkDetector(new TaskRelationHyperlinkDetector());
		} else {
			hyperlinkTextPresentationManager.setHyperlinkDetector(new TaskHyperlinkDetector());
		}
		hyperlinkTextPresentationManager.install(viewer);

		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		Font font = themeManager.getCurrentTheme().getFontRegistry().get(CommonThemes.FONT_EDITOR_COMMENT);
		viewer.getTextWidget().setFont(font);
		toolkit.adapt(viewer.getTextWidget(), false, false);

		setControl(viewer.getTextWidget());
	}

	private void installListeners(RepositoryTextViewer viewer2) {
		viewer.addTextListener(new ITextListener() {
			public void textChanged(TextEvent event) {
				// filter out events caused by text presentation changes, e.g. annotation drawing
				String value = viewer.getTextWidget().getText();
				if (!getValue().equals(value)) {
					setValue(value);
					EditorUtil.ensureVisible(viewer.getTextWidget());
				}
			}
		});
		// ensure that tab traverses to next control instead of inserting a tab character unless editing multi-line text
		if ((style & SWT.MULTI) != 0 && mode != Mode.DEFAULT) {
			viewer.getTextWidget().addListener(SWT.Traverse, new Listener() {
				public void handleEvent(Event event) {
					switch (event.detail) {
					case SWT.TRAVERSE_TAB_NEXT:
					case SWT.TRAVERSE_TAB_PREVIOUS:
						event.doit = true;
						break;
					}
				}
			});
		}
	}

	public String getValue() {
		return getAttributeMapper().getValue(getTaskAttribute());
	}

	public SourceViewer getViewer() {
		return viewer;
	}

	public boolean isSpellCheckingEnabled() {
		return spellCheckingEnabled;
	}

	public void setSpellCheckingEnabled(boolean spellCheckingEnabled) {
		this.spellCheckingEnabled = spellCheckingEnabled;
	}

	public void setValue(String value) {
		getAttributeMapper().setValue(getTaskAttribute(), value);
		attributeChanged();
	}

}
