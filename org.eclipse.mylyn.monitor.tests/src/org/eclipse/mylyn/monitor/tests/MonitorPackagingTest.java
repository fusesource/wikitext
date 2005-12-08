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
/*
 * Created on Jun 13, 2005
  */
package org.eclipse.mylar.monitor.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.core.tests.AbstractContextTest;
import org.eclipse.mylar.core.util.ZipFileUtil;
import org.eclipse.mylar.monitor.MylarMonitorPlugin;

/**
 * @author Mik Kersten
 */
public class MonitorPackagingTest extends AbstractContextTest {

    public void testCreateUploadPackage() throws IOException, InterruptedException {
        MylarMonitorPlugin.getDefault().getInteractionLogger().stopObserving();
//        MylarMonitorPlugin.getDefault().stopLog();
                
        File monitorFile = MylarMonitorPlugin.getDefault().getMonitorLogFile();
//        File logFile = MylarMonitorPlugin.getDefault().getLogFile();
        
        List<File> files = new ArrayList<File>();
        files.add(monitorFile);
//        files.add(logFile);
        
        File zipFile = new File(MylarPlugin.getDefault().getDataDirectory() + "/mylarUpload.zip");
        
        ZipFileUtil.createZipFile(zipFile, files);

//        MylarMonitorPlugin.getDefault().startLog();
        MylarMonitorPlugin.getDefault().getInteractionLogger().startObserving();
        
        // pretend to upload
        Thread.sleep(1000);
        

        zipFile = new File(MylarPlugin.getDefault().getDataDirectory() + "/mylarUpload.zip");
        
        // Open the ZIP file
        ZipFile zf = new ZipFile(zipFile);
    
        int numEntries = 0;
        
        // Enumerate each entry
        for (Enumeration entries = zf.entries(); entries.hasMoreElements();) {
        	numEntries ++;
            String zipEntryName = ((ZipEntry)entries.nextElement()).getName();
            assertTrue("Unknown Entry: " + zipEntryName, zipEntryName.compareTo(monitorFile.getName()) == 0);// || zipEntryName.compareTo(logFile.getName()) == 0);
        }
        assertEquals("Results not correct size", 1, numEntries);
        
        // check the length of the zip
//        long fileLength = monitorFile.length() + logFile.length();
//        if(monitorFile.length() != 0 || logFile.length() != 0)
//        	assertTrue("Zip didn't help", fileLength > zipFile.length());
 
        // delete it
        zipFile.delete();
    }
    
    public void testCreateLargeUploadPackage() throws IOException, InterruptedException {
        
    	for(int i = 0; i < 20000; i++){
    		MylarPlugin.getDefault().notifyInteractionObserved(mockSelection());
    	}
    	testCreateUploadPackage();
    }
    
}
