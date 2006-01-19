package org.eclipse.mylar.tasklist.ui.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylar.core.util.MylarStatusHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Waits for the title from the browser
 * 
 * @author Wesley Coelho
 * @author Mik Kersten
 */
public abstract class RetrieveTitleFromUrlJob extends Job implements TitleListener {

	public static final String LABEL_TITLE = "Retrieving description from URL";

	private final static long MAX_WAIT_TIME_MILLIS = 1000 * 10; // (10 Seconds)

	private final static long SLEEP_INTERVAL_MILLIS = 500;

	private String taskURL = null;

	private String pageTitle = null;

	private boolean retrievalFailed = false;

	private long timeWaitedMillis = 0;

	boolean ignoreChangeCall = false;

	private boolean titleRetrieved = false;

	public RetrieveTitleFromUrlJob(String url) {
		super(LABEL_TITLE);
		taskURL = url;
	}

	protected abstract void setTitle(String pageTitle);

	@Override
	public IStatus run(IProgressMonitor monitor) {

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				final Shell shell = new Shell(Display.getDefault());
				shell.setVisible(false);
				Browser browser = new Browser(shell, SWT.NONE);
				browser.addTitleListener(RetrieveTitleFromUrlJob.this);
				browser.setUrl(taskURL);
			}
		});

		while (pageTitle == null && !retrievalFailed && (timeWaitedMillis <= MAX_WAIT_TIME_MILLIS)) {
			try {
				Thread.sleep(SLEEP_INTERVAL_MILLIS);
			} catch (InterruptedException e) {
				MylarStatusHandler.fail(e, "Thread interrupted during sleep", false);
			}
			timeWaitedMillis += SLEEP_INTERVAL_MILLIS;
		}

		if (pageTitle != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					setTitle(pageTitle);
					titleRetrieved = true;
				}
			});
			return Status.OK_STATUS;
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Task Description Error",
							"Could not retrieve a description from the specified web page.");
				}
			});
			return Status.CANCEL_STATUS;
		}

	}

	public void changed(TitleEvent event) {
		if (!ignoreChangeCall) {
			if (event.title.equals(taskURL)) {
				return;
			} else {
				ignoreChangeCall = true;
				if (event.title.equals(taskURL + "/") || event.title.equals("Object not found!")
						|| event.title.equals("No page to display") || event.title.equals("Cannot find server")
						|| event.title.equals("Invalid Bug ID")) {
					retrievalFailed = true;
				} else {
					pageTitle = event.title;
				}
			}
		}
	}

	public boolean isTitleRetrieved() {
		return titleRetrieved;
	}

	public String getPageTitle() {
		return pageTitle;
	}
}