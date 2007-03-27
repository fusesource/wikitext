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
package org.eclipse.mylar.internal.tasks.ui;

import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.ITask;

/**
 * @author Mik Kersten
 */
public class TaskCompletionFilter extends AbstractTaskListFilter {

	@Override
	public boolean select(Object parent, Object element) {
		if (element instanceof ITask) {
			ITask task = (ITask) element;
			if (shouldAlwaysShow(parent, task)) {
				return true;
			}
			return !task.isCompleted();
		} else if (element instanceof AbstractQueryHit) {
			AbstractQueryHit hit = (AbstractQueryHit) element;
			if (hit.getCorrespondingTask() != null) {
				if (shouldAlwaysShow(parent, hit.getCorrespondingTask())) {
					return true;
				} else {
					return !hit.getCorrespondingTask().isCompleted();
				}
			} else {
				return !hit.isCompleted(); 
			}
		}
		return true;
	}
}
