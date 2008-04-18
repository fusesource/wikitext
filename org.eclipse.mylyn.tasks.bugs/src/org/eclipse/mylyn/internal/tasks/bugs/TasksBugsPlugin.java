/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.bugs;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Steffen Pingel
 */
public class TasksBugsPlugin extends AbstractUIPlugin {

	public static final String ID_PLUGIN = "org.eclipse.mylyn.tasks.bugs";
	
	private static TasksBugsPlugin INSTANCE;

	private static TaskErrorReporter taskErrorReporter;

	public static TasksBugsPlugin getDefault() {
		return INSTANCE;
	}

	public static synchronized TaskErrorReporter getTaskErrorReporter() {
		if (taskErrorReporter == null) {
			taskErrorReporter = new TaskErrorReporter();
		}
		return taskErrorReporter;
	}
	
	public TasksBugsPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		INSTANCE = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		INSTANCE = null;
		super.stop(context);
	}
	
}
