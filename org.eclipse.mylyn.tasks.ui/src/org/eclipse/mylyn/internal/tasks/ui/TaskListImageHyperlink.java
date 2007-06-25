/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

/**
 * Override ImageHyperlink to truncate its text at the end, rather than the middle.
 * 
 * @author Leo Dos Santos
 */
public class TaskListImageHyperlink extends ImageHyperlink {

	public TaskListImageHyperlink(Composite parent, int style) {
		super(parent, style);
	}

	// From PerspectiveBarContributionItem
	@Override
	protected String shortenText(GC gc, String t, int width) {
		if (t == null) {
			return null;
		}

		String returnText = t;
		if (gc.textExtent(t).x > width) {
			for (int i = t.length(); i > 0; i--) {
				String test = t.substring(0, i);
				test = test + "...";
				if (gc.textExtent(test).x < width) {
					returnText = test;
					break;
				}
			}
		}
		return returnText;
	}

}
