/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylar.tasks.ui.editors;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.URLHyperlink;
import org.eclipse.mylar.tasks.ui.TasksUiUtil;

/**
 * @author Rob Elves
 */
public class MylarUrlHyperlink extends URLHyperlink {

	public MylarUrlHyperlink(IRegion region, String urlString) {
		super(region, urlString);
	}
	@Override
	public void open() {
		String url = getURLString();
		if(TasksUiUtil.openRepositoryTask(null, null, url)) {
			return;
		}
		super.open();
	}

}
