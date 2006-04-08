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
 * Created on Jun 10, 2005
 */
package org.eclipse.mylar.internal.monitor.monitors;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.core.internal.preferences.Base64;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylar.internal.core.MylarContextManager;
import org.eclipse.mylar.internal.core.util.MylarStatusHandler;
import org.eclipse.mylar.internal.monitor.MylarMonitorPlugin;
import org.eclipse.mylar.internal.monitor.MylarMonitorPreferenceConstants;
import org.eclipse.mylar.provisional.core.AbstractUserInteractionMonitor;
import org.eclipse.mylar.provisional.core.IMylarElement;
import org.eclipse.mylar.provisional.core.InteractionEvent;
import org.eclipse.mylar.provisional.core.MylarPlugin;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.EditorPart;

/**
 * Limited to Java selections.
 * 
 * @author Mik Kersten
 */
public class SelectionMonitor extends AbstractUserInteractionMonitor {

	public static final String ENCRYPTION_ALGORITHM = "SHA";

	private static final String ID_JAVA_UNKNOWN = "(non-source element)";

	private static final String LABEL_FAILED_TO_OBFUSCATE = "<failed to obfuscate>";

	public static final String SELECTION_DEFAULT = "selected";

	public static final String SELECTION_NEW = "new";

	public static final String SELECTION_DECAYED = "decayed";

	public static final String SELECTION_PREDICTED = "predicted";

	private static final Object ID_JAVA_UNKNOW_OLD = "(non-existing element)";

	private IJavaElement lastSelectedElement = null;

	@Override
	protected void handleWorkbenchPartSelection(IWorkbenchPart part, ISelection selection) {
		// ignored, since not using taskscape monitoring facilities
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		String structureKind = InteractionEvent.ID_UNKNOWN;
		String obfuscatedElementHandle = InteractionEvent.ID_UNKNOWN;
		String elementHandle = InteractionEvent.ID_UNKNOWN;
		InteractionEvent.Kind interactionKind = InteractionEvent.Kind.SELECTION;
		if (selection instanceof StructuredSelection) {
			StructuredSelection structuredSelection = (StructuredSelection) selection;
			Object selectedObject = structuredSelection.getFirstElement();
			if (selectedObject == null)
				return;
			if (selectedObject instanceof IJavaElement) {
				IJavaElement javaElement = (IJavaElement) selectedObject;
				structureKind = "java:" + javaElement.getClass();
				elementHandle = javaElement.getHandleIdentifier();
				obfuscatedElementHandle = obfuscateJavaElementHandle(javaElement);
				lastSelectedElement = javaElement;
			} else {
				structureKind = InteractionEvent.ID_UNKNOWN + ": " + selectedObject.getClass();
				if (selectedObject instanceof IAdaptable) {
					IResource resource = (IResource) ((IAdaptable) selectedObject).getAdapter(IResource.class);
					if (resource != null) {
						obfuscatedElementHandle = obfuscateResourcePath(resource.getProjectRelativePath());
					}
				}
			}
		} else {
			if (selection instanceof TextSelection && part instanceof JavaEditor) {
				TextSelection textSelection = (TextSelection) selection;
				IJavaElement javaElement;
				try {
					javaElement = SelectionConverter.resolveEnclosingElement((JavaEditor) part, textSelection);
					if (javaElement != null) {
						structureKind = "java:" + javaElement.getClass();
						obfuscatedElementHandle = obfuscateJavaElementHandle(javaElement);
						elementHandle = javaElement.getHandleIdentifier();
						if (javaElement != null && javaElement.equals(lastSelectedElement)) {
							interactionKind = InteractionEvent.Kind.EDIT;
						}
						lastSelectedElement = javaElement;
					}
				} catch (JavaModelException e) {
					// ignore unresolved elements
					// MylarPlugin.log("Could not resolve java element from text
					// selection.", this);
				}
			} else if (part instanceof EditorPart) {
				EditorPart editorPart = (EditorPart) part;
				IEditorInput input = editorPart.getEditorInput();
				if (input instanceof IPathEditorInput) {
					structureKind = "file";
					obfuscatedElementHandle = obfuscateResourcePath(((IPathEditorInput) input).getPath());
				}
			}
		}
		IMylarElement node = MylarPlugin.getContextManager().getElement(elementHandle);
		String delta = "";
		float selectionFactor = MylarContextManager.getScalingFactors().get(InteractionEvent.Kind.SELECTION).getValue();

		// XXX: broken in 0.4?
		if (node != null) {
			if (node.getInterest().getEncodedValue() <= selectionFactor
					&& node.getInterest().getValue() > selectionFactor) {
				delta = SELECTION_PREDICTED;
			} else if (node.getInterest().getEncodedValue() < selectionFactor
					&& node.getInterest().getDecayValue() > selectionFactor) {
				delta = SELECTION_DECAYED;
			} else if (node.getInterest().getValue() == selectionFactor
					&& node.getInterest().getDecayValue() < selectionFactor) {
				delta = SELECTION_NEW;
			} else {
				delta = SELECTION_DEFAULT;
			}
		}

		InteractionEvent event = new InteractionEvent(interactionKind, structureKind, obfuscatedElementHandle, part
				.getSite().getId(), "null", delta, 0);
		MylarPlugin.getDefault().notifyInteractionObserved(event);
	}

	private String obfuscateResourcePath(IPath path) {
		if (path == null) {
			return "";
		} else {
			StringBuffer obfuscatedPath = new StringBuffer();
			for (int i = 0; i < path.segmentCount(); i++) {
				obfuscatedPath.append(obfuscateString(path.segments()[i]));
				if (i < path.segmentCount() - 1)
					obfuscatedPath.append('/');
			}
			return obfuscatedPath.toString();
		}
	}

	/**
	 * Encrypts the string using SHA, then makes it reasonable to print.
	 */
	private String obfuscateString(String string) {
		if(!isObfuscationEnabled()) { return string; }
		String obfuscatedString = null;
		try {
			MessageDigest md = MessageDigest.getInstance(ENCRYPTION_ALGORITHM);
			md.update(string.getBytes());
			byte[] digest = md.digest();
			obfuscatedString = new String(Base64.encode(digest)).replace('/', '=');
			// obfuscatedString = "" + new String(digest).hashCode();
		} catch (NoSuchAlgorithmException e) {
			MylarStatusHandler.log("SHA not available", this);
			obfuscatedString = LABEL_FAILED_TO_OBFUSCATE;
		}
		return obfuscatedString;
	}

	private boolean isObfuscationEnabled() {
		return MylarMonitorPlugin.getPrefs()
			.getBoolean(MylarMonitorPreferenceConstants.PREF_MONITORING_OBFUSCATE);
	}

	private String obfuscateJavaElementHandle(IJavaElement javaElement) {
		try {
			StringBuffer obfuscatedPath = new StringBuffer();
			IResource resource;
			resource = (IResource) javaElement.getUnderlyingResource();
			if (resource != null && (resource instanceof IFile)) {
				IFile file = (IFile) resource;
				obfuscatedPath.append(obfuscateResourcePath(file.getProjectRelativePath()));
				obfuscatedPath.append(':');
				obfuscatedPath.append(obfuscateString(javaElement.getElementName()));
				return obfuscatedPath.toString();
			} else {
				return obfuscateString(javaElement.getHandleIdentifier());
			}
		} catch (JavaModelException e) {
			// ignore non-existing element
			// MylarPlugin.log(this, "failed to resolve java element for
			// element: " + javaElement.getHandleIdentifier());
		}
		return ID_JAVA_UNKNOWN;
	}

	/**
	 * Some events do not have a valid handle, e.g. hande is null or ?
	 */
	public static boolean isValidStructureHandle(InteractionEvent event) {
		String handle = event.getStructureHandle();
		return handle != null 
			&& !handle.trim().equals("") 
			&& !handle.equals(SelectionMonitor.ID_JAVA_UNKNOWN)
			&& !handle.equals(SelectionMonitor.ID_JAVA_UNKNOW_OLD)
			&& event.isValidStructureHandle();
	}
}
