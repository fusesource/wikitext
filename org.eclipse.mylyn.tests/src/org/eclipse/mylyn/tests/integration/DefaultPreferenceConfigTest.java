/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tests.integration;

import org.eclipse.mylyn.internal.monitor.usage.MonitorPreferenceConstants;
import org.eclipse.mylyn.internal.monitor.usage.UiUsageMonitorPlugin;

import junit.framework.TestCase;

/**
 * @author Mik Kersten
 */
public class DefaultPreferenceConfigTest extends TestCase {

	public void testMonitorPreferences() {
		assertNotNull(UiUsageMonitorPlugin.getDefault());
		assertTrue(UiUsageMonitorPlugin.getPrefs().getBoolean(MonitorPreferenceConstants.PREF_MONITORING_OBFUSCATE));
	}
}
