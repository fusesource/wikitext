/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.ui.editor;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaAttribute;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorPartDescriptor;

/**
 * @author Rob Elves
 * @since 3.0
 */
public class BugzillaTaskEditorPage extends AbstractTaskEditorPage {

	public static final String ID_PART_BUGZILLA_PLANNING = "org.eclipse.mylyn.bugzilla.ui.editors.part.planning";

	public BugzillaTaskEditorPage(TaskEditor editor) {
		super(editor, BugzillaCorePlugin.CONNECTOR_KIND);
	}

	@Override
	protected Set<TaskEditorPartDescriptor> createPartDescriptors() {
		Set<TaskEditorPartDescriptor> descriptors = super.createPartDescriptors();

		// remove unnecessary default editor parts
		for (TaskEditorPartDescriptor taskEditorPartDescriptor : descriptors) {
			if (taskEditorPartDescriptor.getId().equals(ID_PART_PEOPLE)) {
				descriptors.remove(taskEditorPartDescriptor);
				break;
			}
		}

		// Add Bugzilla Planning part
		try {
			TaskData data = TasksUi.getTaskDataManager().getTaskData(getTask());
			if (data != null) {
				TaskAttribute attrEstimatedTime = data.getRoot().getMappedAttribute(
						BugzillaAttribute.ESTIMATED_TIME.getKey());
				if (attrEstimatedTime != null) {
					descriptors.add(new TaskEditorPartDescriptor(ID_PART_BUGZILLA_PLANNING) {
						@Override
						public AbstractTaskEditorPart createPart() {
							return new BugzillaPlanningEditorPart();
						}
					}.setPath(PATH_ATTRIBUTES));
				}
			}
		} catch (CoreException e) {
			// ignore
		}

		// Add the updated Bugzilla people part
		descriptors.add(new TaskEditorPartDescriptor(ID_PART_PEOPLE) {
			@Override
			public AbstractTaskEditorPart createPart() {
				return new BugzillaPeoplePart();
			}
		}.setPath(PATH_PEOPLE));

		return descriptors;
	}

	@Override
	protected AttributeEditorFactory createAttributeEditorFactory() {
		AttributeEditorFactory factory = new AttributeEditorFactory(getModel(), getTaskRepository()) {
			@Override
			public AbstractAttributeEditor createEditor(String type, TaskAttribute taskAttribute) {
				AbstractAttributeEditor editor;
				if (IBugzillaConstants.EDITOR_TYPE_KEYWORDS.equals(type)) {
					editor = new BugzillaKeywordAttributeEditor(getModel(), taskAttribute);
				} else if (IBugzillaConstants.EDITOR_TYPE_REMOVECC.equals(type)) {
					editor = new BugzillaCcAttributeEditor(getModel(), taskAttribute);
				} else if (IBugzillaConstants.EDITOR_TYPE_VOTES.equals(type)) {
					editor = new BugzillaVotesEditor(getModel(), taskAttribute);
				} else {
					editor = super.createEditor(type, taskAttribute);
					if (TaskAttribute.TYPE_BOOLEAN.equals(type)) {
						editor.setDecorationEnabled(false);
					}
				}

				return editor;
			}
		};
		return factory;
	}

}
