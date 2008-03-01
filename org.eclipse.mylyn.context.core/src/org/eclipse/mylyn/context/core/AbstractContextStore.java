/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.context.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * File-based store used for writing Mylyn-specific date such as the task list and task contexts (e.g.
 * workspace/.metadata/.mylyn folder).
 * 
 * @author Mik Kersten *
 * @since 2.0
 */
public abstract class AbstractContextStore {

	private final List<IContextStoreListener> listeners = new ArrayList<IContextStoreListener>();

	public abstract void init();

	/**
	 * @return a directory that can be written to.
	 */
	public abstract File getRootDirectory();

	public abstract File getContextDirectory();

	public void contextStoreMoved() {
		init();
		for (IContextStoreListener listener : listeners) {
			listener.contextStoreMoved();
		}
	}

	public void addListener(IContextStoreListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IContextStoreListener listener) {
		listeners.remove(listener);
	}
}
