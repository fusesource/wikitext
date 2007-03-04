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

package org.eclipse.mylar.internal.tasks.ui.views;

import java.net.URL;
import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.AbstractTaskContainer;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskListElement;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
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
 */
public class TaskListToolTipHandler {

	// private static final String SEPARATOR = "\n---------------\n";
	private static final String SEPARATOR = "\n\n";

	private Shell tipShell;

	private Label tipLabelImage;

	private Label tipLabelText;

	private WorkweekProgressBar taskProgressBar;

	private Widget tipWidget; // widget this tooltip is hovering over

	protected Point tipPosition; // the position being hovered over on the

	protected Point widgetPosition; // the position hovered over in the Widget;

	public TaskListToolTipHandler(Shell parentShell) {
		if (parentShell != null) {
			tipShell = createTipShell(parentShell, null);
		}
	}

	private Shell createTipShell(Shell parent, Widget widget) {
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

		ITaskListElement element = getTaskListElement(widget);
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
			AbstractTaskContainer container = (AbstractTaskContainer) object;
			int total = container.getChildren().size();
			int completed = 0;
			for (ITask task : container.getChildren()) {
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
				for (AbstractQueryHit hit : query.getHits()) {
					if (hit.isCompleted()) {
						completed++;
					}
				}
				suffix = "  (query max: " + query.getMaxHits() + ")";
			}
			taskProgressBar.reset(completed, total);
			return "Completed " + completed + " of " + total + suffix;
		} else {
			return "";
		}
	}

	private ITaskListElement getTaskListElement(Object hoverObject) {
		if (hoverObject instanceof Widget) {
			Object data = ((Widget) hoverObject).getData();
			if (data != null) {
				if (data instanceof ITaskListElement) {
					return (ITaskListElement) data;
				} else if (data instanceof IAdaptable) {
					return (ITaskListElement) ((IAdaptable) data).getAdapter(ITaskListElement.class);
				}
			}
		}
		return null;
	}

	protected String getToolTipText(Object object) {
		ITaskListElement element = getTaskListElement(object);
		String tooltip = "";
		String priority = "";
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
				tooltip += "\n" + "Last Error: " + query.getStatus().getMessage()+"\n";
			}

			// Set<AbstractQueryHit> hits = query.getHits(); // FIXME provide
			// getHitsSize()
			// method
			// if (hits.size() == 1) {
			// tooltip += "1 hit";
			// } else {
			// tooltip += hits.size() + " hits";
			// }
			// if (query.getMaxHits() != -1) {
			// tooltip += " (max set to: " + query.getMaxHits() + ")";
			// }
			return tooltip;
		}

		if (element instanceof AbstractRepositoryTask || element instanceof AbstractQueryHit) {
			AbstractRepositoryTask repositoryTask;
			if (element instanceof AbstractQueryHit) {
				repositoryTask = ((AbstractQueryHit) element).getCorrespondingTask();
			} else {
				repositoryTask = (AbstractRepositoryTask) element;
			}
			tooltip += (element).getSummary();
			if (repositoryTask != null) {

				TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(repositoryTask.getRepositoryKind(), repositoryTask.getRepositoryUrl());
				if(repository != null && repository.getRepositoryLabel() != null && !repository.getRepositoryLabel().equals("")) {
					tooltip += "\n" + repository.getRepositoryLabel();
				} else {
					tooltip += "\n" + repositoryTask.getRepositoryUrl(); 	
				}
				
				tooltip += formatScheduledFor(repositoryTask);

				if (repositoryTask.getStatus() != null) {
					tooltip += SEPARATOR + "Last Error: " + repositoryTask.getStatus().getMessage();
				}
			}
			return tooltip;
		} else if (element != null) {
			tooltip += (element).getSummary() + formatScheduledFor(element);
			return tooltip + priority;
		} else if (object instanceof Control) {
			return (String) ((Control) object).getData("TIP_TEXT");
		}
		return null;
	}

	private String formatScheduledFor(ITaskListElement element) {
		if (element instanceof ITask) {
			Date date = ((ITask) element).getScheduledForDate();
			if (date != null) {
				return SEPARATOR + "Scheduled for: " 
					    + DateFormat.getDateInstance(DateFormat.FULL).format(date)
						+ ", " + DateFormat.getTimeInstance(DateFormat.SHORT).format(date) + "";
			}
		}
		return "";
	}

	// private String formatLastRefreshTime(Date lastRefresh) {
	// String toolTip = "Last synchronized: ";
	// if (lastRefresh == null)
	// return toolTip += "unknown";
	// Date timeNow = new Date();
	// long timeDifference = (timeNow.getTime() - lastRefresh.getTime()) /
	// 60000;
	// long minutes = timeDifference % 60;
	// timeDifference /= 60;
	// long hours = timeDifference % 24;
	// timeDifference /= 24;
	// if (timeDifference > 0) {
	// toolTip += timeDifference + ((timeDifference == 1) ? " day, " : " days,
	// ");
	// }
	// if (hours > 0 || timeDifference > 0) {
	// toolTip += hours + ((hours == 1) ? " hour, " : " hours, ");
	// }
	// toolTip += minutes + ((minutes == 1) ? " minute " : " minutes ") + "ago";
	// return toolTip;
	// }

	protected Image getToolTipImage(Object object) {
		ITaskListElement element = getTaskListElement(object);
		if (object instanceof Control) {
			return (Image) ((Control) object).getData("TIP_IMAGE");
		} else if (element instanceof AbstractRepositoryQuery) {
			AbstractRepositoryQuery query = (AbstractRepositoryQuery) element;
			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
					query.getRepositoryKind());
			if (connector != null) {
				return TasksUiPlugin.getDefault().getBrandingIcon(connector.getRepositoryType());
			}
		} else if (element instanceof AbstractRepositoryTask || element instanceof AbstractQueryHit) {
			AbstractRepositoryTask repositoryTask;
			if (element instanceof AbstractQueryHit) {
				repositoryTask = ((AbstractQueryHit) element).getCorrespondingTask();
			} else {
				repositoryTask = (AbstractRepositoryTask) element;
			}
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
				String text = getToolTipText(widget);
				Image image = getToolTipImage(widget);
				if (text == null) { // HACK: don't check length
					return;
				}

				if (!tipShell.isDisposed() && tipShell.getShell() != null
						&& PlatformUI.getWorkbench().getDisplay().getActiveShell() != null) {
					tipShell.close();
					tipShell = createTipShell(PlatformUI.getWorkbench().getDisplay().getActiveShell(), widget);
				}

				String progressText = updateContainerProgressBar(taskProgressBar, getTaskListElement(widget));

				tipLabelText.setText(text + progressText);
				tipLabelImage.setImage(image); // accepts null

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
		Rectangle displayBounds = shell.getDisplay().getBounds();
		Rectangle shellBounds = shell.getBounds();
		shellBounds.x = Math.max(Math.min(position.x, displayBounds.width - shellBounds.width), 0);
		shellBounds.y = Math.max(Math.min(position.y + 10, displayBounds.height - shellBounds.height), 0);
		shell.setBounds(shellBounds);
	}

	private void hideTooltip() {
		if (tipShell != null && !tipShell.isDisposed() && tipShell.isVisible())
			tipShell.setVisible(false);
	}
}

// /*
// * Trap F1 Help to pop up a custom help box
// */
// control.addHelpListener(new HelpListener() {
// public void helpRequested(HelpEvent event) {
// if (tipWidget == null)
// return;
// Object help = getToolTipHelp(tipWidget);
// if (help == null)
// return;
// if (help.getClass() != String.class) {
// return;
// }
// if (tipShell.isVisible()) {
// tipShell.setVisible(false);
// Shell helpShell = new Shell(parentShell, SWT.SHELL_TRIM);
// helpShell.setLayout(new FillLayout());
// Label label = new Label(helpShell, SWT.NONE);
// label.setText((String) help);
// helpShell.pack();
// setHoverLocation(helpShell, tipPosition);
// helpShell.open();
// }
// }
// });
