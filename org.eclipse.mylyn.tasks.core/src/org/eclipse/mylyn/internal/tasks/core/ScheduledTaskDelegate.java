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

package org.eclipse.mylyn.internal.tasks.core;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskContainer;

/**
 * @author Rob Elves
 * @author Mik Kersten
 */
public class ScheduledTaskDelegate extends AbstractTask {

	private AbstractTask task = null;

	private ScheduledTaskContainer parent;

	private long startMili = 0;

	private long endMili = 0;

	private long activity = 0;

	public ScheduledTaskDelegate(ScheduledTaskContainer parent, AbstractTask task, Calendar start, Calendar end) {
		this(parent, task, start, end, 0);
	}

	public ScheduledTaskDelegate(ScheduledTaskContainer parent, AbstractTask task, Calendar start, Calendar end,
			long activity) {
		super(task.getRepositoryUrl(), task.getTaskId(), task.getSummary());
		this.task = task;
		if (start != null) {
			this.startMili = start.getTimeInMillis();
		}
		if (end != null) {
			this.endMili = end.getTimeInMillis();
		}
		this.parent = parent;
		this.activity = activity;
	}

	public long getEnd() {
		return endMili;
	}

	public long getStart() {
		return startMili;
	}

	public long getActivity() {
		return activity;
	}

	public AbstractTask getCorrespondingTask() {
		return task;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((task == null) ? 0 : task.hashCode());
		result = PRIME * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ScheduledTaskDelegate other = (ScheduledTaskDelegate) obj;
		if (task == null) {
			if (other.task != null)
				return false;
		} else if (!task.equals(other.task))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}

	public ScheduledTaskContainer getDateRangeContainer() {
		return parent;
	}

	@Override
	public Set<AbstractTaskContainer> getParentContainers() {
		return task.getParentContainers();
	}

	@Override
	public Set<AbstractTask> getChildren() {
		return task.getChildren();
	}

	@Override
	public Date getCompletionDate() {
		return task.getCompletionDate();
	}

	@Override
	public Date getCreationDate() {
		return task.getCreationDate();
	}

	@Override
	public String getSummary() {
		return task.getSummary();
	}

	@Override
	public int getEstimateTimeHours() {
		return task.getEstimateTimeHours();
	}

	@Override
	public String getTaskKind() {
		return task.getTaskKind();
	}

	@Override
	public String getNotes() {
		return task.getNotes();
	}

	@Override
	public String getPriority() {
		return task.getPriority();
	}

	@Override
	public Date getScheduledForDate() {
		return task.getScheduledForDate();
	}

	@Override
	public String getUrl() {
		return task.getUrl();
	}

	@Override
	public boolean isReminded() {
		return task.isReminded();
	}

	@Override
	public boolean hasValidUrl() {
		return task.hasValidUrl();
	}

	@Override
	public boolean isActive() {
		return task.isActive();
	}

	@Override
	public boolean isCompleted() {
		return task.isCompleted();
	}

	@Override
	public boolean isPastReminder() {
		return task.isPastReminder();
	}

	@Override
	public void setActive(boolean active) {
		task.setActive(active);
	}

	@Override
	public void addParentContainer(AbstractTaskContainer container) {
		task.addParentContainer(container);
	}

	@Override
	public void removeParentContainer(AbstractTaskContainer container) {
		task.removeParentContainer(container);
	}

	@Override
	public void setCompleted(boolean completed) {
		task.setCompleted(completed);
	}

	@Override
	public void setCompletionDate(Date date) {
		task.setCompletionDate(date);
	}

	@Override
	public void setCreationDate(Date date) {
		task.setCreationDate(date);
	}

	@Override
	public void setEstimatedTimeHours(int estimated) {
		task.setEstimatedTimeHours(estimated);
	}

	// public void setHandleIdentifier(String taskId) {
	// task.setHandleIdentifier(taskId);
	// }

	@Override
	public void setTaskKind(String kind) {
		task.setTaskKind(kind);
	}

	@Override
	public void setNotes(String notes) {
		task.setNotes(notes);
	}

	@Override
	public void setPriority(String priority) {
		task.setPriority(priority);
	}

	@Override
	public void setReminded(boolean reminded) {
		task.setReminded(reminded);
	}

	@Override
	public void setScheduledForDate(Date date) {
		task.setScheduledForDate(date);
	}

	@Override
	public void setUrl(String url) {
		task.setUrl(url);
	}

	@Override
	public int compareTo(AbstractTaskContainer taskListElement) {
		return task.toString().compareTo(((AbstractTask) taskListElement).toString());
	}

	@Override
	public void setSummary(String description) {
		task.setSummary(description);
	}

	@Override
	public Date getDueDate() {
		return task.getDueDate();
	}

	@Override
	public void setDueDate(Date date) {
		task.setDueDate(date);
	}

	@Override
	public boolean isLocal() {
		return task.isLocal();
	}

	@Override
	public String getConnectorKind() {
		return task.getConnectorKind();
	}
}
