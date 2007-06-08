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

package org.eclipse.mylyn.internal.tasks.core;

/**
 * @author Mik Kersten
 */
public class RepositoryTaskHandleUtil {

	public static final String HANDLE_DELIM = "-";

	private static final String MISSING_REPOSITORY = "norepository";
	
	public static String getHandle(String repositoryUrl, String taskId) {
		if (repositoryUrl == null) {
			return MISSING_REPOSITORY + HANDLE_DELIM + taskId;
//		} else if (taskId.contains(HANDLE_DELIM)) {
//			throw new RuntimeException("invalid handle for task, can not contain: " + HANDLE_DELIM + ", was: "
//					+ taskId);
		} else {
			return repositoryUrl + HANDLE_DELIM + taskId;
		}
	}

	public static String getRepositoryUrl(String taskHandle) {
		int index = taskHandle.lastIndexOf(RepositoryTaskHandleUtil.HANDLE_DELIM);
		String url = null;
		if (index != -1) {
			url = taskHandle.substring(0, index);
		}
		return url;
	}

	// @Deprecated
	public static String getTaskId(String taskHandle) {
		int index = taskHandle.lastIndexOf(HANDLE_DELIM);
		if (index != -1) {
			String id = taskHandle.substring(index + 1);
			return id;
		}
		return null;
	}

}
