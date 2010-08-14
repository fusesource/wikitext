/*******************************************************************************
 * Copyright (c) 2004, 2010 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.ui.editor;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.tasks.ui.PersonProposalLabelProvider;
import org.eclipse.mylyn.internal.tasks.ui.PersonProposalProvider;
import org.eclipse.mylyn.internal.tasks.ui.editors.EditorUtil;
import org.eclipse.mylyn.internal.tasks.ui.editors.Messages;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.ColumnSpan;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.RowSpan;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * @author Frank Becker
 */
public class FlagAttributeEditor extends AbstractAttributeEditor {

	private String[] values;

	private CCombo combo;

	private Text requesteeText;

	private ImageHyperlink selfLink;

	public FlagAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
		super(manager, taskAttribute);
		setLayoutHint(new LayoutHint(RowSpan.SINGLE, ColumnSpan.SINGLE));
		if (taskAttribute.getAttribute("state") != null) { //$NON-NLS-1$
			setReadOnly(taskAttribute.getAttribute("state").getMetaData().isReadOnly()); //$NON-NLS-1$
		}
	}

	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		final Composite flagComposite = toolkit.createComposite(parent);

		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 1;
		layout.marginWidth = 1;
		layout.horizontalSpacing = 10;
		flagComposite.setLayout(layout);
		if (isReadOnly()) {
			Text text = new Text(flagComposite, SWT.FLAT | SWT.READ_ONLY);
			toolkit.adapt(text, false, false);
			text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
			text.setText(getValueLabel());
			text.setBackground(parent.getBackground());
			text.setEditable(false);
			String tooltip = getTaskAttribute().getMetaData().getLabel();
			if (tooltip != null) {
				text.setToolTipText(tooltip);
			}
			TaskAttribute requestee = getTaskAttribute().getAttribute("requestee"); //$NON-NLS-1$
			if (!"".equals(requestee.getValue())) { //$NON-NLS-1$
				text = new Text(flagComposite, SWT.FLAT | SWT.READ_ONLY);
				toolkit.adapt(text, false, false);
				text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
				text.setText(requestee.getValue());
				text.setBackground(parent.getBackground());
				text.setEditable(false);
			}
		} else {
			combo = new CCombo(flagComposite, SWT.FLAT | SWT.READ_ONLY);
			toolkit.adapt(combo, false, false);
			combo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
			String tooltip = getTaskAttribute().getMetaData().getLabel();
			if (tooltip != null) {
				combo.setToolTipText(tooltip);
			}
			EditorUtil.addScrollListener(combo);

			Map<String, String> labelByValue = getAttributeMapper().getAssoctiatedAttribute(getTaskAttribute())
					.getOptions();
			if (labelByValue != null) {
				values = labelByValue.keySet().toArray(new String[0]);
				for (String value : values) {
					combo.add(labelByValue.get(value));
				}
			}

			select(getValue(), getValueLabel());

			if (values != null) {
				combo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						int index = combo.getSelectionIndex();
						if (index > -1) {
							Assert.isNotNull(values);
							Assert.isLegal(index >= 0 && index <= values.length - 1);
							setValue(values[index]);
							if (requesteeText != null) {
								requesteeText.setEnabled(values[index].equals("?")); //$NON-NLS-1$
								selfLink.setEnabled(values[index].equals("?"));
							}
						}
					}
				});
			}
			TaskAttribute requestee = getTaskAttribute().getAttribute("requestee"); //$NON-NLS-1$
			if (requestee != null && !requestee.getMetaData().isReadOnly()) {
				final Composite requesteeComposite = new Composite(flagComposite, SWT.NONE);
				GridLayout requesteeLayout = new GridLayout(2, false);
				requesteeLayout.marginHeight = 0;
				requesteeLayout.marginWidth = 0;
				requesteeLayout.horizontalSpacing = 0;
				requesteeComposite.setLayout(requesteeLayout);
				requesteeComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				requesteeComposite.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);

				requesteeText = toolkit.createText(requesteeComposite, requestee.getValue());
				boolean enabled = "?".equals(getValueLabel()); //$NON-NLS-1$
				requesteeText.setEnabled(enabled);
				IContentProposalProvider contentProposalProvider = new PersonProposalProvider(null,
						requestee.getTaskData());
				ILabelProvider labelPropsalProvider = new PersonProposalLabelProvider();
				if (contentProposalProvider != null && labelPropsalProvider != null) {
					ContentAssistCommandAdapter adapter = new ContentAssistCommandAdapter(requesteeText,
							new TextContentAdapter(), contentProposalProvider,
							ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[0], true);
					adapter.setLabelProvider(labelPropsalProvider);
					adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
				}

				selfLink = new ImageHyperlink(requesteeComposite, SWT.NO_FOCUS);
				selfLink.setToolTipText(Messages.PersonAttributeEditor_Insert_My_User_Id_Tooltip);
				selfLink.setActiveImage(CommonImages.getImage(CommonImages.PERSON_ME_SMALL));
				selfLink.setHoverImage(CommonImages.getImage(CommonImages.PERSON_ME_SMALL));
				selfLink.setEnabled(enabled);
				selfLink.setBackground(requesteeText.getBackground());
				selfLink.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						String userName = getModel().getTaskRepository().getUserName();
						if (userName != null && userName.length() > 0) {
							requesteeText.setText(userName);
							setValue(userName);
						}
					}
				});
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).exclude(true).applyTo(selfLink);
				MouseTrackListener mouseListener = new MouseTrackAdapter() {
					int version = 0;

					@Override
					public void mouseEnter(MouseEvent e) {
						((GridData) selfLink.getLayoutData()).exclude = false;
						requesteeComposite.layout();
						selfLink.setImage(CommonImages.getImage(CommonImages.PERSON_ME_SMALL));
						selfLink.redraw();
						version++;
					}

					@Override
					public void mouseExit(MouseEvent e) {
						final int lastVersion = version;
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								if (version != lastVersion || selfLink.isDisposed()) {
									return;
								}
								selfLink.setImage(null);
								selfLink.redraw();
								((GridData) selfLink.getLayoutData()).exclude = true;
								requesteeComposite.layout();
							}
						});
					}
				};
				requesteeText.addMouseTrackListener(mouseListener);
				selfLink.addMouseTrackListener(mouseListener);
				GridData requesteeData = new GridData(SWT.FILL, SWT.CENTER, true, false);
				requesteeText.setLayoutData(requesteeData);
				requesteeText.setFont(EditorUtil.TEXT_FONT);
				requesteeText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);

				requesteeText.addKeyListener(new KeyListener() {

					public void keyReleased(KeyEvent e) {
						// ignore
						setRequestee(requesteeText.getText());

					}

					public void keyPressed(KeyEvent e) {
					}
				});
				requesteeText.addModifyListener(new ModifyListener() {

					public void modifyText(ModifyEvent e) {
						setRequestee(requesteeText.getText());
					}
				});
				toolkit.adapt(requesteeText, false, false);

			}
		}
		toolkit.paintBordersFor(flagComposite);
		setControl(flagComposite);
	}

	public String getValue() {
		return getAttributeMapper().getValue(getAttributeMapper().getAssoctiatedAttribute(getTaskAttribute()));
	}

	public String getValueLabel() {
		return getAttributeMapper().getValueLabel(getAttributeMapper().getAssoctiatedAttribute(getTaskAttribute()));
	}

	private void select(String value, String label) {
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				if (values[i].equals(value)) {
					combo.select(i);
					break;
				}
			}
		} else {
			combo.setText(label);
		}
	}

	public void setRequestee(String value) {
		TaskAttribute requestee = getTaskAttribute().getAttribute("requestee"); //$NON-NLS-1$
		if (requestee != null) {
			if (!requestee.getValue().equals(value)) {
				if (!requestee.getValue().equals(value)) {
					getAttributeMapper().setValue(requestee, value);
					attributeChanged();
				}
			}
		}
	}

	public void setValue(String value) {
		getAttributeMapper().setValue(getAttributeMapper().getAssoctiatedAttribute(getTaskAttribute()), value);
		attributeChanged();
	}

	@Override
	public String getLabel() {
		String label = getAttributeMapper().getLabel(getAttributeMapper().getAssoctiatedAttribute(getTaskAttribute()));
		if (label != null) {
			label.replace("&", "&&"); //$NON-NLS-1$//$NON-NLS-2$
		} else {
			label = ""; //$NON-NLS-1$
		}

		TaskAttribute setter = getTaskAttribute().getAttribute("setter"); //$NON-NLS-1$
		if (setter != null) {
			String setterValue = setter.getValue();
			if (setterValue != null && !setterValue.equals("")) { //$NON-NLS-1$
				if (setterValue.indexOf("@") > -1) { //$NON-NLS-1$
					setterValue = setterValue.substring(0, setterValue.indexOf("@")); //$NON-NLS-1$
				}
				label = setterValue + ": " + label; //$NON-NLS-1$
			}
		}
		return label;
	}
}
