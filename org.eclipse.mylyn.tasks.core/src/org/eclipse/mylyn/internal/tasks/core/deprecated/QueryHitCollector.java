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

package org.eclipse.mylyn.internal.tasks.core.deprecated;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;

/**
 * @deprecated Do not use. This class is pending for removal: see bug 237552.
 */
@Deprecated
public class QueryHitCollector extends LegacyTaskDataCollector {

	/**
	 * @deprecated Use {@link TaskDataCollector#MAX_HITS} instead
	 */
	@Deprecated
	public static final int MAX_HITS = TaskDataCollector.MAX_HITS;

	private final Set<AbstractTask> taskResults = new HashSet<AbstractTask>();

	private final ITaskFactory taskFactory;

	public QueryHitCollector(ITaskFactory taskFactory) {
		this.taskFactory = taskFactory;
	}

//	public void accept(AbstractTask task) {
//		if (task == null) {
//			throw new IllegalArgumentException();
//		}
//		if (taskResults.size() < MAX_HITS) {
//			taskResults.add(task);
//		}
//	}

	@Override
	public void accept(RepositoryTaskData taskData) {
		if (taskData == null) {
			throw new IllegalArgumentException();
		}

		AbstractTask task;
		try {
			task = taskFactory.createTask(taskData, new NullProgressMonitor());
			if (taskResults.size() < TaskDataCollector.MAX_HITS) {
				taskResults.add(task);
			}
		} catch (CoreException e) {
			// FIXMEx
			e.printStackTrace();
		}
	}

	public Set<AbstractTask> getTasks() {
		return taskResults;
	}

}
