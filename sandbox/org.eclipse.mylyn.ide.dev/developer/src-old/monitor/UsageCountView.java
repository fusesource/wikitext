/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.mylyn.core.MylarPlugin;
import org.eclipse.mylyn.monitor.MylarMonitorPlugin;
import org.eclipse.mylyn.monitor.reports.IUsageCollector;
import org.eclipse.mylyn.monitor.reports.ReportGenerator;
import org.eclipse.mylyn.monitor.reports.collectors.SummaryCollector;
import org.eclipse.mylyn.monitor.reports.internal.InteractionEventSummarySorter;
import org.eclipse.mylyn.monitor.ui.MonitorImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.part.ViewPart;

/**
 * @author Leah Findlater and Mik Kersten
 */
	private ReportGenerator parser;
	
    
    	List<IUsageCollector> collectors = new ArrayList<IUsageCollector>();
		collectors.add(new SummaryCollector());
        
        List<File> usageFiles = new ArrayList<File>();
        usageFiles.add(MylarMonitorPlugin.getDefault().getMonitorLogFile());
        viewer.setLabelProvider(new UsageCountLabelProvider());