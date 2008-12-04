/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * @author Rob Elves
 */
public interface ITasksCoreConstants {

	public static final int MAX_SUBTASK_DEPTH = 10;

	public static final String ID_PLUGIN = "org.eclipse.mylyn.tasks.core"; //$NON-NLS-1$

	public static final String OLD_TASK_LIST_FILE = "tasklist.xml"; //$NON-NLS-1$

	public static final String FILENAME_ENCODING = "UTF-8"; //$NON-NLS-1$

	public static final String OLD_PREFIX_TASKLIST = "tasklist"; //$NON-NLS-1$

	public static final String PREFIX_TASKS = "tasks"; //$NON-NLS-1$

	public static final String DEFAULT_BACKUP_FOLDER_NAME = "backup"; //$NON-NLS-1$

	public static final String FILE_EXTENSION = ".xml.zip"; //$NON-NLS-1$

	public static final String OLD_FILENAME_TIMESTAMP_FORMAT = "yyyy-MM-dd"; //$NON-NLS-1$

	public static final String FILENAME_TIMESTAMP_FORMAT = "yyyy-MM-dd-HHmmss"; //$NON-NLS-1$

	public static final String OLD_M_2_TASKLIST_FILENAME = OLD_PREFIX_TASKLIST + FILE_EXTENSION;

	public static final String DEFAULT_TASK_LIST_FILE = PREFIX_TASKS + FILE_EXTENSION;

	public static final String CONTEXTS_DIRECTORY = "contexts"; //$NON-NLS-1$

	public static final ActivityContextSchedulingRule ACTIVITY_SCHEDULING_RULE = new ActivityContextSchedulingRule();

	public static final TaskListSchedulingRule TASKLIST_SCHEDULING_RULE = new TaskListSchedulingRule();

	public static final ISchedulingRule ROOT_SCHEDULING_RULE = new RootSchedulingRule();

	public static final String ATTRIBUTE_OUTGOING_NEW_REPOSITORY_URL = "outgoingNewRepositoryUrl"; //$NON-NLS-1$

	public static final String ATTRIBUTE_OUTGOING_NEW_CONNECTOR_KIND = "outgoingNewConnectorKind"; //$NON-NLS-1$

	static class ActivityContextSchedulingRule extends RootSchedulingRule {
		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule instanceof ActivityContextSchedulingRule;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			if (rule.getClass() == RootSchedulingRule.class) {
				return true;
			}
			return rule instanceof ActivityContextSchedulingRule;
		}
	}

	static class TaskListSchedulingRule extends RootSchedulingRule {

		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule instanceof TaskListSchedulingRule;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			if (rule.getClass() == RootSchedulingRule.class) {
				return true;
			}
			return rule instanceof TaskListSchedulingRule;
		}
	}

	static class RootSchedulingRule implements ISchedulingRule {

		public boolean contains(ISchedulingRule rule) {
			return rule instanceof RootSchedulingRule;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return rule instanceof RootSchedulingRule;
		}
	}

	public static final String PROPERTY_ACTIVATE_TASK = "org.eclipse.mylyn.activateTask"; //$NON-NLS-1$

	public static final String PROPERTY_LINK_PROVIDER_TIMEOUT = "org.eclipse.mylyn.linkProviderTimeout"; //$NON-NLS-1$

}
