/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.editors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonFormUtil;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.editors.RichTextEditor.State;
import org.eclipse.mylyn.internal.tasks.ui.editors.RichTextEditor.StateChangedEvent;
import org.eclipse.mylyn.internal.tasks.ui.editors.RichTextEditor.StateChangedListener;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * @author Steffen Pingel
 */
public class TaskEditorRichTextPart extends AbstractTaskEditorPart {

	private RichTextAttributeEditor editor;

	private TaskAttribute attribute;

	private Composite composite;

	private int sectionStyle;

	private ToggleToMaximizePartAction toggleToMaximizePartAction;

	private Action toggleEditAction;

	private Action toggleBrowserAction;

	private boolean ignoreToggleEvents;

	public TaskEditorRichTextPart() {
		setSectionStyle(ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
	}

	public void appendText(String text) {
		if (editor == null) {
			return;
		}

		editor.showEditor();
		if (toggleEditAction != null) {
			toggleEditAction.setChecked(false);
		}

		StringBuilder strBuilder = new StringBuilder();
		String oldText = editor.getViewer().getDocument().get();
		if (strBuilder.length() != 0) {
			strBuilder.append("\n"); //$NON-NLS-1$
		}
		strBuilder.append(oldText);
		strBuilder.append(text);
		editor.getViewer().getDocument().set(strBuilder.toString());
		TaskAttribute attribute = getTaskData().getRoot().getMappedAttribute(TaskAttribute.COMMENT_NEW);
		if (attribute != null) {
			attribute.setValue(strBuilder.toString());
			getTaskEditorPage().getModel().attributeChanged(attribute);
		}
		editor.getViewer().getTextWidget().setCaretOffset(strBuilder.length());
		editor.getViewer().getTextWidget().showSelection();
	}

	public int getSectionStyle() {
		return sectionStyle;
	}

	public void setSectionStyle(int sectionStyle) {
		this.sectionStyle = sectionStyle;
	}

	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		if (attribute == null) {
			return;
		}
		AbstractAttributeEditor attributEditor = createAttributeEditor(attribute);
		if (!(attributEditor instanceof RichTextAttributeEditor)) {
			String clazz;
			if (attributEditor != null) {
				clazz = attributEditor.getClass().getName();
			} else {
				clazz = "<null>"; //$NON-NLS-1$
			}
			StatusHandler.log(new Status(IStatus.WARNING, TasksUiPlugin.ID_PLUGIN,
					"Expected an instance of RichTextAttributeEditor, got \"" + clazz + "\"")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}

		Section section = createSection(parent, toolkit, sectionStyle);

		composite = toolkit.createComposite(section);
		composite.setLayout(EditorUtil.createSectionClientLayout());

		editor = (RichTextAttributeEditor) attributEditor;

		editor.createControl(composite, toolkit);
		if (editor.isReadOnly()) {
			composite.setLayout(new FillWidthLayout(EditorUtil.getLayoutAdvisor(getTaskEditorPage()), 0, 0, 0, 3));
		} else {
			StyledText textWidget = editor.getViewer().getTextWidget();
			editor.getControl().setLayoutData(
					EditorUtil.getTextControlLayoutData(getTaskEditorPage(), textWidget, getExpandVertically()));
			editor.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
		}

		getEditor().getControl().setData(EditorUtil.KEY_TOGGLE_TO_MAXIMIZE_ACTION, getMaximizePartAction());
		if (getEditor().getControl() instanceof Composite) {
			for (Control control : ((Composite) getEditor().getControl()).getChildren()) {
				control.setData(EditorUtil.KEY_TOGGLE_TO_MAXIMIZE_ACTION, getMaximizePartAction());
			}
		}
		getTaskEditorPage().getAttributeEditorToolkit().adapt(editor);

		toolkit.paintBordersFor(composite);
		section.setClient(composite);
		setSection(toolkit, section);
	}

	public TaskAttribute getAttribute() {
		return attribute;
	}

	protected Composite getComposite() {
		return composite;
	}

	protected RichTextAttributeEditor getEditor() {
		return editor;
	}

	public void setAttribute(TaskAttribute attribute) {
		this.attribute = attribute;
	}

	@Override
	public void setFocus() {
		if (editor != null) {
			editor.getControl().setFocus();
		}
	}

	protected Action getMaximizePartAction() {
		if (toggleToMaximizePartAction == null) {
			toggleToMaximizePartAction = new ToggleToMaximizePartAction();
		}
		return toggleToMaximizePartAction;
	}

	private class ToggleToMaximizePartAction extends Action {

		private static final String COMMAND_ID = "org.eclipse.mylyn.tasks.ui.command.maximizePart"; //$NON-NLS-1$

		private/*static*/final String MAXIMIZE = Messages.TaskEditorRichTextPart_Maximize;

		private static final int SECTION_HEADER_HEIGHT = 50;

		private int originalHeight = -2;

		public ToggleToMaximizePartAction() {
			super("", SWT.TOGGLE); //$NON-NLS-1$
			setImageDescriptor(CommonImages.PART_MAXIMIZE);
			setToolTipText(MAXIMIZE);
			setActionDefinitionId(COMMAND_ID);
			setChecked(false);
		}

		@Override
		public void run() {
			if (!(getEditor().getControl().getLayoutData() instanceof GridData)) {
				return;
			}

			GridData gd = (GridData) getEditor().getControl().getLayoutData();

			if (originalHeight == -2) {
				originalHeight = gd.heightHint;
			}

			try {
				getTaskEditorPage().setReflow(false);

				int heightHint;
				if (isChecked()) {
					heightHint = getManagedForm().getForm().getClientArea().height - SECTION_HEADER_HEIGHT;
				} else {
					heightHint = originalHeight;
				}

				// ignore when not necessary
				if (gd.heightHint == heightHint) {
					return;
				}
				gd.heightHint = heightHint;
				gd.minimumHeight = heightHint;
			} finally {
				getTaskEditorPage().setReflow(true);
			}

			getTaskEditorPage().reflow();
			CommonFormUtil.ensureVisible(getEditor().getControl());
		}
	}

	@Override
	protected void fillToolBar(ToolBarManager manager) {
		if (getEditor().hasPreview()) {
			toggleEditAction = new Action("", SWT.TOGGLE) { //$NON-NLS-1$
				@Override
				public void run() {
					if (isChecked()) {
						editor.showEditor();
					} else {
						editor.showPreview();
					}

					if (toggleBrowserAction != null) {
						toggleBrowserAction.setChecked(false);
					}
				}
			};
			toggleEditAction.setImageDescriptor(CommonImages.EDIT_SMALL);
			toggleEditAction.setToolTipText(Messages.TaskEditorRichTextPart_Edit_Tooltip);
			toggleEditAction.setChecked(true);
			getEditor().getEditor().addStateChangedListener(new StateChangedListener() {
				public void stateChanged(StateChangedEvent event) {
					try {
						ignoreToggleEvents = true;
						toggleEditAction.setChecked(event.state == State.EDITOR || event.state == State.DEFAULT);
					} finally {
						ignoreToggleEvents = false;
					}
				}
			});
			manager.add(toggleEditAction);
		}
		if (toggleEditAction == null && getEditor().hasBrowser()) {
			toggleBrowserAction = new Action("", SWT.TOGGLE) { //$NON-NLS-1$
				@Override
				public void run() {
					if (ignoreToggleEvents) {
						return;
					}
					if (isChecked()) {
						editor.showBrowser();
					} else {
						editor.showEditor();
					}

					if (toggleEditAction != null) {
						toggleEditAction.setChecked(false);
					}
				}
			};
			toggleBrowserAction.setImageDescriptor(CommonImages.PREVIEW_WEB);
			toggleBrowserAction.setToolTipText(Messages.TaskEditorRichTextPart_Browser_Preview);
			toggleBrowserAction.setChecked(false);
			getEditor().getEditor().addStateChangedListener(new StateChangedListener() {
				public void stateChanged(StateChangedEvent event) {
					try {
						ignoreToggleEvents = true;
						toggleBrowserAction.setChecked(event.state == State.BROWSER);
					} finally {
						ignoreToggleEvents = false;
					}
				}
			});
			manager.add(toggleBrowserAction);
		}
		if (!getEditor().isReadOnly()) {
			manager.add(getMaximizePartAction());
		}
		super.fillToolBar(manager);
	}

}
