/*******************************************************************************
 * Copyright (c) 2007 - 2007 CodeGear and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylar.xplanner.ui;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;

import javax.security.auth.login.LoginException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylar.internal.tasks.core.RepositoryTaskHandleUtil;
import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.xplanner.core.XPlannerCorePlugin;
import org.eclipse.mylar.xplanner.core.service.XPlannerServer;
import org.xplanner.soap.IterationData;
import org.xplanner.soap.PersonData;
import org.xplanner.soap.ProjectData;
import org.xplanner.soap.TaskData;
import org.xplanner.soap.UserStoryData;

/**
 * @author Ravi Kumar
 * @author Helen Bershadskaya
 */
public class XPlannerRepositoryUtils {
	private XPlannerRepositoryUtils() {
		
	}
	
	public static RepositoryTaskData createRepositoryTaskData(TaskRepository repository, XPlannerTask xplannerTask, XPlannerServer server) throws CoreException {
		RepositoryTaskData repositoryTaskData = null;
		
		try {
			if (XPlannerTask.Kind.TASK.toString().equals(xplannerTask.getTaskKind())) {
				TaskData taskData = server.getTask(Integer.valueOf(xplannerTask.getKey()).intValue());
				repositoryTaskData = XPlannerRepositoryUtils.getXPlannerRepositoryTaskData(
						repository.getUrl(), taskData, RepositoryTaskHandleUtil.getTaskId(xplannerTask.getHandleIdentifier()));
				xplannerTask.setCompleted(taskData.isCompleted());
			}
			else if (XPlannerTask.Kind.USER_STORY.toString().equals(xplannerTask.getTaskKind())) {
				UserStoryData userStory = server.getUserStory(Integer.valueOf(xplannerTask.getKey()).intValue());
				repositoryTaskData = XPlannerRepositoryUtils.getXPlannerRepositoryTaskData(
						repository.getUrl(), userStory, RepositoryTaskHandleUtil.getTaskId(xplannerTask.getHandleIdentifier()));
				xplannerTask.setCompleted(userStory.isCompleted());
			}
		} 
		catch (final Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, XPlannerMylarUIPlugin.PLUGIN_ID, 0, 
				MessageFormat.format(Messages.XPlannerRepositoryUtils_TASK_DOWNLOAD_FAILED,
					  xplannerTask.getRepositoryUrl(), TasksUiPlugin.LABEL_VIEW_REPOSITORIES), e));
		}
		
		return repositoryTaskData;
	}

	public static RepositoryTaskData getXPlannerRepositoryTaskData(String repositoryUrl, TaskData taskData, String id) 
		throws IOException, MalformedURLException, LoginException, GeneralSecurityException, CoreException {

		RepositoryTaskData repositoryTaskData = new RepositoryTaskData(
				new XPlannerAttributeFactory(),
				XPlannerMylarUIPlugin.REPOSITORY_KIND, repositoryUrl, id, XPlannerTask.Kind.TASK.toString());
		
		setupTaskAttributes(taskData, repositoryTaskData);

		return repositoryTaskData;
	}

	public static RepositoryTaskData getXPlannerRepositoryTaskData(String repositoryUrl, UserStoryData userStory, String id) 
		throws IOException, MalformedURLException, LoginException, GeneralSecurityException, CoreException {

		RepositoryTaskData repositoryTaskData = new RepositoryTaskData(new XPlannerAttributeFactory(),
				XPlannerMylarUIPlugin.REPOSITORY_KIND, repositoryUrl, id, XPlannerTask.Kind.USER_STORY.toString());
		setupUserStoryAttributes(userStory, repositoryTaskData);
	
		return repositoryTaskData;
	}

	public static void setupTaskAttributes(TaskData taskData, RepositoryTaskData repositoryTaskData) 
		throws CoreException {
		
		TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(
				XPlannerMylarUIPlugin.REPOSITORY_KIND, repositoryTaskData.getRepositoryUrl());
		XPlannerServer server = XPlannerServerFacade.getDefault().getXPlannerServer(repository);

		// description
		repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.DESCRIPTION, taskData.getDescription());
		
		// priority
		repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.PRIORITY, 
			getPriorityFromXPlannerObject(taskData, server));
		
		// status
		repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.STATUS, taskData.getDispositionName());
		
		// summary
		repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.SUMMARY, taskData.getName());

		// assigned to 
		repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.USER_ASSIGNED, getPersonName(
			taskData.getAcceptorId(), server));
		
		// createdDate 
		Date createdDate = taskData.getCreatedDate().getTime();
		repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.DATE_CREATION, 
				XPlannerAttributeFactory.DATE_FORMAT.format(createdDate));
		
		// last updated
		Date lastUpdatedDate = taskData.getLastUpdateTime().getTime();
		if (lastUpdatedDate != null) {
			repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.DATE_MODIFIED, 
					XPlannerAttributeFactory.TIME_DATE_FORMAT.format(lastUpdatedDate));
		}
		
		// est time
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_EST_HOURS_NAME, "" + taskData.getEstimatedHours()); //$NON-NLS-1$

		// act time
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ACT_HOURS_NAME, "" + taskData.getActualHours()); //$NON-NLS-1$

		// act time
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_REMAINING_HOURS_NAME, "" + taskData.getRemainingHours()); //$NON-NLS-1$

		// est original hours
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ESTIMATED_ORIGINAL_HOURS_NAME, "" + taskData.getEstimatedOriginalHours()); //$NON-NLS-1$

		// est adjusted estimated hours
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ADJUSTED_ESTIMATED_HOURS_NAME, "" + taskData.getAdjustedEstimatedHours()); //$NON-NLS-1$

		// project name
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_PROJECT_NAME, getProjectName(taskData, server));

		// iteration name
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ITERATION_NAME, getIterationName(taskData, server));

		// user story name
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_USER_STORY_NAME, 
				getUserStoryName(taskData, server));

		// completed
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_TASK_COMPLETED, 
			taskData.isCompleted() ? "1" : "0");  //$NON-NLS-1$//$NON-NLS-2$
	}

	public static void setupUserStoryAttributes(UserStoryData userStory, RepositoryTaskData repositoryTaskData) 
		throws CoreException {
		
		TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(
				XPlannerMylarUIPlugin.REPOSITORY_KIND, repositoryTaskData.getRepositoryUrl());
		XPlannerServer server = XPlannerServerFacade.getDefault().getXPlannerServer(repository);

		// description
		repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.DESCRIPTION, userStory.getDescription());
		
		// priority
		repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.PRIORITY, 
			getPriorityFromXPlannerObject(userStory, server));
		
		// summary
		repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.SUMMARY, userStory.getName());

		// status
		repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.STATUS, userStory.getDispositionName());
		
		// assigned to 
		repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.USER_ASSIGNED, getPersonName(
				userStory.getTrackerId(), server));
		
		// createdDate -- user story doesn't have created date
		
		// last updated
		Date lastUpdatedDate = userStory.getLastUpdateTime().getTime();
		if (lastUpdatedDate != null) {
			repositoryTaskData.setAttributeValue(RepositoryTaskAttribute.DATE_MODIFIED, 
					XPlannerAttributeFactory.TIME_DATE_FORMAT.format(lastUpdatedDate));
		}
		
		// est time
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_EST_HOURS_NAME, "" + userStory.getEstimatedHours()); //$NON-NLS-1$

		// act time
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ACT_HOURS_NAME, "" + userStory.getActualHours()); //$NON-NLS-1$

		// est original hours
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ESTIMATED_ORIGINAL_HOURS_NAME, "" + userStory.getEstimatedOriginalHours()); //$NON-NLS-1$

		// act time
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_REMAINING_HOURS_NAME, "" + userStory.getRemainingHours()); //$NON-NLS-1$

		// est adjusted estimated hours
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ADJUSTED_ESTIMATED_HOURS_NAME, "" + userStory.getAdjustedEstimatedHours()); //$NON-NLS-1$

		// project name
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_PROJECT_NAME, getProjectName(userStory, server));

		// iteration name
		repositoryTaskData.setAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ITERATION_NAME, getIterationName(userStory, server));
	}
	
	public static String getProjectName(RepositoryTaskData repositoryTaskData) {
		return repositoryTaskData.getAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_PROJECT_NAME);
	}
	
	public static String getIterationName(RepositoryTaskData repositoryTaskData) {
		return repositoryTaskData.getAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ITERATION_NAME);
	}
	
	public static String getUserStoryName(RepositoryTaskData repositoryTaskData) {
		return repositoryTaskData.getAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_USER_STORY_NAME);
	}
	
	public static String getPersonName(int personId, XPlannerServer server) {
		String personName = Messages.XPlannerRepositoryUtils_NO_PERSON_NAME;

		try {
		  PersonData personData = server.getPerson(personId);
		  if (personData != null) {
		  	personName = personData.getName();
		  }
		} 
		catch (Exception e) { //RemoteException e) {
		  e.printStackTrace();
		}
		
		return personName;
	}
	
	public static double getActualHours(RepositoryTaskData repositoryTaskData) {
		String hours = repositoryTaskData.getAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ACT_HOURS_NAME);
		return Double.valueOf(hours).doubleValue();
	}

	public static double getRemainingHours(RepositoryTaskData repositoryTaskData) {
		String hours = repositoryTaskData.getAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_REMAINING_HOURS_NAME);
		return Double.valueOf(hours).doubleValue();
	}

	public static double getEstimatedHours(RepositoryTaskData repositoryTaskData) {
		String hours = repositoryTaskData.getAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_EST_HOURS_NAME);
		return Double.valueOf(hours).doubleValue();
	}

	public static Double getAdjustedEstimatedHours(RepositoryTaskData repositoryTaskData) {
		String hours = repositoryTaskData.getAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ADJUSTED_ESTIMATED_HOURS_NAME);
		return Double.valueOf(hours).doubleValue();
	}

	public static Double getEstimatedOriginalHours(RepositoryTaskData repositoryTaskData) {
		String hours = repositoryTaskData.getAttributeValue(XPlannerAttributeFactory.ATTRIBUTE_ESTIMATED_ORIGINAL_HOURS_NAME);
		return Double.valueOf(hours).doubleValue();
	}

	public static Date getCreatedDate(RepositoryTaskData repositoryTaskData) {
		Date createdDate = null;
		
		String dateString = repositoryTaskData.getAttributeValue(RepositoryTaskAttribute.DATE_CREATION);
		try {
			createdDate = XPlannerAttributeFactory.DATE_FORMAT.parse(dateString);
		}
		catch (ParseException e) {
			XPlannerMylarUIPlugin.log(e.getCause(), "", false);
		}
		
		return createdDate;
	}

	public static String getProjectName(TaskData taskData, XPlannerServer server) {
	  String projectName = Messages.XPlannerRepositoryUtils_NO_PROJECT_NAME;

	  UserStoryData userStory;
		try {
			userStory = server.getUserStory(taskData.getStoryId());
			projectName = getProjectName(userStory, server);
		}
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return projectName;
	}
	
	public static String getProjectName(UserStoryData userStory, XPlannerServer server) {
	  String projectName = Messages.XPlannerRepositoryUtils_NO_PROJECT_NAME;
	  
		try {
		  if (userStory != null) {
		    IterationData iteration = server.getIteration(userStory.getIterationId());
		    if (iteration != null) {
		      ProjectData project = server.getProject(iteration.getProjectId());
		      if (project != null) {
		        projectName = project.getName();
		      }  
		    }
		  }
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	  
	  return projectName;
	}

	public static String getIterationName(TaskData taskData, XPlannerServer server) {
	  String iterationName = Messages.XPlannerRepositoryUtils_NO_ITERATION_NAME;
	  
		try {
			UserStoryData userStory = server.getUserStory(taskData.getStoryId());
			iterationName = getIterationName(userStory, server);
		}
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return iterationName;
	}

	public static String getIterationName(UserStoryData userStory, XPlannerServer server) {
	  String iterationName = Messages.XPlannerRepositoryUtils_NO_ITERATION_NAME;
	  
		try {
	    IterationData iteration = server.getIteration(userStory.getIterationId());
	    if (iteration != null) {
	      iterationName = iteration.getName();
	    }
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	  
	  return iterationName;
	}

	public static String getUserStoryName(TaskData taskData, XPlannerServer server) {
	  String userStoryName = Messages.XPlannerRepositoryUtils_NO_USER_STORY_NAME;
	  
		try {
      UserStoryData userStory = server.getUserStory(taskData.getStoryId());
		  if (userStory != null) {
		  	userStoryName = userStory.getName();
		  }
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	  
	  return userStoryName;
	}

	public static String getDescription(RepositoryTaskData repositoryTaskData) {
		return repositoryTaskData.getAttributeValue(RepositoryTaskAttribute.DESCRIPTION);
	}

	public static boolean isCompleted(RepositoryTaskData repositoryTaskData) {
		return "1".equals(repositoryTaskData.getAttributeValue( //$NON-NLS-1$
			XPlannerAttributeFactory.ATTRIBUTE_TASK_COMPLETED));
	}

	public static String getName(RepositoryTaskData repositoryTaskData) {
		return repositoryTaskData.getAttributeValue(RepositoryTaskAttribute.SUMMARY);
	}

	public static String getPriorityFromXPlannerObject(Object xplannerObject, XPlannerServer server) {
		int priority = -1;
		UserStoryData userStory = null;
		
		try {
			if (xplannerObject instanceof TaskData) {
				userStory = server.getUserStory(((TaskData)xplannerObject).getStoryId());
			}
			else if (xplannerObject instanceof UserStoryData) {
				userStory = (UserStoryData)xplannerObject;
			}
			
			if (userStory != null) {
				priority = userStory.getPriority();
			}
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
		
		return priority == -1 ? "" : String.valueOf(priority); //$NON-NLS-1$
	}
	
	private static HashSet<String> validatedRepositoryUrls = new HashSet<String>();
	public static boolean isRepositoryUrlValidated(String repositoryUrl) {
		return validatedRepositoryUrls.contains(repositoryUrl);
	}
	
	static void addValidatedRepositoryUrl(String url) {
		validatedRepositoryUrls.add(url);
	}

	static void removeValidatedRepositoryUrl(String url) {
		validatedRepositoryUrls.remove(url);
	}
	
	public static void checkRepositoryValidated(String repositoryUrl) throws CoreException {
		if (repositoryUrl == null) {
			return;
		}
		
		TaskRepository taskRepository = TasksUiPlugin.getRepositoryManager().getRepository(
				XPlannerMylarUIPlugin.REPOSITORY_KIND, repositoryUrl);		
		if (taskRepository != null && !isRepositoryUrlValidated(taskRepository.getUrl())) {
			validateRepository(taskRepository);
		}
	}

	public static void validateRepository(TaskRepository taskRepository) throws CoreException {
		try {
			XPlannerServerFacade.getDefault().validateServerAndCredentials(
					taskRepository.getUrl(),
					taskRepository.getUserName(), taskRepository.getPassword());
		} 
		catch (Exception e) {
			throw new CoreException(XPlannerCorePlugin.toStatus(e));
		}

	}
}
 