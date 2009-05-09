/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.discovery.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A directory representing locations of discovery sources.
 * 
 * @author David Green
 */
public class Directory {
	/**
	 * an entry in the directory, which represents a specific discovery source. Future versions of this class may
	 * include policy or other attributes declared by the directory.
	 */
	public static final class Entry {
		private String location;

		public Entry() {
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}
	}

	private final List<Entry> entries = new ArrayList<Entry>();

	public List<Entry> getEntries() {
		return entries;
	}
}