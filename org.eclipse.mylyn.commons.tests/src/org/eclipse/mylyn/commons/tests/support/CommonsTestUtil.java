/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.commons.tests.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import junit.framework.AssertionFailedError;

import org.eclipse.core.runtime.Platform;

/**
 * @author Steffen Pingel
 */
public class CommonsTestUtil {

	public static File getFile(Object source, String filename) throws Exception {
		if (Platform.isRunning()) {
//				if (ContextTestsPlugin.getDefault() != null) {
//					URL localURL = FileLocator.toFileURL(ContextTestsPlugin.getDefault().getBundle().getEntry(
//							filename));
//					filename = localURL.getFile();
//				}
		} else {
			URL localURL = source.getClass().getResource("");
			String directory = source.getClass().getName().replaceAll("[^.]", "");
			directory = directory.replaceAll(".", "../");
			directory += "../"; // account for bin/
			return new File(localURL.getFile() + directory + filename);
		}
		throw new AssertionFailedError("Could not locate " + filename);
	}

	public static String read(File source) throws IOException {
		InputStream in = new FileInputStream(source);
		try {
			StringBuilder sb = new StringBuilder();
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				sb.append(new String(buf, 0, len));
			}
			return sb.toString();
		} finally {
			in.close();
		}
	}

	/**
	 * Copies all files in the current data directory to the specified folder. Will overwrite.
	 */
	public static void copyFolder(File sourceFolder, File targetFolder) throws IOException {
		for (File currFile : sourceFolder.listFiles()) {
			if (currFile.isFile()) {
				File destFile = new File(targetFolder, currFile.getName());
				copy(currFile, destFile);
			} else if (currFile.isDirectory()) {
				File destDir = new File(targetFolder, currFile.getName());
				if (!destDir.exists()) {
					if (!destDir.mkdir()) {
						throw new IOException("Unable to create destination context folder: "
								+ destDir.getAbsolutePath());
					}
				}
				for (File file : currFile.listFiles()) {
					File destFile = new File(destDir, file.getName());
					if (destFile.exists()) {
						destFile.delete();
					}
					copy(file, destFile);
				}
			}
		}
	}

	public static void copy(File source, File dest) throws IOException {
		InputStream in = new FileInputStream(source);
		try {
			OutputStream out = new FileOutputStream(dest);
			try {
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
	}

}
