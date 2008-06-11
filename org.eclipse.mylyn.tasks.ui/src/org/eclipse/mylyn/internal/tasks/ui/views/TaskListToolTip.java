/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
/**
 * Copied from newsgroup, forwarded from Make Technologies
 */

package org.eclipse.mylyn.internal.tasks.ui.views;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.mylyn.commons.core.DateUtil;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.provisional.commons.ui.ScalingHyperlink;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.internal.tasks.core.DateRange;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.ScheduledTaskContainer;
import org.eclipse.mylyn.internal.tasks.core.TaskCategory;
import org.eclipse.mylyn.internal.tasks.core.UncategorizedTaskContainer;
import org.eclipse.mylyn.internal.tasks.core.UnmatchedTaskContainer;
import org.eclipse.mylyn.internal.tasks.ui.AbstractTaskListFilter;
import org.eclipse.mylyn.internal.tasks.ui.ITasksUiPreferenceConstants;
import org.eclipse.mylyn.internal.tasks.ui.TaskHyperlink;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.notifications.TaskDataDiff;
import org.eclipse.mylyn.internal.tasks.ui.notifications.TaskListNotification;
import org.eclipse.mylyn.internal.tasks.ui.notifications.TaskListNotifier;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryElement;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskContainer;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.ITask.SynchronizationState;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.TaskElementLabelProvider;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/**
 * @author Mik Kersten
 * @author Eric Booth
 * @author Leo Dos Santos - multi-monitor support
 * @author Steffen Pingel
 */
public class TaskListToolTip extends ToolTip {

	private final static int MAX_TEXT_WIDTH = 300;

	private final static int X_SHIFT;

	static {
		if ("gtk".equals(SWT.getPlatform()) || "carbon".equals(SWT.getPlatform())) {
			X_SHIFT = -26;
		} else {
			X_SHIFT = -23;
		}
	}

	private final static int Y_SHIFT = 1;

	private IRepositoryElement currentTipElement;

	private final List<TaskListToolTipListener> listeners = new ArrayList<TaskListToolTipListener>();

	private boolean visible;

	private boolean triggeredByMouse;

	private final Control control;

	public TaskListToolTip(Control control) {
		super(control);

		this.control = control;
		setShift(new Point(1, 1));
	}

	public void dispose() {
		hide();
	}

	@Override
	protected void afterHideToolTip(Event event) {
		triggeredByMouse = true;
		visible = false;
		for (TaskListToolTipListener listener : listeners.toArray(new TaskListToolTipListener[0])) {
			listener.toolTipHidden(event);
		}
	}

	public void addTaskListToolTipListener(TaskListToolTipListener listener) {
		listeners.add(listener);
	}

	public void removeTaskListToolTipListener(TaskListToolTipListener listener) {
		listeners.remove(listener);
	}

	private IRepositoryElement getTaskListElement(Object hoverObject) {
		if (hoverObject instanceof ScalingHyperlink) {
			TaskHyperlink hyperlink = (TaskHyperlink) hoverObject;
			return hyperlink.getTask();
		} else if (hoverObject instanceof Widget) {
			Object data = ((Widget) hoverObject).getData();
			if (data != null) {
				if (data instanceof ITaskContainer) {
					return (IRepositoryElement) data;
				} else if (data instanceof IAdaptable) {
					return (IRepositoryElement) ((IAdaptable) data).getAdapter(AbstractTaskContainer.class);
				}
			}
		}
		return null;
	}

	private String getTitleText(IRepositoryElement element) {
		if (element instanceof ScheduledTaskContainer) {
			StringBuilder sb = new StringBuilder();
			sb.append(element.getSummary());
			Calendar start = ((ScheduledTaskContainer) element).getDateRange().getStartDate();
			sb.append("  [");
			sb.append(DateFormat.getDateInstance(DateFormat.LONG).format(start.getTime()));
			sb.append("]");
			return sb.toString();
		} else if (element instanceof IRepositoryQuery) {
			IRepositoryQuery query = (IRepositoryQuery) element;
			StringBuilder sb = new StringBuilder();
			sb.append(element.getSummary());
			sb.append("  [");
			sb.append(getRepositoryLabel(query.getConnectorKind(), query.getRepositoryUrl()));
			sb.append("]");
			return sb.toString();
		} else {
			return new TaskElementLabelProvider(false).getText(element);
		}
	}

	private String getDetailsText(IRepositoryElement element) {
		if (element instanceof ScheduledTaskContainer) {
			ScheduledTaskContainer container = (ScheduledTaskContainer) element;
			int estimateTotal = 0;
			long elapsedTotal = 0;
			for (ITask child : container.getChildren()) {
				if (child instanceof AbstractTask) {
					estimateTotal += ((AbstractTask) child).getEstimatedTimeHours();
					elapsedTotal += TasksUiPlugin.getTaskActivityManager().getElapsedTime(child,
							container.getDateRange());
				}
			}
			StringBuilder sb = new StringBuilder();
			sb.append("Estimate: ");
			sb.append(estimateTotal);
			sb.append(" hours");
			sb.append("\n");
			sb.append("Elapsed: ");
			sb.append(DateUtil.getFormattedDurationShort(elapsedTotal));
			sb.append("\n");
			return sb.toString();
		} else if (element instanceof ITask) {
			ITask task = (ITask) element;
			StringBuilder sb = new StringBuilder();
			AbstractRepositoryConnectorUi connectorUi = TasksUiPlugin.getConnectorUi(task.getConnectorKind());
			if (connectorUi != null) {
				sb.append(connectorUi.getTaskKindLabel(task));
			}
			String key = task.getTaskKey();
			if (key != null) {
				sb.append(" ");
				sb.append(key);
			}
			sb.append(", ");
			sb.append(task.getPriority());
			sb.append("  [");
			sb.append(getRepositoryLabel(task.getConnectorKind(), task.getRepositoryUrl()));
			sb.append("]");
			sb.append("\n");
			return sb.toString();
		} else {
			return null;
		}
	}

	private String getRepositoryLabel(String repositoryKind, String repositoryUrl) {
		TaskRepository repository = TasksUi.getRepositoryManager().getRepository(repositoryKind, repositoryUrl);
		if (repository != null) {
			String label = repository.getRepositoryLabel();
			if (label.indexOf("//") != -1) {
				return label.substring((repository.getRepositoryUrl().indexOf("//") + 2));
			}
			return label + "";
		}
		return "";
	}

	private String getActivityText(IRepositoryElement element) {
//		if (element instanceof ScheduledTaskDelegate) {
//			ScheduledTaskDelegate task = (ScheduledTaskDelegate) element;
//
//			StringBuilder sb = new StringBuilder();
//			Date date = task.getScheduledForDate();
//			if (date != null) {
//				sb.append("Scheduled for: ");
//				sb.append(new SimpleDateFormat("E").format(date)).append(", ");
//				sb.append(DateFormat.getDateInstance(DateFormat.LONG).format(date));
//				sb.append(" (").append(DateFormat.getTimeInstance(DateFormat.SHORT).format(date)).append(")\n");
//			}
//
//			long elapsed = TasksUiPlugin.getTaskActivityManager().getElapsedTime(task.getCorrespondingTask(),
//					task.getDateRangeContainer().getStart(), task.getDateRangeContainer().getEnd());
//			String elapsedTimeString = DateUtil.getFormattedDurationShort(elapsed);
//			sb.append("Elapsed: ");
//			sb.append(elapsedTimeString);
//			sb.append("\n");
//
//			return sb.toString();
//		} else 
//			
		if (element instanceof ITask) {
			AbstractTask task = (AbstractTask) element;

			StringBuilder sb = new StringBuilder();

			Date dueDate = task.getDueDate();
			if (dueDate != null) {
				sb.append("Due: ");
				sb.append(new SimpleDateFormat("E").format(dueDate)).append(", ");
				sb.append(DateFormat.getDateInstance(DateFormat.LONG).format(dueDate));
				sb.append(" (").append(DateFormat.getTimeInstance(DateFormat.SHORT).format(dueDate)).append(')');
				sb.append('\n');
			}

			DateRange scheduledDate = task.getScheduledForDate();
			if (scheduledDate != null) {
				sb.append("Scheduled: ");
				sb.append(scheduledDate.toString());
//				sb.append(new SimpleDateFormat("E").format(scheduledDate)).append(", ");
//				sb.append(DateFormat.getDateInstance(DateFormat.LONG).format(scheduledDate));
				sb.append('\n');
			}

			long elapsed = TasksUiPlugin.getTaskActivityManager().getElapsedTime(task);
			String elapsedTimeString = DateUtil.getFormattedDurationShort(elapsed);
			sb.append("Elapsed: ");
			sb.append(elapsedTimeString);
			sb.append("\n");

			return sb.toString();
		}
		return null;
	}

	@SuppressWarnings( { "deprecation", "restriction" })
	private String getIncommingText(IRepositoryElement element) {
		if (element instanceof ITask) {
			ITask task = (ITask) element;
			if (task.getSynchronizationState().isIncoming()) {
				String text = null;
				AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
						task.getConnectorKind());
				if (connector instanceof org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractLegacyRepositoryConnector) {
					TaskListNotification notification = org.eclipse.mylyn.internal.tasks.ui.LegacyChangeManager.getIncommingNotification(
							connector, task);
					if (notification != null) {
						text = notification.getDescription();
					}
				} else {
					TaskListNotifier notifier = new TaskListNotifier(TasksUiPlugin.getRepositoryModel(),
							TasksUiPlugin.getTaskDataManager());
					TaskDataDiff diff = notifier.getDiff(task);
					if (diff != null) {
						text = diff.toString(MAX_TEXT_WIDTH);
					}
				}
				if (text != null && text.length() > 0) {
					return text;
				}
			}
		}
		return null;
	}

	private String getStatusText(IRepositoryElement element) {
		IStatus status = null;
		if (element instanceof AbstractTask) {
			AbstractTask task = (AbstractTask) element;
			status = task.getStatus();
		} else if (element instanceof IRepositoryQuery) {
			RepositoryQuery query = (RepositoryQuery) element;
			status = query.getStatus();
		}

		if (status != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(status.getMessage());
			if (status instanceof RepositoryStatus && ((RepositoryStatus) status).isHtmlMessage()) {
				sb.append(" Please synchronize manually for full error message.");
			}
			return sb.toString();
		}

		return null;
	}

	@Override
	public Point getLocation(Point tipSize, Event event) {
		Widget widget = getTipWidget(event);
		if (widget != null) {
			Rectangle bounds = getBounds(widget);
			if (bounds != null) {
				return control.toDisplay(bounds.x + X_SHIFT, bounds.y + bounds.height + Y_SHIFT);
			}
		}
		return super.getLocation(tipSize, event);//control.toDisplay(event.x + xShift, event.y + yShift);
	}

	private ProgressData getProgressData(IRepositoryElement element) {
		if (element instanceof ITask) {
			return null;
		} else if (element instanceof ITaskContainer) {
			Object[] children = new Object[0];

			children = ((ITaskContainer) element).getChildren().toArray();

			int total = children.length;
			int completed = 0;
			for (ITask task : ((ITaskContainer) element).getChildren()) {
				if (task.isCompleted()) {
					completed++;
				}
			}

			String text = "Total: " + total + " (Complete: " + completed + ", Incomplete: " + (total - completed) + ")";
			return new ProgressData(completed, total, text);
		} else {
			return null;
		}
	}

	private Image getImage(IRepositoryElement element) {
		if (element instanceof IRepositoryQuery) {
			IRepositoryQuery query = (IRepositoryQuery) element;
			AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
					query.getConnectorKind());
			if (connector != null) {
				return TasksUiPlugin.getDefault().getBrandingIcon(connector.getConnectorKind());
			}
		} else if (element instanceof ITask) {
			ITask repositoryTask = (ITask) element;
			AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
					repositoryTask.getConnectorKind());
			if (connector != null) {
				return TasksUiPlugin.getDefault().getBrandingIcon(connector.getConnectorKind());
			}
		} else if (element instanceof ScheduledTaskContainer) {
			return CommonImages.getImage(CommonImages.CALENDAR);
		}
		return null;
	}

	protected Widget getTipWidget(Event event) {
		Point widgetPosition = new Point(event.x, event.y);
		Widget widget = event.widget;
		if (widget instanceof ToolBar) {
			ToolBar w = (ToolBar) widget;
			return w.getItem(widgetPosition);
		}
		if (widget instanceof Table) {
			Table w = (Table) widget;
			return w.getItem(widgetPosition);
		}
		if (widget instanceof Tree) {
			Tree w = (Tree) widget;
			return w.getItem(widgetPosition);
		}

		return widget;
	}

	private Rectangle getBounds(Widget widget) {
		if (widget instanceof ToolItem) {
			ToolItem w = (ToolItem) widget;
			return w.getBounds();
		}
		if (widget instanceof TableItem) {
			TableItem w = (TableItem) widget;
			return w.getBounds();
		}
		if (widget instanceof TreeItem) {
			TreeItem w = (TreeItem) widget;
			return w.getBounds();
		}
		return null;
	}

	@Override
	protected boolean shouldCreateToolTip(Event event) {
		currentTipElement = null;

		if (super.shouldCreateToolTip(event)) {
			Widget tipWidget = getTipWidget(event);
			if (tipWidget != null) {
				Rectangle bounds = getBounds(tipWidget);
				if (tipWidget instanceof ScalingHyperlink) {
					currentTipElement = getTaskListElement(tipWidget);
				} else if (bounds != null && control.getBounds().contains(bounds.x, bounds.y)) {
					currentTipElement = getTaskListElement(tipWidget);
				}
			}
		}

		if (currentTipElement == null) {
			hide();
			return false;
		} else {
			return true;
		}
	}

	@Override
	protected Composite createToolTipContentArea(Event event, Composite parent) {
		assert currentTipElement != null;

		Composite composite = createToolTipContentAreaComposite(parent);

		addIconAndLabel(composite, getImage(currentTipElement), getTitleText(currentTipElement));

		String detailsText = getDetailsText(currentTipElement);
		if (detailsText != null) {
			addIconAndLabel(composite, null, detailsText);
		}

		String synchText = getSynchText(currentTipElement);
		if (synchText != null) {
			addIconAndLabel(composite, CommonImages.getImage(TasksUiImages.REPOSITORY_SYNCHRONIZE), synchText);
		}

		String activityText = getActivityText(currentTipElement);
		if (activityText != null) {
			addIconAndLabel(composite, CommonImages.getImage(CommonImages.CALENDAR), activityText);
		}

		String incommingText = getIncommingText(currentTipElement);
		if (incommingText != null) {
			Image image = CommonImages.getImage(CommonImages.OVERLAY_SYNC_INCOMMING);
			if (currentTipElement instanceof ITask) {
				ITask task = (ITask) currentTipElement;
				if (task.getSynchronizationState() == SynchronizationState.INCOMING_NEW) {
					image = CommonImages.getImage(CommonImages.OVERLAY_SYNC_INCOMMING_NEW);
				} else if (task.getSynchronizationState() == SynchronizationState.OUTGOING_NEW) {
					image = CommonImages.getImage(CommonImages.OVERLAY_SYNC_OUTGOING_NEW);
				}
			}
			addIconAndLabel(composite, image, incommingText);
		}

		ProgressData progress = getProgressData(currentTipElement);
		if (progress != null) {
			addIconAndLabel(composite, null, progress.text);

			// label height need to be set to 0 to remove gap below the progress bar 
			Label label = new Label(composite, SWT.NONE);
			GridData labelGridData = new GridData(SWT.FILL, SWT.TOP, true, false);
			labelGridData.heightHint = 0;
			label.setLayoutData(labelGridData);

			Composite progressComposite = new Composite(composite, SWT.NONE);
			GridLayout progressLayout = new GridLayout(1, false);
			progressLayout.marginWidth = 0;
			progressLayout.marginHeight = 0;
			progressComposite.setLayout(progressLayout);
			progressComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

			WorkweekProgressBar taskProgressBar = new WorkweekProgressBar(progressComposite);
			taskProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			taskProgressBar.reset(progress.completed, progress.total);

			// do we really need custom canvas? code below renders the same
//			IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
//			Color color = themeManager.getCurrentTheme().getColorRegistry().get(
//					TaskListColorsAndFonts.THEME_COLOR_TASK_TODAY_COMPLETED);
//			ProgressBar bar = new ProgressBar(tipShell, SWT.SMOOTH);
//			bar.setForeground(color);
//			bar.setSelection((int) (100d * progress.completed / progress.total));
//			GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
//			gridData.heightHint = 5;
//			bar.setLayoutData(gridData);
		}

		String statusText = getStatusText(currentTipElement);
		if (statusText != null) {
			addIconAndLabel(composite, CommonImages.getImage(CommonImages.WARNING), statusText);
		}

		String helpText = getHelpText(currentTipElement);
		if (helpText != null) {
			addIconAndLabel(composite, CommonImages.getImage(CommonImages.QUESTION), helpText);
		}

		visible = true;

		return composite;
	}

	private String getHelpText(IRepositoryElement element) {
		if (element instanceof TaskCategory || element instanceof IRepositoryQuery) {
			if (AbstractTaskListFilter.hasDescendantIncoming((ITaskContainer) element)) {
				TaskListView taskListView = TaskListView.getFromActivePerspective();
				if (taskListView != null) {

					if (!taskListView.isFocusedMode()
							&& TasksUiPlugin.getDefault().getPreferenceStore().getBoolean(
									ITasksUiPreferenceConstants.FILTER_COMPLETE_MODE)) {
						Object[] children = ((TaskListContentProvider) taskListView.getViewer().getContentProvider()).getChildren(element);
						boolean hasIncoming = false;
						for (Object child : children) {
							if (child instanceof ITask) {
								if (((ITask) child).getSynchronizationState().isIncoming()) {
									hasIncoming = true;
									break;
								}
							}
						}
						if (!hasIncoming) {
							return "Some incoming elements may be filtered,\nfocus the view to see all incomings";
						}
					}
				}
			}
			// if has incoming but no top level children have incoming, suggest incoming tasks may be filtered
		}
		if (element instanceof UncategorizedTaskContainer) {
			return "Automatic container for all local tasks\nwith no category set";
		} else if (element instanceof UnmatchedTaskContainer) {
			return "Automatic container for repository tasks\nnot matched by any query";
		}
		return null;
	}

	protected Composite createToolTipContentAreaComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginWidth = 5;
		gridLayout.marginHeight = 2;
		composite.setLayout(gridLayout);
		composite.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		return composite;
	}

	private String getSynchText(IRepositoryElement element) {
		if (element instanceof IRepositoryQuery) {
			String syncStamp = ((RepositoryQuery) element).getLastSynchronizedTimeStamp();
			if (syncStamp != null) {
				return "Synchronized: " + syncStamp;
			}
		}
		return null;
	}

	private String removeTrailingNewline(String text) {
		if (text.endsWith("\n")) {
			return text.substring(0, text.length() - 1);
		}
		return text;
	}

	protected void addIconAndLabel(Composite parent, Image image, String text) {
		Label imageLabel = new Label(parent, SWT.NONE);
		imageLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		imageLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		imageLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));
		imageLabel.setImage(image);

		Label textLabel = new Label(parent, SWT.NONE);
		textLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		textLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		textLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));
		textLabel.setText(removeTrailingNewline(text));
	}

	private static class ProgressData {

		int completed;

		int total;

		String text;

		public ProgressData(int completed, int total, String text) {
			this.completed = completed;
			this.total = total;
			this.text = text;
		}

	}

	public static interface TaskListToolTipListener {

		void toolTipHidden(Event event);

	}

	public boolean isVisible() {
		return visible;
	}

	public boolean isTriggeredByMouse() {
		return triggeredByMouse;
	}

	@Override
	public void show(Point location) {
		super.show(location);
		triggeredByMouse = false;
	}

}
