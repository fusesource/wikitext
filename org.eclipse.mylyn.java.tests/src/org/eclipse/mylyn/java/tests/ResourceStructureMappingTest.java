/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.java.tests;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionElement;
import org.eclipse.mylyn.internal.resources.ui.ResourcesUiBridgePlugin;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * @author Mik Kersten
 */
public class ResourceStructureMappingTest extends AbstractJavaContextTest {

	public void testParentResourceMapping() throws CoreException {
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		IMethod m1 = type1.createMethod("public void m1() { }", null, true, null);

		monitor.selectionChanged(part, new StructuredSelection(m1));
		IInteractionElement m1Node = ContextCore.getContextManager().getElement(m1.getHandleIdentifier());
		assertTrue(m1Node.getInterest().isInteresting());

		IResource containingResource = ResourcesUiBridgePlugin.getDefault().getResourceForElement(m1Node, true);
		assertEquals(m1.getCompilationUnit().getAdapter(IResource.class), containingResource);
	}

}
