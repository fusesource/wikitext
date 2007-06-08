/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylyn.core.IStatusHandler;
import org.eclipse.mylyn.core.MylarStatusHandler;
import org.eclipse.mylyn.internal.tasks.ui.util.WebBrowserDialog;
import org.eclipse.mylyn.tasks.core.IMylarStatusConstants;
import org.eclipse.mylyn.tasks.core.MylarStatus;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @author Mik Kersten
 * @author Rob Elves
 */
public class RepositoryAwareStatusHandler implements IStatusHandler {

	protected static final String ERROR_MESSAGE = "Please report the following error at:\n"
			+ "http://bugs.eclipse.org/bugs/enter_bug.cgi?product=Mylar\n\n"
			+ "Or select Report as Bug from popup menu on error in the Error Log (Window -> Show View -> Error Log)";

	// TODO: implement option to report bug
	public void fail(final IStatus status, boolean informUser) {
		if (informUser && Platform.isRunning()) {
			try {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						Shell shell = null;
						if (PlatformUI.getWorkbench() != null
								&& PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
							shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
						}
						ErrorDialog.openError(shell, "Mylar Error", ERROR_MESSAGE, status);
					}
				});
			} catch (Throwable t) {
				status.getException().printStackTrace();
			}
		}
	}

	public void displayStatus(final String title, final IStatus status) {

		if (status.getCode() == IMylarStatusConstants.INTERNAL_ERROR) {
			MylarStatusHandler.log(status);
			fail(status, true);
			return;
		}

		if (Platform.isRunning()) {
			try {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						Shell shell = null;
						if (PlatformUI.getWorkbench() != null
								&& PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
							shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
						}

						if (status instanceof MylarStatus && ((MylarStatus) status).isHtmlMessage()) {
							WebBrowserDialog.openAcceptAgreement(shell, title, status.getMessage(),
									((MylarStatus) status).getHtmlMessage());
							return;
						}

						switch (status.getSeverity()) {
						case IStatus.CANCEL:
						case IStatus.INFO:
							createDialog(shell, title, status.getMessage(), MessageDialog.INFORMATION).open();
							break;
						case IStatus.WARNING:
							createDialog(shell, title, status.getMessage(), MessageDialog.WARNING).open();
							break;
						case IStatus.ERROR:
						default:
							createDialog(shell, title, status.getMessage(), MessageDialog.ERROR).open();
							break;
						}
					}
				});
			} catch (Throwable t) {
				status.getException().printStackTrace();
			}
		}
	}

	private MessageDialog createDialog(Shell shell, String title, String message, int type) {
		return new MessageDialog(shell, title, null, message, type, new String[] { IDialogConstants.OK_LABEL }, 0);
	}

}
