/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.trac.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.internal.trac.core.TracAttributeFactory.Attribute;
import org.eclipse.mylyn.internal.trac.core.model.TracAttachment;
import org.eclipse.mylyn.internal.trac.core.model.TracComment;
import org.eclipse.mylyn.internal.trac.core.model.TracTicket;
import org.eclipse.mylyn.internal.trac.core.model.TracTicketField;
import org.eclipse.mylyn.internal.trac.core.model.TracTicket.Key;
import org.eclipse.mylyn.internal.trac.core.util.TracUtils;
import org.eclipse.mylyn.tasks.core.AbstractAttributeFactory;
import org.eclipse.mylyn.tasks.core.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.RepositoryAttachment;
import org.eclipse.mylyn.tasks.core.RepositoryOperation;
import org.eclipse.mylyn.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskComment;
import org.eclipse.mylyn.tasks.core.TaskRepository;

/**
 * @author Steffen Pingel
 */
public class TracTaskDataHandler extends AbstractTaskDataHandler {

	private static final String CC_DELIMETER = ", ";

	private AbstractAttributeFactory attributeFactory = new TracAttributeFactory();

	private TracRepositoryConnector connector;

	public TracTaskDataHandler(TracRepositoryConnector connector) {
		this.connector = connector;
	}

	@Override
	public RepositoryTaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor)
			throws CoreException {
		return downloadTaskData(repository, TracRepositoryConnector.getTicketId(taskId));
	}

	public RepositoryTaskData downloadTaskData(TaskRepository repository, int id) throws CoreException {
		if (!TracRepositoryConnector.hasRichEditor(repository)) {
			// offline mode is only supported for XML-RPC
			return null;
		}

		try {
			RepositoryTaskData data = new RepositoryTaskData(attributeFactory, TracCorePlugin.REPOSITORY_KIND,
					repository.getUrl(), id + "");
			ITracClient client = connector.getClientManager().getRepository(repository);
			client.updateAttributes(new NullProgressMonitor(), false);
			TracTicket ticket = client.getTicket(id);
			createDefaultAttributes(attributeFactory, data, client, true);
			updateTaskData(repository, attributeFactory, data, ticket);
			return data;
		} catch (Exception e) {
			throw new CoreException(TracCorePlugin.toStatus(e, repository));
		}
	}

	@Override
	public AbstractAttributeFactory getAttributeFactory(String repositoryUrl, String repositoryKind, String taskKind) {
		// we don't care about the repository information right now
		return attributeFactory;
	}

	@Override
	public AbstractAttributeFactory getAttributeFactory(RepositoryTaskData taskData) {
		return getAttributeFactory(taskData.getRepositoryUrl(), taskData.getRepositoryKind(), taskData.getTaskKind());
	}

	public static void updateTaskData(TaskRepository repository, AbstractAttributeFactory factory,
			RepositoryTaskData data, TracTicket ticket) {
		if (ticket.getCreated() != null) {
			data.setAttributeValue(Attribute.TIME.getTracKey(), TracUtils.toTracTime(ticket.getCreated()) + "");
		}
		
		Date lastChanged = ticket.getLastChanged();

		Map<String, String> valueByKey = ticket.getValues();
		for (String key : valueByKey.keySet()) {
			if (Key.CC.getKey().equals(key)) {
				StringTokenizer t = new StringTokenizer(valueByKey.get(key), CC_DELIMETER);
				while (t.hasMoreTokens()) {
					data.addAttributeValue(key, t.nextToken());
				}
			} else {
				data.setAttributeValue(key, valueByKey.get(key));
			}
		}

		TracComment[] comments = ticket.getComments();
		if (comments != null) {
			for (int i = 0; i < comments.length; i++) {
				if (!"comment".equals(comments[i].getField()) || "".equals(comments[i].getNewValue())) {
					continue;
				}

				TaskComment taskComment = new TaskComment(factory, data.getComments().size() + 1);
				taskComment.setAttributeValue(RepositoryTaskAttribute.COMMENT_AUTHOR, comments[i].getAuthor());
				taskComment.setAttributeValue(RepositoryTaskAttribute.COMMENT_DATE, comments[i].getCreated().toString());
				taskComment.setAttributeValue(RepositoryTaskAttribute.COMMENT_TEXT, comments[i].getNewValue());
				data.addComment(taskComment);
			}
		}

		TracAttachment[] attachments = ticket.getAttachments();
		if (attachments != null) {
			for (int i = 0; i < attachments.length; i++) {
				RepositoryAttachment taskAttachment = new RepositoryAttachment(factory);
				taskAttachment.setCreator(attachments[i].getAuthor());
				taskAttachment.setRepositoryKind(TracCorePlugin.REPOSITORY_KIND);
				taskAttachment.setRepositoryUrl(repository.getUrl());
				taskAttachment.setTaskId("" + ticket.getId());
				taskAttachment.setAttributeValue(Attribute.DESCRIPTION.getTracKey(), attachments[i].getDescription());
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_FILENAME,
						attachments[i].getFilename());
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.USER_OWNER, attachments[i].getAuthor());
				if (attachments[i].getCreated() != null) {
					if (lastChanged == null || attachments[i].getCreated().after(lastChanged)) {
						lastChanged = attachments[i].getCreated(); 
					}
					
					taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_DATE, attachments[i].getCreated()
							.toString());
				}
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_URL, repository.getUrl()
						+ ITracClient.TICKET_ATTACHMENT_URL + ticket.getId() + "/" + attachments[i].getFilename());
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_ID, i + "");
				data.addAttachment(taskAttachment);
			}
		}

		String[] actions = ticket.getActions();
		if (actions != null) {
			// add operations in a defined order
			List<String> actionList = new ArrayList<String>(Arrays.asList(actions));
			addOperation(repository, data, ticket, actionList, "leave");
			addOperation(repository, data, ticket, actionList, "accept");
			addOperation(repository, data, ticket, actionList, "resolve");
			addOperation(repository, data, ticket, actionList, "reassign");
			addOperation(repository, data, ticket, actionList, "reopen");
		}

		if (lastChanged != null) {
			data.setAttributeValue(Attribute.CHANGE_TIME.getTracKey(), TracUtils.toTracTime(lastChanged) + "");
		}
	}

	// TODO Reuse Labels from BugzillaServerFacade
	private static void addOperation(TaskRepository repository, RepositoryTaskData data, TracTicket ticket,
			List<String> actions, String action) {
		if (!actions.remove(action)) {
			return;
		}

		RepositoryOperation operation = null;
		if ("leave".equals(action)) {
			operation = new RepositoryOperation(action, "Leave as " + data.getStatus() + " " + data.getResolution());
			operation.setChecked(true);
		} else if ("accept".equals(action)) {
			operation = new RepositoryOperation(action, "Accept");
		} else if ("resolve".equals(action)) {
			operation = new RepositoryOperation(action, "Resolve as");
			operation.setUpOptions("resolution");
			for (String resolution : ticket.getResolutions()) {
				operation.addOption(resolution, resolution);
			}
		} else if ("reassign".equals(action)) {
			operation = new RepositoryOperation(action, "Reassign to");
			operation.setInputName("owner");
			operation.setInputValue(TracRepositoryConnector.getDisplayUsername(repository));
		} else if ("reopen".equals(action)) {
			operation = new RepositoryOperation(action, "Reopen");
		}

		if (operation != null) {
			data.addOperation(operation);
		}
	}

	public static void createDefaultAttributes(AbstractAttributeFactory factory, RepositoryTaskData data,
			ITracClient client, boolean existingTask) {
		TracTicketField[] fields = client.getTicketFields();

		if (existingTask) {
			createAttribute(factory, data, Attribute.STATUS, client.getTicketStatus());
			createAttribute(factory, data, Attribute.RESOLUTION, client.getTicketResolutions());
		}

		createAttribute(factory, data, Attribute.COMPONENT, client.getComponents());
		createAttribute(factory, data, Attribute.VERSION, client.getVersions(), true);
		createAttribute(factory, data, Attribute.PRIORITY, client.getPriorities());
		createAttribute(factory, data, Attribute.SEVERITY, client.getSeverities());

		createAttribute(factory, data, Attribute.TYPE, client.getTicketTypes());
		if (existingTask) {
			createAttribute(factory, data, Attribute.OWNER);
		}
		createAttribute(factory, data, Attribute.MILESTONE, client.getMilestones(), true);
		if (existingTask) {
			createAttribute(factory, data, Attribute.REPORTER);
		}

		if (existingTask) {
			createAttribute(factory, data, Attribute.NEW_CC);
		}
		createAttribute(factory, data, Attribute.CC);
		createAttribute(factory, data, Attribute.KEYWORDS);

		if (!existingTask) {
			createAttribute(factory, data, Attribute.SUMMARY);
			createAttribute(factory, data, Attribute.DESCRIPTION);
		}

		if (fields != null) {
			for (TracTicketField field : fields) {
				if (field.isCustom()) {
					createAttribute(data, field);
				}
			}
		}
	}

	private static void createAttribute(RepositoryTaskData data, TracTicketField field) {
		RepositoryTaskAttribute attr = new RepositoryTaskAttribute(field.getName(), field.getLabel(), false);
		if (field.getType() == TracTicketField.Type.CHECKBOX) {
			// attr.addOption("True", "1");
			// attr.addOption("False", "0");
			attr.addOption("1", "1");
			attr.addOption("0", "0");

			if (field.getDefaultValue() != null) {
				attr.setValue(field.getDefaultValue());
			}
		} else if (field.getType() == TracTicketField.Type.SELECT || field.getType() == TracTicketField.Type.RADIO) {
			String[] values = field.getOptions();
			if (values != null && values.length > 0) {
				if (field.isOptional()) {
					attr.addOption("", "");
				}
				for (int i = 0; i < values.length; i++) {
					attr.addOption(values[i], values[i]);
				}

				if (field.getDefaultValue() != null) {
					try {
						int index = Integer.parseInt(field.getDefaultValue());
						if (index > 0 && index < values.length) {
							attr.setValue(values[index]);
						}
					} catch (NumberFormatException e) {
						for (int i = 0; i < values.length; i++) {
							if (field.getDefaultValue().equals(values[i].toString())) {
								attr.setValue(values[i]);
								break;
							}
						}
					}
				}
			}
		} else {
			if (field.getDefaultValue() != null) {
				attr.setValue(field.getDefaultValue());
			}
		}
		data.addAttribute(attr.getId(), attr);
	}

	private static RepositoryTaskAttribute createAttribute(AbstractAttributeFactory factory, RepositoryTaskData data,
			Attribute attribute, Object[] values, boolean allowEmtpy) {
		RepositoryTaskAttribute attr = factory.createAttribute(attribute.getTracKey());
		if (values != null && values.length > 0) {
			if (allowEmtpy) {
				attr.addOption("", "");
			}
			for (int i = 0; i < values.length; i++) {
				attr.addOption(values[i].toString(), values[i].toString());
			}
		} else {
			// attr.setHidden(true);
			attr.setReadOnly(true);
		}
		data.addAttribute(attribute.getTracKey(), attr);
		return attr;
	}

	private static RepositoryTaskAttribute createAttribute(AbstractAttributeFactory factory, RepositoryTaskData data,
			Attribute attribute) {
		RepositoryTaskAttribute attr = factory.createAttribute(attribute.getTracKey());
		data.addAttribute(attribute.getTracKey(), attr);
		return attr;
	}

	private static RepositoryTaskAttribute createAttribute(AbstractAttributeFactory factory, RepositoryTaskData data,
			Attribute attribute, Object[] values) {
		return createAttribute(factory, data, attribute, values, false);
	}

	@Override
	public String postTaskData(TaskRepository repository, RepositoryTaskData taskData, IProgressMonitor monitor)
			throws CoreException {
		try {
			TracTicket ticket = TracRepositoryConnector.getTracTicket(repository, taskData);
			ITracClient server = connector.getClientManager().getRepository(repository);
			if (taskData.isNew()) {
				int id = server.createTicket(ticket);
				return id + "";
			} else {
				server.updateTicket(ticket, taskData.getNewComment());
				return null;
			}
		} catch (Exception e) {
			throw new CoreException(TracCorePlugin.toStatus(e, repository));
		}
	}

	@Override
	public boolean initializeTaskData(TaskRepository repository, RepositoryTaskData data, IProgressMonitor monitor)
			throws CoreException {
		try {
			ITracClient client = connector.getClientManager().getRepository(repository);
			client.updateAttributes(new NullProgressMonitor(), false);
			createDefaultAttributes(attributeFactory, data, client, false);
			return true;
		} catch (Exception e) {
			throw new CoreException(TracCorePlugin.toStatus(e, repository));
		}
	}

	@Override
	public Set<String> getSubTaskIds(RepositoryTaskData taskData) {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

}
