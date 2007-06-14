/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
/**
 * Copied from newsgroup, forwarded from Make Technologies
 */

package org.eclipse.mylyn.internal.tasks.ui.views;

import java.net.URL;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.mylyn.internal.tasks.core.ScheduledTaskContainer;
import org.eclipse.mylyn.internal.tasks.ui.ITaskListNotification;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.monitor.core.DateUtil;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.AbstractTask.RepositoryTaskSyncState;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * @author Mik Kersten
 * @author Eric Booth
 * @author Leo Dos Santos - multi-monitor support
 */
public class TaskListToolTipHandler {

	private static final String SEPARATOR = "\n\n";

	private static final String UNITS_HOURS = " hours";

	private static final String NO_MINUTES = "0 minutes";

	private Shell tipShell;

	private Label tipLabelImage;

	private Label tipLabelText;

	private Label scheduledTipLabelImage;

	private Label scheduledTipLabelText;

	private Label incommingTipLabelImage;

	private Label incommingTipLabelText;

	private WorkweekProgressBar taskProgressBar;

	private Widget tipWidget;

	protected Point tipPosition;

	protected Point widgetPosition;

	public TaskListToolTipHandler(Shell parentShell) {
		if (parentShell != null) {
			tipShell = createTipShell(parentShell, null, true, true);
		}
	}

	private Shell createTipShell(Shell parent, Widget widget, boolean showScheduled, boolean showIncomming) {
		Shell tipShell = new Shell(parent.getDisplay(), SWT.TOOL | SWT.NO_FOCUS | SWT.MODELESS | SWT.ON_TOP);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginWidth = 2;
		gridLayout.marginHeight = 2;
		tipShell.setLayout(gridLayout);
		tipShell.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		tipLabelImage = new Label(tipShell, SWT.NONE);
		tipLabelImage.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		tipLabelImage.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		GridData imageGridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		tipLabelImage.setLayoutData(imageGridData);

		tipLabelText = new Label(tipShell, SWT.NONE);
		tipLabelText.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		tipLabelText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		GridData textGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		tipLabelText.setLayoutData(textGridData);

		if (showScheduled) {

			scheduledTipLabelImage = new Label(tipShell, SWT.NONE);
			scheduledTipLabelImage.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
			scheduledTipLabelImage.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

			imageGridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
			scheduledTipLabelImage.setLayoutData(imageGridData);

			scheduledTipLabelText = new Label(tipShell, SWT.NONE);
			scheduledTipLabelText.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
			scheduledTipLabelText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

			textGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
			scheduledTipLabelText.setLayoutData(textGridData);
		}

		if (showIncomming) {
			incommingTipLabelImage = new Label(tipShell, SWT.NONE);
			incommingTipLabelImage.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
			incommingTipLabelImage.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

			imageGridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
			incommingTipLabelImage.setLayoutData(imageGridData);

			incommingTipLabelText = new Label(tipShell, SWT.NONE);
			incommingTipLabelText.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
			incommingTipLabelText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

			textGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
			incommingTipLabelText.setLayoutData(textGridData);
		}

		AbstractTaskContainer element = getTaskListElement(widget);
		if (element instanceof AbstractTaskContainer) {
			Composite progressComposite = new Composite(tipShell, SWT.NONE);
			GridLayout progressLayout = new GridLayout(1, false);
			progressLayout.marginWidth = 2;
			progressLayout.marginHeight = 0;
			progressLayout.marginBottom = 2;
			progressLayout.horizontalSpacing = 0;
			progressLayout.verticalSpacing = 0;
			progressComposite.setLayout(progressLayout);
			progressComposite.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 4, 1));

			taskProgressBar = new WorkweekProgressBar(progressComposite);
			taskProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}

		return tipShell;
	}

	private String updateContainerProgressBar(WorkweekProgressBar taskProgressBar, Object object) {
		if (taskProgressBar != null && !taskProgressBar.isDisposed() && object instanceof AbstractTaskContainer) {
			String text = "";
			AbstractTaskContainer container = (AbstractTaskContainer) object;
						
			int total = container.getChildren().size();
			int completed = 0;
			for (AbstractTask task : container.getChildren()) {
				if (task.isCompleted()) {
					completed++;
				}
			}
			String suffix = "";
			if (container instanceof AbstractRepositoryQuery) {
				AbstractRepositoryQuery query = ((AbstractRepositoryQuery) container);
				total = 0;
				completed = 0;
				total += query.getHits().size();
				for (AbstractTask hit : query.getHits()) {
					if (hit.isCompleted()) {
						completed++;
					}
				}
				// suffix = " (query max: " + query.getMaxHits() + ")";
			}
			taskProgressBar.reset(completed, total);
			text += "Completed " + completed + " of " + total + suffix;
			return text;
		} else {
			return "";
		}
	}

	private AbstractTaskContainer getTaskListElement(Object hoverObject) {
		if (hoverObject instanceof Widget) {
			Object data = ((Widget) hoverObject).getData();
			if (data != null) {
				if (data instanceof AbstractTaskContainer) {
					return (AbstractTaskContainer) data;
				} else if (data instanceof IAdaptable) {
					return (AbstractTaskContainer) ((IAdaptable) data).getAdapter(AbstractTaskContainer.class);
				}
			}
		}
		return null;
	}

	protected String getBasicToolTextTip(Object object) {
		AbstractTaskContainer element = getTaskListElement(object);
		String tooltip = "";
		String priority = "";

		if (element instanceof ScheduledTaskContainer) {
			ScheduledTaskContainer container = (ScheduledTaskContainer) element;
			tooltip += "Estimate: " + container.getTotalEstimated() + UNITS_HOURS;
			String elapsedTimeString = NO_MINUTES;
			try {
				elapsedTimeString = DateUtil.getFormattedDurationShort(container.getTotalElapsed());
				if (elapsedTimeString.equals("")) {
					elapsedTimeString = NO_MINUTES;
				}
			} catch (RuntimeException e) {
				// ignore
			}
			tooltip += "   Elapsed: " + elapsedTimeString + "\n";
			return tooltip;
		}

		if (element instanceof AbstractRepositoryQuery) {
			AbstractRepositoryQuery query = (AbstractRepositoryQuery) element;

			try {
				tooltip += new URL(query.getRepositoryUrl()).getHost();
			} catch (Exception e) {
				// ignore
			}

			String syncStamp = query.getLastRefreshTimeStamp();
			if (syncStamp != null) {
				tooltip += " (synched: " + syncStamp + ")\n";
			}
			if (query.getStatus() != null) {
				tooltip += "\n" + "Last Error: " + query.getStatus().getMessage();
				if (query.getStatus() instanceof RepositoryStatus && ((RepositoryStatus) query.getStatus()).isHtmlMessage()) {
					tooltip += " Please synchronize manually for full error message.";
				}
				tooltip += "\n";
			}
			return tooltip;
		}

		if (element instanceof AbstractTask) {

			AbstractTask repositoryTask = (AbstractTask) element;

			tooltip += (element).getSummary();
			if (repositoryTask != null) {

				String taskKindLabel = TasksUiPlugin.getRepositoryUi(repositoryTask.getRepositoryKind())
						.getTaskKindLabel(repositoryTask);

				tooltip += "\n" + taskKindLabel + ", " + repositoryTask.getPriority();

				TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(
						repositoryTask.getRepositoryKind(), repositoryTask.getRepositoryUrl());
				if (repository != null) {
					tooltip += "  [" + repository.getRepositoryLabel() + "]";
				}

				if (repositoryTask.getStatus() != null) {
					tooltip += SEPARATOR + "Last Error: " + repositoryTask.getStatus().getMessage();
				}
			}
			return tooltip;
		} else if (element != null) {
			tooltip += (element).getSummary();
			return tooltip + priority;
		} else if (object instanceof Control) {
			return (String) ((Control) object).getData("TIP_TEXT");
		}
		return null;
	}

	private String getActivityText(AbstractTaskContainer element) {

		if (element != null && element instanceof AbstractTask) {
			try {
				String result = "";
				Date date = ((AbstractTask) element).getScheduledForDate();
				if (date != null) {
					result += "Scheduled for: " + DateFormat.getDateInstance(DateFormat.LONG).format(date) + " ("
							+ DateFormat.getTimeInstance(DateFormat.SHORT).format(date) + ")\n";
				}

				long elapsed = TasksUiPlugin.getTaskListManager().getElapsedTime((AbstractTask) element);
				String elapsedTimeString = DateUtil.getFormattedDurationShort(elapsed);
				if (!elapsedTimeString.equals("")) {
					result += "Elapsed: " + elapsedTimeString + "\n";
				}
				return result;
			} catch (Exception e) {
				// ignore
			}
		}
		return null;
	}

	private String getIncommingText(AbstractTaskContainer element) {
		if (element instanceof AbstractTask) {

			AbstractTask repositoryTask = (AbstractTask) element;

			if (repositoryTask != null && repositoryTask.getSyncState() == RepositoryTaskSyncState.INCOMING) {
				AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
						repositoryTask);
				if (connector != null) {
					ITaskListNotification notification = TasksUiPlugin.getIncommingNotification(connector,
							repositoryTask);
					if (notification != null) {
						String descriptionText = null;
						if (notification.getDescription() != null) {
							descriptionText = notification.getDescription();
						}

						if (descriptionText != null && !descriptionText.equals("")) {
							return descriptionText;
						}
					}
				}
			}
		}
		return null;
	}

	protected Image getImage(Object object) {
		AbstractTaskContainer element = getTaskListElement(object);
		if (object instanceof Control) {
			return (Image) ((Control) object).getData("TIP_IMAGE");
		} else if (element instanceof AbstractRepositoryQuery) {
			AbstractRepositoryQuery query = (AbstractRepositoryQuery) element;
			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
					query.getRepositoryKind());
			if (connector != null) {
				return TasksUiPlugin.getDefault().getBrandingIcon(connector.getRepositoryType());
			}
		} else if (element instanceof AbstractTask) {
			AbstractTask repositoryTask = (AbstractTask) element;

			if (repositoryTask != null) {
				AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
						repositoryTask.getRepositoryKind());
				if (connector != null) {
					return TasksUiPlugin.getDefault().getBrandingIcon(connector.getRepositoryType());
				}
			}
		}
		return null;
	}

	protected Object getToolTipHelp(Object object) {
		if (object instanceof Control) {
			return ((Control) object).getData("TIP_HELPTEXT");
		}
		return null;
	}

	/**
	 * Enables customized hover help for a specified control
	 * 
	 * @control the control on which to enable hoverhelp
	 */
	public void activateHoverHelp(final Control control) {

		PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {

			public void windowActivated(IWorkbenchWindow window) {
				// ignore

			}

			public void windowClosed(IWorkbenchWindow window) {
				// ignore

			}

			public void windowDeactivated(IWorkbenchWindow window) {
				hideTooltip();

			}

			public void windowOpened(IWorkbenchWindow window) {
				// ignore

			}
		});

		/*
		 * Get out of the way if we attempt to activate the control underneath
		 * the tooltip
		 */
		control.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(MouseEvent e) {
				hideTooltip();
			}
		});
		/*
		 * Trap hover events to pop-up tooltip
		 */
		control.addMouseTrackListener(new MouseTrackAdapter() {

			@Override
			public void mouseExit(MouseEvent e) {
				// TODO: can these conditions be simplified? see bug 131776
				if (tipShell != null && !tipShell.isDisposed() && tipShell.getDisplay() != null
						&& !tipShell.getDisplay().isDisposed() && tipShell.isVisible()) {
					tipShell.setVisible(false);
				}
				tipWidget = null;
			}

			@Override
			public void mouseHover(MouseEvent event) {
				if (tipShell.isDisposed()) {
					return;
				}
				widgetPosition = new Point(event.x, event.y);
				Widget widget = event.widget;
				if (widget instanceof ToolBar) {
					ToolBar w = (ToolBar) widget;
					widget = w.getItem(widgetPosition);
				}
				if (widget instanceof Table) {
					Table w = (Table) widget;
					widget = w.getItem(widgetPosition);
				}
				if (widget instanceof Tree) {
					Tree w = (Tree) widget;
					widget = w.getItem(widgetPosition);
				}
				if (widget == null && !tipShell.isDisposed()) {
					tipShell.setVisible(false);
					tipWidget = null;
					return;
				}
				if (widget == tipWidget)
					return;
				tipWidget = widget;
				tipPosition = control.toDisplay(widgetPosition);
				String baseText = getBasicToolTextTip(widget);
				String scheduledText = getActivityText(getTaskListElement(widget));
				String incommingText = getIncommingText(getTaskListElement(widget));

				Image repositoryImage = getImage(widget);
				Image activityImage = TasksUiImages.getImage(TasksUiImages.CALENDAR); // TODO
																						// Fixme
				Image incommingImage = TasksUiImages.getImage(TasksUiImages.OVERLAY_INCOMMING);

				if (baseText == null) { // HACK: don't check length
					return;
				}

				if (!tipShell.isDisposed() && tipShell.getShell() != null
						&& PlatformUI.getWorkbench().getDisplay().getActiveShell() != null) {
					tipShell.close();
					tipShell = createTipShell(PlatformUI.getWorkbench().getDisplay().getActiveShell(), widget,
							scheduledText != null, incommingText != null);
				}
				
				AbstractTaskContainer element = getTaskListElement(widget);
				String progressText = updateContainerProgressBar(taskProgressBar, element);

				String dateText = "";
				if (element instanceof ScheduledTaskContainer) {
					Calendar start = ((ScheduledTaskContainer)element).getStart();
					dateText += DateFormat.getDateInstance(DateFormat.LONG).format(start.getTime()) + "\n"; 
				}
				
				tipLabelText.setText(dateText + baseText + progressText);
				tipLabelImage.setImage(repositoryImage); // accepts null

				if (scheduledText != null) {
					scheduledTipLabelText.setText(scheduledText);
					scheduledTipLabelImage.setImage(activityImage); // accepts
																	// null
				}

				if (incommingText != null) {
					incommingTipLabelText.setText(incommingText);
					incommingTipLabelImage.setImage(incommingImage); // accepts
																		// null
				}

				tipShell.pack();
				setHoverLocation(tipShell, tipPosition);
				tipShell.setVisible(true);
			}
		});
	}

	/**
	 * Sets the location for a hovering shell
	 * 
	 * @param shell
	 *            the object that is to hover
	 * @param position
	 *            the position of a widget to hover over
	 * @return the top-left location for a hovering box
	 */
	private void setHoverLocation(Shell shell, Point position) {
		Rectangle displayBounds = shell.getMonitor().getClientArea();
		Rectangle shellBounds = shell.getBounds();

		// We need to find the exact monitor we're mousing over
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=166990
		Monitor[] array = PlatformUI.getWorkbench().getDisplay().getMonitors();
		for (Monitor m : array) {
			Rectangle monitorBounds = m.getBounds();
			if ((position.x >= monitorBounds.x) && (position.x < (monitorBounds.x + monitorBounds.width))
					&& (position.y >= monitorBounds.y) && (position.y < (monitorBounds.y + monitorBounds.height))) {
				displayBounds = m.getClientArea();
			}
		}

		if ((position.x + shellBounds.width) > (displayBounds.x + displayBounds.width))
			shellBounds.x = displayBounds.x + displayBounds.width - shellBounds.width;
		else
			shellBounds.x = position.x;
		
		if ((position.y + 10 + shellBounds.height) > (displayBounds.y + displayBounds.height))
			shellBounds.y = displayBounds.y + displayBounds.height - shellBounds.height;
		else
			shellBounds.y = position.y + 10;

		shell.setBounds(shellBounds);
	}

	private void hideTooltip() {
		if (tipShell != null && !tipShell.isDisposed() && tipShell.isVisible())
			tipShell.setVisible(false);
	}
}
