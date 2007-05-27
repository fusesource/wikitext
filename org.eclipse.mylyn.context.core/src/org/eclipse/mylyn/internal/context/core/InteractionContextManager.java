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

package org.eclipse.mylar.internal.context.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.mylar.context.core.AbstractContextStructureBridge;
import org.eclipse.mylar.context.core.AbstractRelationProvider;
import org.eclipse.mylar.context.core.ContextCorePlugin;
import org.eclipse.mylar.context.core.IInteractionContext;
import org.eclipse.mylar.context.core.IInteractionContextListener;
import org.eclipse.mylar.context.core.IInteractionElement;
import org.eclipse.mylar.context.core.IInteractionRelation;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.monitor.core.InteractionEvent;

/**
 * This is the core class resposible for context management.
 * 
 * @author Mik Kersten
 */
public class InteractionContextManager {

	// TODO: move constants

	public static final String CONTEXT_FILENAME_ENCODING = "UTF-8";

	public static final String ACTIVITY_DELTA_DEACTIVATED = "deactivated";

	public static final String ACTIVITY_DELTA_ACTIVATED = "activated";

	public static final String ACTIVITY_DELTA_PULSE = "pulse";

	public static final String ACTIVITY_ORIGIN_ID = "org.eclipse.mylar.core";

	public static final String ACTIVITY_HANDLE_ATTENTION = "attention";

	public static final String ACTIVITY_HANDLE_LIFECYCLE = "lifecycle";

	public static final String ACTIVITY_DELTA_STARTED = "started";

	public static final String ACTIVITY_DELTA_STOPPED = "stopped";

	public static final String ACTIVITY_STRUCTURE_KIND = "context";

	public static final String CONTEXT_HISTORY_FILE_NAME = "activity";

	public static final String OLD_CONTEXT_HISTORY_FILE_NAME = "context-history";

	public static final String SOURCE_ID_MODEL_PROPAGATION = "org.eclipse.mylar.core.model.interest.propagation";

	public static final String SOURCE_ID_DECAY = "org.eclipse.mylar.core.model.interest.decay";

	public static final String SOURCE_ID_DECAY_CORRECTION = "org.eclipse.mylar.core.model.interest.decay.correction";

	public static final String SOURCE_ID_MODEL_ERROR = "org.eclipse.mylar.core.model.interest.propagation";

	public static final String CONTAINMENT_PROPAGATION_ID = "org.eclipse.mylar.core.model.edges.containment";

	public static final String CONTEXT_FILE_EXTENSION = ".xml.zip";

	public static final String CONTEXT_FILE_EXTENSION_OLD = ".xml";

	private static final int MAX_PROPAGATION = 17; // TODO: parametrize this

	private int numInterestingErrors = 0;

	private List<String> errorElementHandles = new ArrayList<String>();

	private Set<File> contextFiles = null;

	private boolean contextCapturePaused = false;

	private CompositeInteractionContext activeContext = new CompositeInteractionContext();

	/**
	 * Global contexts do not participate in the regular activation lifecycle
	 * but are instead activated and deactivated by clients.
	 */
	private Map<String, InteractionContext> globalContexts = new HashMap<String, InteractionContext>();

	private InteractionContext activityMetaContext = null;

	private List<IInteractionContextListener> activityMetaContextListeners = new ArrayList<IInteractionContextListener>();

	private List<IInteractionContextListener> listeners = new CopyOnWriteArrayList<IInteractionContextListener>();

	private List<IInteractionContextListener> waitingListeners = new ArrayList<IInteractionContextListener>();

	private boolean suppressListenerNotification = false;

	private InteractionContextExternalizer externalizer = new InteractionContextExternalizer();

	private boolean activationHistorySuppressed = false;

	private static ScalingFactors scalingFactors = new ScalingFactors();

	public InteractionContext getActivityMetaContext() {
		if (activityMetaContext == null) {
			loadActivityMetaContext();
		}
		return activityMetaContext;
	}

	public void loadActivityMetaContext() {
		if (ContextCorePlugin.getDefault().getContextStore() != null) {
			File contextActivityFile = getFileForContext(CONTEXT_HISTORY_FILE_NAME);
			activityMetaContext = externalizer.readContextFromXML(CONTEXT_HISTORY_FILE_NAME, contextActivityFile);
			if (activityMetaContext == null) {
				resetActivityHistory();
			}
			for (IInteractionContextListener listener : activityMetaContextListeners) {
				listener.contextActivated(activityMetaContext);
			}
		} else {
			resetActivityHistory();
			MylarStatusHandler.log("No context store installed, not restoring activity context.", this);
		}
	}

	public void processActivityMetaContextEvent(InteractionEvent event) {
		IInteractionElement element = getActivityMetaContext().parseEvent(event);
		for (IInteractionContextListener listener : activityMetaContextListeners) {
			try {
				List<IInteractionElement> changed = new ArrayList<IInteractionElement>();
				changed.add(element);
				listener.interestChanged(changed);
			} catch (Throwable t) {
				MylarStatusHandler.fail(t, "context listener failed", false);
			}
		}
	}

	public void resetActivityHistory() {
		activityMetaContext = new InteractionContext(CONTEXT_HISTORY_FILE_NAME, InteractionContextManager
				.getScalingFactors());
		saveActivityContext();
	}

	public IInteractionElement getActiveElement() {
		if (activeContext != null) {
			return activeContext.getActiveNode();
		} else {
			return null;
		}
	}

	public void addErrorPredictedInterest(String handle, String kind, boolean notify) {
		if (numInterestingErrors > scalingFactors.getMaxNumInterestingErrors()
				|| activeContext.getContextMap().isEmpty())
			return;
		InteractionEvent errorEvent = new InteractionEvent(InteractionEvent.Kind.PROPAGATION, kind, handle,
				SOURCE_ID_MODEL_ERROR, scalingFactors.getErrorInterest());
		processInteractionEvent(errorEvent, true);
		errorElementHandles.add(handle);
		numInterestingErrors++;
	}

	/**
	 * TODO: worry about decay-related change if predicted interest dacays
	 */
	public void removeErrorPredictedInterest(String handle, String kind, boolean notify) {
		if (activeContext.getContextMap().isEmpty())
			return;
		if (handle == null)
			return;
		IInteractionElement element = activeContext.get(handle);
		if (element != null && element.getInterest().isInteresting() && errorElementHandles.contains(handle)) {
			InteractionEvent errorEvent = new InteractionEvent(InteractionEvent.Kind.MANIPULATION, kind, handle,
					SOURCE_ID_MODEL_ERROR, -scalingFactors.getErrorInterest());
			processInteractionEvent(errorEvent, true);
			numInterestingErrors--;
			errorElementHandles.remove(handle);
			// TODO: this results in double-notification
			if (notify)
				for (IInteractionContextListener listener : listeners) {
					List<IInteractionElement> changed = new ArrayList<IInteractionElement>();
					changed.add(element);
					listener.interestChanged(changed);
				}
		}
	}

	public IInteractionElement getElement(String elementHandle) {
		if (activeContext != null && elementHandle != null) {
			return activeContext.get(elementHandle);
		} else {
			return null;
		}
	}

	public IInteractionElement processInteractionEvent(InteractionEvent event) {
		return processInteractionEvent(event, true);
	}

	public IInteractionElement processInteractionEvent(InteractionEvent event, boolean propagateToParents) {
		return processInteractionEvent(event, propagateToParents, true);
	}

	public void processInteractionEvents(List<InteractionEvent> events, boolean propagateToParents) {
		Set<IInteractionElement> compositeDelta = new HashSet<IInteractionElement>();
		for (InteractionEvent event : events) {
			if (isContextActive()) {
				compositeDelta.addAll(internalProcessInteractionEvent(event, activeContext, propagateToParents));
			}
			for (InteractionContext globalContext : globalContexts.values()) {
				if (globalContext.getContentLimitedTo().equals(event.getStructureKind())) {
					internalProcessInteractionEvent(event, globalContext, propagateToParents);
				}
			}
		}
		notifyInterestDelta(new ArrayList<IInteractionElement>(compositeDelta));
	}

	public IInteractionElement processInteractionEvent(InteractionEvent event, boolean propagateToParents,
			boolean notifyListeners) {
		boolean alreadyNotified = false;
		if (isContextActive()) {
			List<IInteractionElement> interestDelta = internalProcessInteractionEvent(event, activeContext,
					propagateToParents);
			if (notifyListeners) {
				notifyInterestDelta(interestDelta);
			}
		}
		for (InteractionContext globalContext : globalContexts.values()) {
			if (globalContext.getContentLimitedTo().equals(event.getStructureKind())) {
				List<IInteractionElement> interestDelta = internalProcessInteractionEvent(event, globalContext, propagateToParents);
				if (notifyListeners && !alreadyNotified) {
					notifyInterestDelta(interestDelta);
				}
			}
		}

		return activeContext.get(event.getStructureHandle());
	}

	private List<IInteractionElement> internalProcessInteractionEvent(InteractionEvent event,
			IInteractionContext interactionContext, boolean propagateToParents) {
		if (contextCapturePaused || InteractionEvent.Kind.COMMAND.equals(event.getKind())
				|| suppressListenerNotification) {
			return Collections.emptyList();
		}

		IInteractionElement previous = interactionContext.get(event.getStructureHandle());
		float previousInterest = 0;
		boolean previouslyPredicted = false;
		boolean previouslyPropagated = false;
		float decayOffset = 0;
		if (previous != null) {
			previousInterest = previous.getInterest().getValue();
			previouslyPredicted = previous.getInterest().isPredicted();
			previouslyPropagated = previous.getInterest().isPropagated();
		}
		if (event.getKind().isUserEvent()) {
			decayOffset = ensureIsInteresting(interactionContext, event.getStructureKind(), event.getStructureHandle(),
					previous, previousInterest);
		}
		IInteractionElement element = addInteractionEvent(interactionContext, event);
		List<IInteractionElement> interestDelta = new ArrayList<IInteractionElement>();
		if (propagateToParents && !event.getKind().equals(InteractionEvent.Kind.MANIPULATION)) {
			propegateInterestToParents(interactionContext, event.getKind(), element, previousInterest, decayOffset, 1,
					interestDelta);
		}
		if (event.getKind().isUserEvent() && interactionContext instanceof CompositeInteractionContext) {
			((CompositeInteractionContext) interactionContext).setActiveElement(element);
		}

		if (isInterestDelta(previousInterest, previouslyPredicted, previouslyPropagated, element)) {
			interestDelta.add(element);
		}

		checkForLandmarkDeltaAndNotify(previousInterest, element);
		return interestDelta;
	}

	private IInteractionElement addInteractionEvent(IInteractionContext interactionContext, InteractionEvent event) {
		if (interactionContext instanceof CompositeInteractionContext) {
			return ((CompositeInteractionContext) interactionContext).addEvent(event);
		} else if (interactionContext instanceof InteractionContext) {
			return ((InteractionContext) interactionContext).parseEvent(event);
		} else {
			return null;
		}
	}

	private float ensureIsInteresting(IInteractionContext interactionContext, String contentType, String handle,
			IInteractionElement previous, float previousInterest) {
		float decayOffset = 0;
		if (previousInterest < 0) { // reset interest if not interesting
			decayOffset = (-1) * (previous.getInterest().getValue());
			addInteractionEvent(interactionContext, new InteractionEvent(InteractionEvent.Kind.MANIPULATION,
					contentType, handle, SOURCE_ID_DECAY_CORRECTION, decayOffset));
		}
		return decayOffset;
	}

	private void notifyInterestDelta(List<IInteractionElement> interestDelta) {
		if (!interestDelta.isEmpty()) {
			for (IInteractionContextListener listener : listeners) {
				listener.interestChanged(interestDelta);
			}
		}
	}

	protected boolean isInterestDelta(float previousInterest, boolean previouslyPredicted,
			boolean previouslyPropagated, IInteractionElement node) {
		float currentInterest = node.getInterest().getValue();
		if (previousInterest <= 0 && currentInterest > 0) {
			return true;
		} else if (previousInterest > 0 && currentInterest <= 0) {
			return true;
		} else if (currentInterest > 0 && previouslyPredicted && !node.getInterest().isPredicted()) {
			return true;
		} else if (currentInterest > 0 && previouslyPropagated && !node.getInterest().isPropagated()) {
			return true;
		} else {
			return false;
		}
	}

	protected void checkForLandmarkDeltaAndNotify(float previousInterest, IInteractionElement node) {
		// TODO: don't call interestChanged if it's a landmark?
		AbstractContextStructureBridge bridge = ContextCorePlugin.getDefault()
				.getStructureBridge(node.getContentType());
		if (bridge.canBeLandmark(node.getHandleIdentifier())) {
			if (previousInterest >= scalingFactors.getLandmark() && !node.getInterest().isLandmark()) {
				for (IInteractionContextListener listener : listeners)
					listener.landmarkRemoved(node);
			} else if (previousInterest < scalingFactors.getLandmark() && node.getInterest().isLandmark()) {
				for (IInteractionContextListener listener : listeners)
					listener.landmarkAdded(node);
			}
		}
	}

	private void propegateInterestToParents(IInteractionContext interactionContext, InteractionEvent.Kind kind,
			IInteractionElement node, float previousInterest, float decayOffset, int level,
			List<IInteractionElement> interestDelta) {

		if (level > MAX_PROPAGATION || node == null || node.getHandleIdentifier() == null
				|| node.getInterest().getValue() <= 0) {
			return;
		}

		checkForLandmarkDeltaAndNotify(previousInterest, node);

		level++; // original is 1st level
		float propagatedIncrement = node.getInterest().getValue() - previousInterest + decayOffset;

		AbstractContextStructureBridge bridge = ContextCorePlugin.getDefault()
				.getStructureBridge(node.getContentType());
		String parentHandle = bridge.getParentHandle(node.getHandleIdentifier());

		// check if should use child bridge
		for (String contentType : ContextCorePlugin.getDefault().getChildContentTypes(bridge.getContentType())) {
			AbstractContextStructureBridge childBridge = ContextCorePlugin.getDefault().getStructureBridge(contentType);
			Object resolved = childBridge.getObjectForHandle(parentHandle);
			if (resolved != null) {
				AbstractContextStructureBridge canonicalBridge = ContextCorePlugin.getDefault().getStructureBridge(
						resolved);
				// HACK: hard-coded resource content type
				if (!canonicalBridge.getContentType().equals(ContextCorePlugin.CONTENT_TYPE_ANY)) {
					// NOTE: resetting bridge
					bridge = canonicalBridge;
				}
			}
		}

		if (parentHandle != null) {
			InteractionEvent propagationEvent = new InteractionEvent(InteractionEvent.Kind.PROPAGATION, bridge
					.getContentType(node.getHandleIdentifier()), parentHandle, SOURCE_ID_MODEL_PROPAGATION,
					CONTAINMENT_PROPAGATION_ID, propagatedIncrement);
			IInteractionElement previous = interactionContext.get(propagationEvent.getStructureHandle());
			if (previous != null && previous.getInterest() != null) {
				previousInterest = previous.getInterest().getValue();
			}
			IInteractionElement parentNode = addInteractionEvent(interactionContext, propagationEvent);
			if (kind.isUserEvent() && parentNode.getInterest().getEncodedValue() < scalingFactors.getInteresting()) {
				float parentOffset = ((-1) * parentNode.getInterest().getEncodedValue()) + 1;
				addInteractionEvent(interactionContext, new InteractionEvent(InteractionEvent.Kind.MANIPULATION,
						parentNode.getContentType(), parentNode.getHandleIdentifier(), SOURCE_ID_DECAY_CORRECTION,
						parentOffset));
			}
			if (previous != null && isInterestDelta(previousInterest, previous.getInterest().isPredicted(), previous.getInterest()
					.isPropagated(), parentNode)) {
				interestDelta.add(0, parentNode);
			}
			propegateInterestToParents(interactionContext, kind, parentNode, previousInterest, decayOffset, level,
					interestDelta);// adapter.getResourceExtension(),
		}
	}

// public List<IInteractionElement>
// findCompositesForNodes(List<InteractionContextElement> nodes) {
// List<IInteractionElement> composites = new ArrayList<IInteractionElement>();
// for (InteractionContextElement node : nodes) {
// composites.add(aaactiveContext.get(node.getHandleIdentifier()));
// }
// return composites;
// }

	public void addListener(IInteractionContextListener listener) {
		if (listener != null) {
			if (suppressListenerNotification && !waitingListeners.contains(listener)) {
				waitingListeners.add(listener);
			} else {
				if (!listeners.contains(listener))
					listeners.add(listener);
			}
		} else {
			MylarStatusHandler.log("attempted to add null lisetener", this);
		}
	}

	public void removeListener(IInteractionContextListener listener) {
		listeners.remove(listener);
	}

	public void addActivityMetaContextListener(IInteractionContextListener listener) {
		activityMetaContextListeners.add(listener);
	}

	public void removeActivityMetaContextListener(IInteractionContextListener listener) {
		activityMetaContextListeners.remove(listener);
	}

	public void removeAllListeners() {
		listeners.clear();
	}

	/**
	 * Public for testing, activiate via handle
	 */
	public void activateContext(InteractionContext context) {
		activeContext.getContextMap().put(context.getHandleIdentifier(), context);
		if (contextFiles != null) {
			contextFiles.add(getFileForContext(context.getHandleIdentifier()));
		}
		if (!activationHistorySuppressed) {
			processActivityMetaContextEvent(new InteractionEvent(InteractionEvent.Kind.COMMAND,
					ACTIVITY_STRUCTURE_KIND, context.getHandleIdentifier(), ACTIVITY_ORIGIN_ID, null,
					ACTIVITY_DELTA_ACTIVATED, 1f));
		}
	}

	public Collection<InteractionContext> getActiveContexts() {
		return Collections.unmodifiableCollection(activeContext.getContextMap().values());
	}

	public void activateContext(String handleIdentifier) {
		try {
			suppressListenerNotification = true;
			InteractionContext context = activeContext.getContextMap().get(handleIdentifier);
			if (context == null)
				context = loadContext(handleIdentifier);
			if (context != null) {
				activateContext(context);
				for (IInteractionContextListener listener : listeners) {
					try {
						listener.contextActivated(context);
					} catch (Exception e) {
						MylarStatusHandler.fail(e, "context listener failed", false);
					}
				}
				// refreshRelatedElements();
			} else {
				MylarStatusHandler.log("Could not load context", this);
			}
			suppressListenerNotification = false;
			listeners.addAll(waitingListeners);
			waitingListeners.clear();
		} catch (Throwable t) {
			MylarStatusHandler.log(t, "Could not activate context");
		}
	}

	/**
	 * Lazily loads set of handles with corresponding contexts.
	 */
	public boolean hasContext(String handleIdentifier) {
		if (handleIdentifier == null) {
			return false;
		}
		if (contextFiles == null) {
			contextFiles = new HashSet<File>();
			File contextDirectory = ContextCorePlugin.getDefault().getContextStore().getContextDirectory();
			File[] files = contextDirectory.listFiles();
			for (File file : files) {
				contextFiles.add(file);
			}
		}
		if (getActiveContext() != null && handleIdentifier.equals(getActiveContext().getHandleIdentifier())) {
			return !getActiveContext().getAllElements().isEmpty();
		} else {
			File file = getFileForContext(handleIdentifier);
			return contextFiles.contains(file);
		}
// File contextFile = getFileForContext(path);
// return contextFile.exists() && contextFile.length() > 0;
	}

	public void deactivateAllContexts() {
		for (String handleIdentifier : activeContext.getContextMap().keySet()) {
			deactivateContext(handleIdentifier);
		}
	}

	public void deactivateContext(String handleIdentifier) {
		try {
			IInteractionContext context = activeContext.getContextMap().get(handleIdentifier);
			if (context != null) {
				saveContext(handleIdentifier);
				activeContext.getContextMap().remove(handleIdentifier);

				setContextCapturePaused(true);
				for (IInteractionContextListener listener : listeners) {
					try {
						listener.contextDeactivated(context);
					} catch (Exception e) {
						MylarStatusHandler.fail(e, "context listener failed", false);
					}
				}
				if (context.getAllElements().size() == 0) {
					contextFiles.remove(getFileForContext(context.getHandleIdentifier()));
				}
				setContextCapturePaused(false);
			}
			if (!activationHistorySuppressed) {
				processActivityMetaContextEvent(new InteractionEvent(InteractionEvent.Kind.COMMAND,
						ACTIVITY_STRUCTURE_KIND, handleIdentifier, ACTIVITY_ORIGIN_ID, null,
						ACTIVITY_DELTA_DEACTIVATED, 1f));
			}
			saveActivityContext();
		} catch (Throwable t) {
			MylarStatusHandler.log(t, "Could not deactivate context");
		}
	}

	public void deleteContext(String handleIdentifier) {
		IInteractionContext context = activeContext.getContextMap().get(handleIdentifier);
		eraseContext(handleIdentifier, false);
		try {
			File file = getFileForContext(handleIdentifier);
			if (file.exists()) {
				file.delete();
			}
			setContextCapturePaused(true);
			for (IInteractionContextListener listener : listeners) {
				listener.contextCleared(context);
			}
			setContextCapturePaused(false);
			if (contextFiles != null) {
				contextFiles.add(getFileForContext(handleIdentifier));
			}
		} catch (SecurityException e) {
			MylarStatusHandler.fail(e, "Could not delete context file", false);
		}
	}

	private void eraseContext(String handleIdentifier, boolean notify) {
		if (contextFiles != null) {
			contextFiles.remove(getFileForContext(handleIdentifier));
		}
		InteractionContext context = activeContext.getContextMap().get(handleIdentifier);
		if (context == null)
			return;
		activeContext.getContextMap().remove(context);
		context.reset();
	}

	/**
	 * @return false if the map could not be read for any reason
	 */
	public InteractionContext loadContext(String handleIdentifier) {
		InteractionContext loadedContext = externalizer.readContextFromXML(handleIdentifier,
				getFileForContext(handleIdentifier));
		if (loadedContext == null) {
			return new InteractionContext(handleIdentifier, InteractionContextManager.getScalingFactors());
		} else {
			return loadedContext;
		}
	}

	public void saveContext(String handleIdentifier) {
		InteractionContext context = activeContext.getContextMap().get(handleIdentifier);
		if (context == null) {
			return;
		} else {
			saveContext(context);
		}
	}

	public void saveContext(InteractionContext context) {
		boolean wasPaused = contextCapturePaused;
		try {
			if (!wasPaused) {
				setContextCapturePaused(true);
			}
			
			context.collapse();
			externalizer.writeContextToXml(context, getFileForContext(context.getHandleIdentifier()));
			if (contextFiles == null) {
				contextFiles = new HashSet<File>();
			}
			contextFiles.add(getFileForContext(context.getHandleIdentifier()));
		} catch (Throwable t) {
			MylarStatusHandler.fail(t, "could not save context", false);
		} finally {
			if (!wasPaused) {
				setContextCapturePaused(false);
			}
		}
	}

	public void saveActivityContext() {
		if (ContextCorePlugin.getDefault().getContextStore() == null) {
			return;
		}
		boolean wasPaused = contextCapturePaused;
		try {
			if (!wasPaused) {
				setContextCapturePaused(true);
			}

			List<InteractionEvent> attention = new ArrayList<InteractionEvent>();

			InteractionContext context = getActivityMetaContext();
			InteractionContext tempContext = new InteractionContext(CONTEXT_HISTORY_FILE_NAME,
					InteractionContextManager.getScalingFactors());
			for (InteractionEvent event : context.getInteractionHistory()) {
				if (event.getDelta().equals(InteractionContextManager.ACTIVITY_DELTA_ACTIVATED)
						&& event.getStructureHandle().equals(InteractionContextManager.ACTIVITY_HANDLE_ATTENTION)) {
					attention.add(event);
				} else {
					addAttentionEvents(attention, tempContext);
					tempContext.parseEvent(event);
				}
			}

			if (!attention.isEmpty()) {
				addAttentionEvents(attention, tempContext);
			}

			externalizer.writeContextToXml(tempContext, getFileForContext(CONTEXT_HISTORY_FILE_NAME));
		} catch (Throwable t) {
			MylarStatusHandler.fail(t, "could not save activity history", false);
		} finally {
			if (!wasPaused) {
				setContextCapturePaused(false);
			}
		}
	}

	private void addAttentionEvents(List<InteractionEvent> attention, InteractionContext temp) {
		InteractionEvent aggregateEvent = null;
		try {
			if (attention.size() > 1) {
				InteractionEvent firstEvent = attention.get(0);
				long totalTime = 0;
				for (InteractionEvent interactionEvent : attention) {
					totalTime += interactionEvent.getEndDate().getTime() - interactionEvent.getDate().getTime();
				}
				if (totalTime != 0) {
					Date newEndDate = new Date(firstEvent.getDate().getTime() + totalTime);
					aggregateEvent = new InteractionEvent(firstEvent.getKind(), firstEvent.getStructureKind(),
							firstEvent.getStructureHandle(), firstEvent.getOriginId(), firstEvent.getNavigation(),
							firstEvent.getDelta(), 1f, firstEvent.getDate(), newEndDate);
				}
			} else if (attention.size() == 1) {
				if (attention.get(0).getEndDate().getTime() - attention.get(0).getDate().getTime() > 0) {
					aggregateEvent = attention.get(0);
				}
			}
			if (aggregateEvent != null) {
				temp.parseEvent(aggregateEvent);
			}
			attention.clear();
		} catch (Exception e) {
			MylarStatusHandler.fail(e, "Error during meta activity collapse", false);
		}
	}

	public File getFileForContext(String handleIdentifier) {
		String encoded;
		try {
			encoded = URLEncoder.encode(handleIdentifier, CONTEXT_FILENAME_ENCODING);
			File contextDirectory = ContextCorePlugin.getDefault().getContextStore().getContextDirectory();
			File contextFile = new File(contextDirectory, encoded + CONTEXT_FILE_EXTENSION);
			return contextFile;
		} catch (UnsupportedEncodingException e) {
			MylarStatusHandler.fail(e, "Could not determine path for context", false);
		}
		return null;
	}

	public IInteractionContext getActiveContext() {
		return activeContext;
	}

	public void resetLandmarkRelationshipsOfKind(String reltationKind) {
		for (IInteractionElement landmark : activeContext.getLandmarks()) {
			for (IInteractionRelation edge : landmark.getRelations()) {
				if (edge.getRelationshipHandle().equals(reltationKind)) {
					landmark.clearRelations();
				}
			}
		}
		for (IInteractionContextListener listener : listeners)
			listener.relationsChanged(null);
	}

	/**
	 * Copy the listener list in case it is modified during the notificiation.
	 * 
	 * @param node
	 */
	public void notifyRelationshipsChanged(IInteractionElement node) {
		if (suppressListenerNotification)
			return;
		for (IInteractionContextListener listener : listeners) {
			listener.relationsChanged(node);
		}
	}

	public static ScalingFactors getScalingFactors() {
		return InteractionContextManager.scalingFactors;
	}

	public boolean isContextActive() {
		return !contextCapturePaused && activeContext.getContextMap().values().size() > 0;
	}

	public List<IInteractionElement> getActiveLandmarks() {
		List<IInteractionElement> allLandmarks = activeContext.getLandmarks();
		List<IInteractionElement> acceptedLandmarks = new ArrayList<IInteractionElement>();
		for (IInteractionElement node : allLandmarks) {
			AbstractContextStructureBridge bridge = ContextCorePlugin.getDefault().getStructureBridge(
					node.getContentType());

			if (bridge.canBeLandmark(node.getHandleIdentifier())) {
				acceptedLandmarks.add(node);
			}
		}
		return acceptedLandmarks;
	}

	public Collection<IInteractionElement> getInterestingDocuments(IInteractionContext context) {
		Set<IInteractionElement> set = new HashSet<IInteractionElement>();
		if (context == null) {
			return set;
		} else {
			List<IInteractionElement> allIntersting = context.getInteresting();
			for (IInteractionElement node : allIntersting) {
				if (ContextCorePlugin.getDefault().getStructureBridge(node.getContentType()).isDocument(
						node.getHandleIdentifier())) {
					set.add(node);
				}
			}
			return set;
		}
	}

	public Collection<IInteractionElement> getInterestingDocuments() {
		return getInterestingDocuments(activeContext);
	}

	public boolean isActivationHistorySuppressed() {
		return activationHistorySuppressed;
	}

	public void setActivationHistorySuppressed(boolean activationHistorySuppressed) {
		this.activationHistorySuppressed = activationHistorySuppressed;
	}

	/**
	 * @return true if interest was manipulated successfully
	 */
	public boolean manipulateInterestForElement(IInteractionElement element, boolean increment, boolean forceLandmark,
			String sourceId) {
		if (element == null) {
			return false;
		}
		float originalValue = element.getInterest().getValue();
		float changeValue = 0;
		AbstractContextStructureBridge bridge = ContextCorePlugin.getDefault().getStructureBridge(
				element.getContentType());
		if (!increment) {
			if (element.getInterest().isLandmark() && bridge.canBeLandmark(element.getHandleIdentifier())) {
				// keep it interesting
				changeValue = (-1 * originalValue) + 1;
			} else {
				// make uninteresting
				if (originalValue >= 0) {
					changeValue = (-1 * originalValue) - 1;
				}

				// reduce interest of children
				for (String childHandle : bridge.getChildHandles(element.getHandleIdentifier())) {
					IInteractionElement childElement = getElement(childHandle);
					if (childElement != null && childElement.getInterest().isInteresting()
							&& !childElement.equals(element)) {
						manipulateInterestForElement(childElement, increment, forceLandmark, sourceId);
					}
				}
			}
		} else {
			if (!forceLandmark && (originalValue > InteractionContextManager.getScalingFactors().getLandmark())) {
				changeValue = 0;
			} else {
				// make it a landmark by setting interest to 2 x landmark
				// interest
				if (element != null && bridge.canBeLandmark(element.getHandleIdentifier())) {
					changeValue = (2 * InteractionContextManager.getScalingFactors().getLandmark()) - originalValue + 1;
				} else {
					return false;
				}
			}
		}
		if (changeValue != 0) {
			InteractionEvent interactionEvent = new InteractionEvent(InteractionEvent.Kind.MANIPULATION, element
					.getContentType(), element.getHandleIdentifier(), sourceId, changeValue);
			processInteractionEvent(interactionEvent);
		}
		return true;
	}

	public void setActiveSearchEnabled(boolean enabled) {
		for (AbstractRelationProvider provider : ContextCorePlugin.getDefault().getRelationProviders()) {
			provider.setEnabled(enabled);
		}
	}

	/**
	 * Retruns the highest interet context.
	 * 
	 * TODO: refactor this into better multiple context support
	 */
	public String getDominantContextHandleForElement(IInteractionElement node) {
		IInteractionElement dominantNode = null;
		if (node instanceof CompositeContextElement) {
			CompositeContextElement compositeNode = (CompositeContextElement) node;
			if (compositeNode.getNodes().isEmpty())
				return null;
			dominantNode = (IInteractionElement) compositeNode.getNodes().toArray()[0];

			for (IInteractionElement concreteNode : compositeNode.getNodes()) {
				if (dominantNode != null
						&& dominantNode.getInterest().getValue() < concreteNode.getInterest().getValue()) {
					dominantNode = concreteNode;
				}
			}
		} else if (node instanceof InteractionContextElement) {
			dominantNode = node;
		}
		if (node != null) {
			return ((InteractionContextElement) dominantNode).getContext().getHandleIdentifier();
		} else {
			return null;
		}
	}

	public void updateHandle(IInteractionElement element, String newHandle) {
		if (element == null)
			return;
		getActiveContext().updateElementHandle(element, newHandle);
		for (IInteractionContextListener listener : listeners) {
			List<IInteractionElement> changed = new ArrayList<IInteractionElement>();
			changed.add(element);
			listener.interestChanged(changed);
		}
		if (element.getInterest().isLandmark()) {
			for (IInteractionContextListener listener : listeners) {
				listener.landmarkAdded(element);
			}
		}
	}

	public void delete(IInteractionElement element) {
		if (element == null)
			return;
		getActiveContext().delete(element);
		for (IInteractionContextListener listener : listeners) {
			listener.elementDeleted(element);
		}
	}

	/**
	 * NOTE: If pausing ensure to restore to original state.
	 */
	public void setContextCapturePaused(boolean paused) {
		this.contextCapturePaused = paused;
	}

	public boolean isContextCapturePaused() {
		return contextCapturePaused;
	}

	/**
	 * For testing.
	 */
	public List<IInteractionContextListener> getListeners() {
		return Collections.unmodifiableList(listeners);
	}

	public boolean isValidContextFile(File file) {
		if (file.exists() && file.getName().endsWith(InteractionContextManager.CONTEXT_FILE_EXTENSION)) {
			InteractionContext context = externalizer.readContextFromXML("temp", file);
			return context != null;
		}
		return false;
	}

	public void transferContextAndActivate(String handleIdentifier, File file) {
		File contextFile = getFileForContext(handleIdentifier);
		contextFile.delete();
		try {
			copy(file, contextFile);
		} catch (IOException e) {
			MylarStatusHandler.fail(e, "Cold not transfer context", false);
		}
	}

	private void copy(File src, File dest) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dest);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	public void addGlobalContext(InteractionContext context) {
		globalContexts.put(context.getHandleIdentifier(), context);
	}

	public void removeGlobalContext(InteractionContext context) {
		globalContexts.remove(context.getHandleIdentifier());
	}
	
	public Collection<InteractionContext> getGlobalContexts() {	
		return globalContexts.values();
	}
}
