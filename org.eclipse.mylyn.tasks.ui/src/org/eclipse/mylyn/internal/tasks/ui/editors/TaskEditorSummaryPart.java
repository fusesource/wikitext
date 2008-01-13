/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.editors;

import java.util.Date;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.mylyn.monitor.core.DateUtil;
import org.eclipse.mylyn.tasks.core.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.RepositoryTaskAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @author Steffen Pingel
 */
public class TaskEditorSummaryPart extends AbstractTaskEditorPart {

	private static final String HEADER_DATE_FORMAT = "yyyy-MM-dd HH:mm";

	protected TextViewer summaryTextViewer;

	/**
	 * WARNING: This is present for backward compatibility only. You can get and set text on this widget but all ui
	 * related changes to this widget will have no affect as ui is now being presented with a StyledText widget. This
	 * simply proxies get/setText calls to the StyledText widget.
	 */
	protected Text summaryText;

	private FormToolkit toolkit;

	public TaskEditorSummaryPart(AbstractTaskEditorPage taskEditorPage) {
		super(taskEditorPage);
	}

	private void addNameValue(Composite parent, RepositoryTaskAttribute attribute) {
		Composite nameValue = toolkit.createComposite(parent);
		nameValue.setLayout(new GridLayout(2, false));
		if (attribute != null) {
			createLabel(nameValue, attribute);
			createTextField(nameValue, attribute, SWT.FLAT | SWT.READ_ONLY);
		}
	}

	/**
	 * Adds a text editor with spell checking enabled to display and edit the task's summary.
	 * 
	 * @author Raphael Ackermann (modifications) (bug 195514)
	 * @param attributesComposite
	 *            The composite to add the text editor to.
	 */
	protected void addSummaryText(Composite attributesComposite) {
		Composite summaryComposite = toolkit.createComposite(attributesComposite);
		GridLayout summaryLayout = new GridLayout(2, false);
		summaryLayout.verticalSpacing = 0;
		summaryLayout.marginHeight = 2;
		summaryComposite.setLayout(summaryLayout);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(summaryComposite);

		if (getTaskData() != null) {
			RepositoryTaskAttribute attribute = getTaskData().getAttribute(RepositoryTaskAttribute.SUMMARY);
			if (attribute == null) {
				getTaskData().setAttributeValue(RepositoryTaskAttribute.SUMMARY, "");
				attribute = getTaskData().getAttribute(RepositoryTaskAttribute.SUMMARY);
			}

			final RepositoryTaskAttribute summaryAttribute = attribute;

			summaryTextViewer = getTaskEditorPage().addTextEditor(getTaskRepository(), summaryComposite, attribute.getValue(), true, SWT.FLAT
					| SWT.SINGLE);
			Composite hiddenComposite = new Composite(summaryComposite, SWT.NONE);
			hiddenComposite.setLayout(new GridLayout());
			GridData hiddenLayout = new GridData();
			hiddenLayout.exclude = true;
			hiddenComposite.setLayoutData(hiddenLayout);

			// bugg#210695 - work around for 2.0 api breakage
			summaryText = new Text(hiddenComposite, SWT.NONE);
			summaryText.setText(attribute.getValue());
			summaryText.addKeyListener(new KeyListener() {

				public void keyPressed(KeyEvent e) {
					if (summaryTextViewer != null && !summaryTextViewer.getTextWidget().isDisposed()) {
						String newValue = summaryText.getText();
						String oldValue = summaryTextViewer.getTextWidget().getText();
						if (!newValue.equals(oldValue)) {
							summaryTextViewer.getTextWidget().setText(newValue);
							summaryAttribute.setValue(newValue);
							getTaskEditorPage().getAttributeEditorManager().attributeChanged(summaryAttribute);
						}
					}
				}

				public void keyReleased(KeyEvent e) {
					// ignore
				}
			});

			summaryText.addFocusListener(new FocusListener() {

				public void focusGained(FocusEvent e) {
					summaryTextViewer.getTextWidget().setFocus();
				}

				public void focusLost(FocusEvent e) {
					// ignore
				}
			});

			summaryTextViewer.setEditable(true);
			summaryTextViewer.getTextWidget().setIndent(2);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(summaryTextViewer.getControl());

			getTaskEditorPage().getAttributeEditorManager().decorate(attribute, summaryTextViewer.getTextWidget());
			summaryTextViewer.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);

			summaryTextViewer.addTextListener(new ITextListener() {
				public void textChanged(TextEvent event) {
					String newValue = summaryTextViewer.getTextWidget().getText();
					if (!newValue.equals(summaryAttribute.getValue())) {
						summaryAttribute.setValue(newValue);
						getTaskEditorPage().getAttributeEditorManager().attributeChanged(summaryAttribute);
					}
					if (summaryText != null && !newValue.equals(summaryText.getText())) {
						summaryText.setText(newValue);
					}
				}
			});

		}
		toolkit.paintBordersFor(summaryComposite);
	}

	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		createSummaryLayout(parent);
	}

	protected Label createLabel(Composite composite, RepositoryTaskAttribute attribute) {
		Label label;
		if (getTaskEditorPage().getAttributeEditorManager().hasOutgoingChanges(attribute)) {
			label = toolkit.createLabel(composite, "*" + attribute.getName());
		} else {
			label = toolkit.createLabel(composite, attribute.getName());
		}
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(label);
		return label;
	}

	/**
	 * @author Raphael Ackermann (modifications) (bug 195514)
	 */
	protected void createSummaryLayout(Composite composite) {
		addSummaryText(composite);
		if (summaryTextViewer != null) {
			summaryTextViewer.prependVerifyKeyListener(new TabVerifyKeyListener());
		}

		headerInfoComposite = toolkit.createComposite(composite);
		GridLayout headerLayout = new GridLayout(11, false);
		headerLayout.verticalSpacing = 1;
		headerLayout.marginHeight = 1;
		headerLayout.marginHeight = 1;
		headerLayout.marginWidth = 1;
		headerLayout.horizontalSpacing = 6;
		headerInfoComposite.setLayout(headerLayout);

		RepositoryTaskAttribute statusAtribute = getTaskData().getAttribute(RepositoryTaskAttribute.STATUS);
		addNameValue(headerInfoComposite, statusAtribute);
		toolkit.paintBordersFor(headerInfoComposite);

		RepositoryTaskAttribute priorityAttribute = getTaskData().getAttribute(RepositoryTaskAttribute.PRIORITY);
		addNameValue(headerInfoComposite, priorityAttribute);

		String idLabel = getTaskData().getTaskKey();
		if (idLabel != null) {
			Composite nameValue = toolkit.createComposite(headerInfoComposite);
			nameValue.setLayout(new GridLayout(2, false));
			Label label = toolkit.createLabel(nameValue, "ID:");// .setFont(TITLE_FONT);
			label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
			// toolkit.createText(nameValue, idLabel, SWT.FLAT | SWT.READ_ONLY);
			Text text = new Text(nameValue, SWT.FLAT | SWT.READ_ONLY);
			toolkit.adapt(text, true, true);
			text.setText(idLabel);
		}

		String openedDateString = "";
		String modifiedDateString = "";
		final AbstractTaskDataHandler taskDataManager = getConnector().getTaskDataHandler();
		if (taskDataManager != null) {
			Date created = getTaskData().getAttributeFactory().getDateForAttributeType(
					RepositoryTaskAttribute.DATE_CREATION, getTaskData().getCreated());
			openedDateString = created != null ? DateUtil.getFormattedDate(created, HEADER_DATE_FORMAT) : "";

			Date modified = getTaskData().getAttributeFactory().getDateForAttributeType(
					RepositoryTaskAttribute.DATE_MODIFIED, getTaskData().getLastModified());
			modifiedDateString = modified != null ? DateUtil.getFormattedDate(modified, HEADER_DATE_FORMAT) : "";
		}

		RepositoryTaskAttribute creationAttribute = getTaskData().getAttribute(RepositoryTaskAttribute.DATE_CREATION);
		if (creationAttribute != null) {
			Composite nameValue = toolkit.createComposite(headerInfoComposite);
			nameValue.setLayout(new GridLayout(2, false));
			createLabel(nameValue, creationAttribute);
			// toolkit.createText(nameValue, openedDateString, SWT.FLAT |
			// SWT.READ_ONLY);
			Text text = new Text(nameValue, SWT.FLAT | SWT.READ_ONLY);
			toolkit.adapt(text, true, true);
			text.setText(openedDateString);
		}

		RepositoryTaskAttribute modifiedAttribute = getTaskData().getAttribute(RepositoryTaskAttribute.DATE_MODIFIED);
		if (modifiedAttribute != null) {
			Composite nameValue = toolkit.createComposite(headerInfoComposite);
			nameValue.setLayout(new GridLayout(2, false));
			createLabel(nameValue, modifiedAttribute);
			// toolkit.createText(nameValue, modifiedDateString, SWT.FLAT |
			// SWT.READ_ONLY);
			Text text = new Text(nameValue, SWT.FLAT | SWT.READ_ONLY);
			toolkit.adapt(text, true, true);
			text.setText(modifiedDateString);
		}
	}

	/**
	 * Utility method to create text field sets background to TaskListColorsAndFonts.COLOR_ATTRIBUTE_CHANGED if
	 * attribute has changed.
	 * 
	 * @param composite
	 * @param attribute
	 * @param style
	 */
	protected Text createTextField(Composite composite, RepositoryTaskAttribute attribute, int style) {
		String value;
		if (attribute == null || attribute.getValue() == null) {
			value = "";
		} else {
			value = attribute.getValue();
		}

		final Text text;
		if ((SWT.READ_ONLY & style) == SWT.READ_ONLY) {
			text = new Text(composite, style);
			toolkit.adapt(text, true, true);
			text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
			text.setText(value);
		} else {
			text = toolkit.createText(composite, value, style);
		}

		if (attribute != null && !attribute.isReadOnly()) {
			text.setData(attribute);
			text.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					String newValue = text.getText();
					RepositoryTaskAttribute attribute = (RepositoryTaskAttribute) text.getData();
					attribute.setValue(newValue);
					getTaskEditorPage().getAttributeEditorManager().attributeChanged(attribute);
				}
			});
		}
		getTaskEditorPage().getAttributeEditorManager().decorate(attribute, text);
		return text;
	}

	/**
	 * @author Raphael Ackermann (bug 195514)
	 */
	private class TabVerifyKeyListener implements VerifyKeyListener {

		public void verifyKey(VerifyEvent event) {
			// if there is a tab key, do not "execute" it and instead select the Status control
			if (event.keyCode == SWT.TAB) {
				event.doit = false;
				if (headerInfoComposite != null) {
					headerInfoComposite.setFocus();
				}
			}
		}

	}
	private Composite headerInfoComposite;

	@Override
	public void setFocus() {
		summaryTextViewer.getTextWidget().setFocus();
	}
}
