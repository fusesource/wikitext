/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.core;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;

/**
 * This object holds RepositoryTaskData and is serialized to disk for offline
 * storage.
 * 
 * @author Rob Elves
 */
class OfflineDataStore implements Serializable {

	private static final long serialVersionUID = -3909632088254980426L;

	/** Last new repository task taskId */
	private int lastNewRepositoryTaskId = 0;

	// Local changes to existing reports
	private Map<String, Set<RepositoryTaskAttribute>> localEdits = new ConcurrentHashMap<String, Set<RepositoryTaskAttribute>>();

	/** Older version of Task Data */
	private Map<String, RepositoryTaskData> oldTaskDataMap = new ConcurrentHashMap<String, RepositoryTaskData>();

	/** Newest version of the task data */
	private Map<String, RepositoryTaskData> newTaskDataMap = new ConcurrentHashMap<String, RepositoryTaskData>();

	/** New unsubmitted repository task data */
	private Map<String, RepositoryTaskData> unsubmittedTaskData = new ConcurrentHashMap<String, RepositoryTaskData>();

	public void setLastNewTaskId(int lastNumber) {
		lastNewRepositoryTaskId = new Integer(lastNumber);
	}

	public int getNextTaskId() {
		lastNewRepositoryTaskId++;
		return lastNewRepositoryTaskId;
	}

	public Map<String, RepositoryTaskData> getOldDataMap() {
		return oldTaskDataMap;
	}

	public Map<String, RepositoryTaskData> getNewDataMap() {
		return newTaskDataMap;
	}

	public Map<String, RepositoryTaskData> getUnsubmittedTaskData() {
		return unsubmittedTaskData;
	}

	public Map<String, Set<RepositoryTaskAttribute>> getLocalEdits() {
		return localEdits;
	}
}