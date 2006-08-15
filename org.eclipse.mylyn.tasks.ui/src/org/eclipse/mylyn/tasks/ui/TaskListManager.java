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
/*
 * Created on Dec 26, 2004
 */
package org.eclipse.mylar.tasks.ui;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.mylar.context.core.ContextCorePlugin;
import org.eclipse.mylar.context.core.IMylarContext;
import org.eclipse.mylar.context.core.IMylarContextListener;
import org.eclipse.mylar.context.core.IMylarElement;
import org.eclipse.mylar.context.core.InteractionEvent;
import org.eclipse.mylar.context.core.MylarStatusHandler;
import org.eclipse.mylar.internal.context.core.MylarContextManager;
import org.eclipse.mylar.internal.context.core.util.TimerThread;
import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.internal.tasks.ui.util.TaskActivityTimer;
import org.eclipse.mylar.internal.tasks.ui.util.TaskListWriter;
import org.eclipse.mylar.internal.tasks.ui.views.TaskActivationHistory;
import org.eclipse.mylar.monitor.MylarMonitorPlugin;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.DateRangeActivityDelegate;
import org.eclipse.mylar.tasks.core.DateRangeContainer;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskActivityListener;
import org.eclipse.mylar.tasks.core.TaskList;
import org.eclipse.mylar.tasks.core.TaskRepository;

/**
 * TODO: clean-up
 * 
 * @author Mik Kersten
 * @author Rob Elves (task activity)
 */
public class TaskListManager implements IPropertyChangeListener {

	private static final long SECOND = 1000;

	private static final long MINUTE = 60 * SECOND;

	private static final long ROLLOVER_DELAY = 30 * MINUTE;

	private static final int NUM_WEEKS_PREVIOUS = -1;

	private static final int NUM_WEEKS_NEXT = 1;

	private static final int NUM_WEEKS_FUTURE_START = 2;

	private static final int NUM_WEEKS_FUTURE_END = 8;

	private static final int NUM_WEEKS_PAST_START = -8;

	private static final int NUM_WEEKS_PAST_END = -2;

	public static final String ARCHIVE_CATEGORY_DESCRIPTION = "Archive";

	private static final String DESCRIPTION_THIS_WEEK = "This Week";

	private static final String DESCRIPTION_PREVIOUS_WEEK = "Previous Week";

	private static final String DESCRIPTION_NEXT_WEEK = "Next Week";

	private static final String DESCRIPTION_FUTURE = "Future";

	private static final String DESCRIPTION_PAST = "Past";

	public static final String[] ESTIMATE_TIMES = new String[] { "0 Hours", "1 Hours", "2 Hours", "3 Hours", "4 Hours",
			"5 Hours", "6 Hours", "7 Hours", "8 Hours", "9 Hours", "10 Hours" };

	private DateRangeContainer activityThisWeek;

	private DateRangeContainer activityNextWeek;

	private DateRangeContainer activityPreviousWeek;

	private DateRangeContainer activityFuture;

	private DateRangeContainer activityPast;

	private boolean isInactive;

	private long startInactive;

	private long totalInactive;

	private ArrayList<DateRangeContainer> dateRangeContainers = new ArrayList<DateRangeContainer>();

	private Set<ITask> tasksWithReminders = new HashSet<ITask>();

	private ITask currentTask = null;

	private String currentHandle = "";

	private Calendar currentTaskStart = null;

	private Calendar currentTaskEnd = null;

	private Map<ITask, TaskActivityTimer> timerMap = new HashMap<ITask, TaskActivityTimer>();

	private List<ITaskActivityListener> activityListeners = new ArrayList<ITaskActivityListener>();

	private TaskListWriter taskListWriter;

	private File taskListFile;

	private TaskList taskList = new TaskList();

	private TaskActivationHistory taskActivationHistory = new TaskActivationHistory();
	
	private boolean taskListInitialized = false;

	private boolean taskActivityHistoryInitialized = false;

//	private int nextLocalTaskId;

	private int timerSleepInterval = TimerThread.DEFAULT_SLEEP_INTERVAL;

	private int startDay;

	private int endDay;

	private int scheduledStartHour;

	private int scheduledEndHour;

	private Timer timer;

	/** public for testing */
	public Date startTime = new Date();

	private final IMylarContextListener CONTEXT_LISTENER = new IMylarContextListener() {

		public void contextActivated(IMylarContext context) {
			parseTaskActivityInteractionHistory();
		}

		public void contextDeactivated(IMylarContext context) {
			// ignore
		}

		public void presentationSettingsChanging(UpdateKind kind) {
			// ignore
		}

		public void presentationSettingsChanged(UpdateKind kind) {
			// ignore
		}

		public void interestChanged(List<IMylarElement> elements) {
			List<InteractionEvent> events = ContextCorePlugin.getContextManager().getActivityHistoryMetaContext()
					.getInteractionHistory();
			InteractionEvent event = events.get(events.size() - 1);
			parseInteractionEvent(event);
		}

		public void nodeDeleted(IMylarElement element) {
			// ignore
		}

		public void landmarkAdded(IMylarElement element) {
			// ignore
		}

		public void landmarkRemoved(IMylarElement element) {
			// ignore
		}

		public void edgesChanged(IMylarElement element) {
			// ignore
		}
	};

	public TaskListManager(TaskListWriter taskListWriter, File file) {
		this.taskListFile = file;
		this.taskListWriter = taskListWriter;
		
		timer = new Timer();
		timer.schedule(new RolloverCheck(), ROLLOVER_DELAY, ROLLOVER_DELAY);
	}
	
	public void init() {
		ContextCorePlugin.getContextManager().addActivityMetaContextListener(CONTEXT_LISTENER);		
	}
	
	public void dispose() {
		ContextCorePlugin.getContextManager().removeActivityMetaContextListener(CONTEXT_LISTENER);
	}

	public TaskList resetTaskList() {
		resetActivity();
		taskList.reset();
		taskListInitialized = true;
		return taskList;
	}

	private void resetActivity() {
		dateRangeContainers.clear();
		setupCalendarRanges();
	}

	private void parseTaskActivityInteractionHistory() {
		if (!TasksUiPlugin.getTaskListManager().isTaskListInitialized()) {
			return;
		}
		List<InteractionEvent> events = ContextCorePlugin.getContextManager().getActivityHistoryMetaContext()
				.getInteractionHistory();
		for (InteractionEvent event : events) {
			parseInteractionEvent(event);
		}
		taskActivityHistoryInitialized = true;
	}

	private void parseFutureReminders() {
		activityFuture.clear();
		activityNextWeek.clear();
		HashSet<ITask> toRemove = new HashSet<ITask>();
		toRemove.addAll(activityThisWeek.getChildren());
		for (ITask activity : toRemove) {
			DateRangeActivityDelegate delegate = (DateRangeActivityDelegate) activity;
			Calendar calendar = GregorianCalendar.getInstance();
			if (delegate.getReminderDate() != null) {
				calendar.setTime(delegate.getReminderDate());
				if (!activityThisWeek.includes(calendar) && activityThisWeek.getElapsed(delegate) == 0) {
					activityThisWeek.remove(delegate);
				}
			} else {
				if (activityThisWeek.getElapsed(delegate) == 0) {
					activityThisWeek.remove(delegate);
				}
			}
		}
		GregorianCalendar tempCalendar = new GregorianCalendar();
		tempCalendar.setFirstDayOfWeek(startDay);
		for (ITask task : tasksWithReminders) {
			if (task.getReminderDate() != null) {
				tempCalendar.setTime(task.getReminderDate());
				if (activityNextWeek.includes(tempCalendar)) {
					activityNextWeek.addTask(new DateRangeActivityDelegate(activityNextWeek, task, tempCalendar,
							tempCalendar));
				} else if (activityFuture.includes(tempCalendar)) {
					activityFuture.addTask(new DateRangeActivityDelegate(activityFuture, task, tempCalendar,
							tempCalendar));
				} else if (activityThisWeek.includes(tempCalendar) && !activityThisWeek.getChildren().contains(task)) {
					activityThisWeek.addTask(new DateRangeActivityDelegate(activityThisWeek, task, tempCalendar,
							tempCalendar));
				}
			}
		}
	}

	/** public for testing * */
	public void parseInteractionEvent(InteractionEvent event) {
		if (event.getDelta().equals(MylarContextManager.ACTIVITY_DELTA_ACTIVATED)) {
			if (!event.getStructureHandle().equals(MylarContextManager.ACTIVITY_HANDLE_ATTENTION)) {
				if (isInactive) {
					isInactive = false;
					totalInactive = 0;
					startInactive = 0;
				}
				currentTask = TasksUiPlugin.getTaskListManager().getTaskList()
						.getTask(event.getStructureHandle());
				if (currentTask != null) {
					GregorianCalendar calendar = new GregorianCalendar();
					calendar.setFirstDayOfWeek(startDay);
					calendar.setTime(event.getDate());
					currentTaskStart = calendar;
					currentHandle = event.getStructureHandle();
				}
			} else if (event.getStructureHandle().equals(MylarContextManager.ACTIVITY_HANDLE_ATTENTION) && isInactive) {
				isInactive = false;
				totalInactive += event.getDate().getTime() - startInactive;
			}
		} else if (event.getDelta().equals(MylarContextManager.ACTIVITY_DELTA_DEACTIVATED)) {
			if (!event.getStructureHandle().equals(MylarContextManager.ACTIVITY_HANDLE_ATTENTION)
					&& currentHandle.equals(event.getStructureHandle())) {
				GregorianCalendar calendarEnd = new GregorianCalendar();
				calendarEnd.setFirstDayOfWeek(startDay);
				calendarEnd.setTime(event.getDate());
				calendarEnd.getTime();
				currentTaskEnd = calendarEnd;
				if (isInactive) {
					isInactive = false;
					totalInactive += event.getDate().getTime() - startInactive;
				}
				for (DateRangeContainer week : dateRangeContainers) {
					if (week.includes(currentTaskStart)) {
						if (currentTask != null) {
							week.addTask(new DateRangeActivityDelegate(week, currentTask, currentTaskStart,
									currentTaskEnd, totalInactive));
							if (taskActivityHistoryInitialized) {
								for (ITaskActivityListener listener : activityListeners) {
									listener.activityChanged(week);
								}
							}
						}
					}
				}
				currentTask = null;
				currentHandle = "";
				totalInactive = 0;
				startInactive = 0;
			} else if (event.getStructureHandle().equals(MylarContextManager.ACTIVITY_HANDLE_ATTENTION) && !isInactive) {
				if (!currentHandle.equals("")) {
					isInactive = true;
					startInactive = event.getDate().getTime();
				}
			}
		}
	}

	/** public for testing * */
	public DateRangeContainer getActivityThisWeek() {
		return activityThisWeek;
	}

	/** public for testing * */
	public DateRangeContainer getActivityPast() {
		return activityPast;
	}

	/** public for testing * */
	public DateRangeContainer getActivityFuture() {
		return activityFuture;
	}

	/** public for testing * */
	public DateRangeContainer getActivityNextWeek() {
		return activityNextWeek;
	}

	/** public for testing * */
	public DateRangeContainer getActivityPrevious() {
		return activityPreviousWeek;
	}

	private void setupCalendarRanges() {
		// MylarTaskListPlugin.getMylarCorePrefs().getInt(TaskListPreferenceConstants.PLANNING_STARTDAY);
		startDay = Calendar.MONDAY;
		// MylarTaskListPlugin.getMylarCorePrefs().getInt(TaskListPreferenceConstants.PLANNING_ENDDAY);
		endDay = Calendar.SUNDAY;
		scheduledStartHour = TasksUiPlugin.getDefault().getPreferenceStore().getInt(
				TaskListPreferenceConstants.PLANNING_STARTHOUR);
		scheduledEndHour = TasksUiPlugin.getDefault().getPreferenceStore().getInt(TaskListPreferenceConstants.PLANNING_ENDHOUR);

		GregorianCalendar currentBegin = new GregorianCalendar();
		currentBegin.setFirstDayOfWeek(startDay);
		currentBegin.setTime(startTime);
		snapToStartOfWeek(currentBegin);
		GregorianCalendar currentEnd = new GregorianCalendar();
		currentEnd.setFirstDayOfWeek(startDay);
		currentEnd.setTime(startTime);
		snapToEndOfWeek(currentEnd);
		activityThisWeek = new DateRangeContainer(currentBegin, currentEnd, DESCRIPTION_THIS_WEEK, taskList);
		dateRangeContainers.add(activityThisWeek);

		GregorianCalendar previousStart = new GregorianCalendar();
		previousStart.setFirstDayOfWeek(startDay);
		previousStart.setTime(startTime);
		previousStart.add(Calendar.WEEK_OF_YEAR, NUM_WEEKS_PREVIOUS);
		snapToStartOfWeek(previousStart);
		GregorianCalendar previousEnd = new GregorianCalendar();
		previousEnd.setFirstDayOfWeek(startDay);
		previousEnd.setTime(startTime);
		previousEnd.add(Calendar.WEEK_OF_YEAR, NUM_WEEKS_PREVIOUS);
		snapToEndOfWeek(previousEnd);
		activityPreviousWeek = new DateRangeContainer(previousStart.getTime(), previousEnd.getTime(),
				DESCRIPTION_PREVIOUS_WEEK, taskList);
		dateRangeContainers.add(activityPreviousWeek);

		GregorianCalendar nextStart = new GregorianCalendar();
		nextStart.setFirstDayOfWeek(startDay);
		nextStart.setTime(startTime);
		nextStart.add(Calendar.WEEK_OF_YEAR, NUM_WEEKS_NEXT);
		snapToStartOfWeek(nextStart);
		GregorianCalendar nextEnd = new GregorianCalendar();
		nextEnd.setFirstDayOfWeek(startDay);
		nextEnd.setTime(startTime);
		nextEnd.add(Calendar.WEEK_OF_YEAR, NUM_WEEKS_NEXT);
		snapToEndOfWeek(nextEnd);
		activityNextWeek = new DateRangeContainer(nextStart.getTime(), nextEnd.getTime(), DESCRIPTION_NEXT_WEEK,
				taskList);
		dateRangeContainers.add(activityNextWeek);

		GregorianCalendar futureStart = new GregorianCalendar();
		futureStart.setFirstDayOfWeek(startDay);
		futureStart.setTime(startTime);
		futureStart.add(Calendar.WEEK_OF_YEAR, NUM_WEEKS_FUTURE_START);
		snapToStartOfWeek(futureStart);
		GregorianCalendar futureEnd = new GregorianCalendar();
		futureEnd.setFirstDayOfWeek(startDay);
		futureEnd.setTime(startTime);
		futureEnd.add(Calendar.WEEK_OF_YEAR, NUM_WEEKS_FUTURE_END);
		snapToEndOfWeek(futureEnd);
		activityFuture = new DateRangeContainer(futureStart.getTime(), futureEnd.getTime(), DESCRIPTION_FUTURE,
				taskList);
		dateRangeContainers.add(activityFuture);

		GregorianCalendar pastStart = new GregorianCalendar();
		pastStart.setFirstDayOfWeek(startDay);
		pastStart.setTime(startTime);
		pastStart.add(Calendar.WEEK_OF_YEAR, NUM_WEEKS_PAST_START);
		snapToStartOfWeek(pastStart);
		GregorianCalendar pastEnd = new GregorianCalendar();
		pastEnd.setFirstDayOfWeek(startDay);
		pastEnd.setTime(startTime);
		pastEnd.add(Calendar.WEEK_OF_YEAR, NUM_WEEKS_PAST_END);
		snapToEndOfWeek(pastEnd);
		activityPast = new DateRangeContainer(pastStart.getTime(), pastEnd.getTime(), DESCRIPTION_PAST, taskList);
		dateRangeContainers.add(activityPast);
	}

	private void snapToStartOfWeek(GregorianCalendar cal) {
		cal.getTime();
		cal.set(Calendar.DAY_OF_WEEK, startDay);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.getTime();
	}

	private void snapToEndOfWeek(GregorianCalendar cal) {
		cal.getTime();
		cal.set(Calendar.DAY_OF_WEEK, endDay);
		cal.set(Calendar.HOUR_OF_DAY, cal.getMaximum(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, cal.getMaximum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getMaximum(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, cal.getMaximum(Calendar.MILLISECOND));
		cal.getTime();
	}

	public Calendar setSecheduledIn(Calendar calendar, int days) {
		calendar.add(Calendar.DAY_OF_MONTH, days);
		calendar.set(Calendar.HOUR_OF_DAY, scheduledStartHour);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}

	public Calendar setScheduledToday(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, scheduledEndHour);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}

	public Object[] getDateRanges() {
		// parseFutureReminders();
		return dateRangeContainers.toArray();
	}

	public String genUniqueTaskHandle() {
		int nextId = taskList.findLargestTaskHandle()+1;
		return TaskRepositoryManager.PREFIX_LOCAL + nextId;//nextLocalTaskId++;
	}

	public void refactorRepositoryUrl(String oldUrl, String newUrl) {
		if (oldUrl == null || newUrl == null || oldUrl.equals(newUrl)) {
			return;
		}
		List<ITask> activeTasks = taskList.getActiveTasks();
		for (ITask task : new ArrayList<ITask>(activeTasks)) {
			deactivateTask(task);
		}
		taskList.refactorRepositoryUrl(oldUrl, newUrl);

		File dataDir = new File(TasksUiPlugin.getDefault().getDataDirectory());
		if (dataDir.exists() && dataDir.isDirectory()) {
			for (File file : dataDir.listFiles()) {
				int dotIndex = file.getName().lastIndexOf('.');
				if (dotIndex != -1) {
					String storedHandle;
					try {
						storedHandle = URLDecoder.decode(file.getName().substring(0, dotIndex),
								MylarContextManager.CONTEXT_FILENAME_ENCODING);
						int delimIndex = storedHandle.lastIndexOf(AbstractRepositoryTask.HANDLE_DELIM);
						if (delimIndex != -1) {
							String storedUrl = storedHandle.substring(0, delimIndex);
							if (oldUrl.equals(storedUrl)) {
								String id = AbstractRepositoryTask.getTaskId(storedHandle);
								String newHandle = AbstractRepositoryTask.getHandle(newUrl, id);
								File newFile = ContextCorePlugin.getContextManager().getFileForContext(newHandle);
								file.renameTo(newFile);
							}
						}
					} catch (Exception e) {
						MylarStatusHandler.fail(e, "Could not move context file: " + file.getName(), false);
					}
				}
			}
		}
		saveTaskList();
	}

	public boolean readExistingOrCreateNewList() {	
		try {
			if (taskListFile.exists()) {
				taskListWriter.readTaskList(taskList, taskListFile);
			} else {
				resetTaskList();
			}

			for (ITask task : taskList.getAllTasks()) {
				if (task.getReminderDate() != null)// && task.hasBeenReminded()
					// != true
					tasksWithReminders.add(task);
			}
			resetActivity();
			parseFutureReminders();
			taskListInitialized = true;
			for (ITaskActivityListener listener : new ArrayList<ITaskActivityListener>(activityListeners)) {
				listener.taskListRead();
			}

			// only activate the first task to avoid confusion of mutliple
			// active tasks on startup
			if (taskList.getActiveTask() != null) {
				activateTask(taskList.getActiveTask());
			}
			parseTaskActivityInteractionHistory();
			taskActivationHistory.loadPersistentHistory();
		} catch (Exception e) {
			MylarStatusHandler.log(e, "Could not read task list");
			return false;
		}
		return true;
	}

	/**
	 * Will not save an empty task list to avoid losing data on bad startup.
	 */
	public void saveTaskList() {
		try {
			if (taskListInitialized) {
				taskListWriter.writeTaskList(taskList, taskListFile);
//				TasksUiPlugin.getDefault().getPreferenceStore().setValue(TaskListPreferenceConstants.TASK_ID,
//				nextLocalTaskId);
			} else {
				MylarStatusHandler.log("task list save attempted before initialization", this);
			}
		} catch (Exception e) {
			MylarStatusHandler.fail(e, "Could not save task list", true);
		}
	}

	public TaskList getTaskList() {
		return taskList;
	}

	public void addActivityListener(ITaskActivityListener listener) {
		activityListeners.add(listener);
	}

	public void removeActivityListener(ITaskActivityListener listener) {
		activityListeners.remove(listener);
	}


	public void activateTask(ITask task) {
		if (!TasksUiPlugin.getDefault().isMultipleActiveTasksMode()) {
			deactivateAllTasks();
		}

		try {
			int timeout = MylarMonitorPlugin.getDefault().getInactivityTimeout();
			TaskActivityTimer activityTimer = new TaskActivityTimer(task, timeout, timerSleepInterval);
			activityTimer.startTimer();
			timerMap.put(task, activityTimer);
			taskList.setActive(task, true);			
			for (ITaskActivityListener listener : new ArrayList<ITaskActivityListener>(activityListeners)) {
				listener.taskActivated(task);
			}
		} catch (Throwable t) {
			MylarStatusHandler.fail(t, "could not activate task", false);
		}
	}

	
	public void deactivateAllTasks() {
		// Make a copy to avoid modification on list being traversed; can result
		// in a ConcurrentModificationException
		List<ITask> activeTasks = new ArrayList<ITask>(taskList.getActiveTasks());
		for (ITask task : activeTasks) {
			deactivateTask(task);
		}
	}

	public void deactivateTask(ITask task) {
		if (task == null) {
			return;
		}	
		
		TaskActivityTimer taskTimer = timerMap.remove(task);
		if (taskTimer != null) {
			taskTimer.stopTimer();
		}
		if (task.isActive()) {
			taskList.setActive(task, false);			
			for (ITaskActivityListener listener : new ArrayList<ITaskActivityListener>(activityListeners)) {
				try {
					listener.taskDeactivated(task);
				} catch (Throwable t) {
					MylarStatusHandler.fail(t, "notification failed for: " + listener, false);
				}
			}
		}
	}

	public void setTaskListFile(File file) {
		this.taskListFile = file;
	}

	public boolean isTaskListInitialized() {
		return taskListInitialized;
	}

	public TaskListWriter getTaskListWriter() {
		return taskListWriter;
	}

	public File getTaskListFile() {
		return taskListFile;
	}

	/**
	 * Public for testing
	 */
	public Map<ITask, TaskActivityTimer> getTimerMap() {
		return timerMap;
	}

	/**
	 * For testing
	 */
	public void setTimerSleepInterval(int timerSleepInterval) {
		this.timerSleepInterval = timerSleepInterval;
	}

	public boolean isActiveThisWeek(ITask task) {
		for (ITask activityDelegateTask : activityThisWeek.getChildren()) {
			if (activityDelegateTask.getHandleIdentifier().equals(task.getHandleIdentifier())) {
				return true;
			}
		}
		return false;
	}

	public boolean isCompletedToday(ITask task) {
		if (task != null) {	
			
			if(task instanceof AbstractRepositoryTask) {
				AbstractRepositoryTask repositoryTask = (AbstractRepositoryTask)task;
				TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(repositoryTask.getRepositoryKind(), repositoryTask.getRepositoryUrl());
				if(repository != null && repositoryTask.getOwner() != null && !repositoryTask.getOwner().equals(repository.getUserName())) return false;
			}

			Date completionDate = task.getCompletionDate();
			if (completionDate != null) {
				Calendar tomorrow = Calendar.getInstance();
				TasksUiPlugin.getTaskListManager().setSecheduledIn(tomorrow, 1);

				Calendar yesterday = Calendar.getInstance();
				yesterday.set(Calendar.HOUR_OF_DAY, 0);
				yesterday.set(Calendar.MINUTE, 0);
				yesterday.set(Calendar.SECOND, 0);
				yesterday.set(Calendar.MILLISECOND, 0);

				return completionDate.compareTo(yesterday.getTime()) == 1
						&& completionDate.compareTo(tomorrow.getTime()) == -1;
			}
		}
		return false;
	}

	public boolean isReminderAfterThisWeek(ITask task) {
		if (task != null) {
			Date reminder = task.getReminderDate();
			if (reminder != null) {
				return reminder.compareTo(activityNextWeek.getStart().getTime()) > -1;
			}
		}
		return false;
	}

	public boolean isReminderLater(ITask task) {
		if (task != null) {
			Date reminder = task.getReminderDate();
			if (reminder != null) {
				return reminder.compareTo(activityFuture.getStart().getTime()) > -1;
			}
		}
		return false;
	}

	public boolean isReminderThisWeek(ITask task) {
		if (task != null) {
			Date reminder = task.getReminderDate();
			if (reminder != null) {
				Date now = new Date();
				return (reminder.compareTo(now) == 1 && reminder.compareTo(activityThisWeek.getEnd().getTime()) == -1);
			}
		}
		return false;
	}

	public boolean isReminderToday(ITask task) {
		if (task != null) {
			Date reminder = task.getReminderDate();
			if (reminder != null) {
				Date now = new Date();
				Calendar tomorrow = GregorianCalendar.getInstance();
				TasksUiPlugin.getTaskListManager().setSecheduledIn(tomorrow, 1);
				return (reminder.compareTo(now) == 1 && reminder.compareTo(tomorrow.getTime()) == -1);
			}
		}
		return false;
	}

	/**
	 * TODO: Need to migrate to use of this method for setting of reminders
	 */
	public void setReminder(ITask task, Date reminderDate) {
		task.setReminderDate(reminderDate);
		task.setReminded(false);
		if (reminderDate == null) {
			tasksWithReminders.remove(task);
		} else {
			tasksWithReminders.add(task);
		}
		parseFutureReminders();
		taskList.notifyLocalInfoChanged(task);
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(TaskListPreferenceConstants.PLANNING_STARTHOUR)
				|| event.getProperty().equals(TaskListPreferenceConstants.PLANNING_ENDHOUR)) {
			// event.getProperty().equals(TaskListPreferenceConstants.PLANNING_STARTDAY)
			scheduledStartHour = TasksUiPlugin.getDefault().getPreferenceStore().getInt(
					TaskListPreferenceConstants.PLANNING_STARTHOUR);
			scheduledEndHour = TasksUiPlugin.getDefault().getPreferenceStore().getInt(
					TaskListPreferenceConstants.PLANNING_ENDHOUR);
		}
	}

	/** public for testing */
	public void resetAndRollOver() {
		taskActivityHistoryInitialized = false;
		tasksWithReminders.clear();
		for (ITask task : taskList.getAllTasks()) {
			if (task.getReminderDate() != null) {
				tasksWithReminders.add(task);
			}
		}
		resetActivity();
		parseFutureReminders();
		parseTaskActivityInteractionHistory();
		for (ITaskActivityListener listener : activityListeners) {
			listener.calendarChanged();
		}
	}

	private class RolloverCheck extends TimerTask {

		@Override
		public void run() {
			if (!Platform.isRunning() || ContextCorePlugin.getDefault() == null) {
				return;
			} else {
				Calendar now = GregorianCalendar.getInstance();
				DateRangeContainer thisWeek = getActivityThisWeek();
				if (!thisWeek.includes(now)) {
					startTime = new Date();
					resetAndRollOver();
				}
			}
		}
	}

	
	public TaskActivationHistory getTaskActivationHistory() {
		return taskActivationHistory;
	}

}
