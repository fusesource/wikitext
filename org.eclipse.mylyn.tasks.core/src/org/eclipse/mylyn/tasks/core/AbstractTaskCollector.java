/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core;

/**
 * This class is used for collecting tasks, e.g. when performing queries on a repository.
 * 
 * @author Rob Elves
 * @since 3.0
 */
public abstract class AbstractTaskCollector {

	public static final int MAX_HITS = 5000;

	/**
	 * @since 3.0
	 */
	public abstract void accept(RepositoryTaskData taskData);

}
