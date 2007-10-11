/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.monitor.ui;

/**
 * TODO: consider changing to abstract class
 * 
 * @author Shawn Minto
 */
public interface IMonitoredWindow {

	public boolean isMonitored();
	
	public boolean isPerspectiveManaged();
	
}
