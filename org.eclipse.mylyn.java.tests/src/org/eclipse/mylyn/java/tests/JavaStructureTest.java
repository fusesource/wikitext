/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jun 6, 2005
  */
package org.eclipse.mylar.java.tests;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylar.core.IMylarContextNode;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.core.internal.MylarContextManager;
import org.eclipse.mylar.core.internal.ScalingFactors;
import org.eclipse.mylar.core.internal.MylarContext;
import org.eclipse.mylar.core.tests.AbstractTaskscapeTest;
import org.eclipse.mylar.core.tests.support.TestProject;
import org.eclipse.mylar.java.JavaEditingMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.Workbench;

/**
 * @author Mik Kersten
 */
public class JavaStructureTest extends AbstractTaskscapeTest {
    
    private MylarContextManager manager = MylarPlugin.getContextManager();
    private JavaEditingMonitor monitor = new JavaEditingMonitor();
    private IWorkbenchPart part = Workbench.getInstance().getActiveWorkbenchWindow().getActivePage().getActivePart();
    
        
    private TestProject project;
    private IPackageFragment pkg;
    private IType typeFoo;
    private IMethod caller;
    private IMethod callee;
    private MylarContext taskscape;
    private ScalingFactors scaling = new ScalingFactors();
    
    @Override
    protected void setUp() throws Exception {
        project = new TestProject(this.getClass().getName());
        pkg = project.createPackage("pkg1");
        typeFoo = project.createType(pkg, "Foo.java", "public class Foo { }" );
        caller = typeFoo.createMethod("void caller() { callee(); }", null, true, null);
        callee = typeFoo.createMethod("void callee() { }", callee, true, null);

        taskscape = new MylarContext("1", scaling);
        manager.contextActivated(taskscape);
    }
    
    @Override
    protected void tearDown() throws Exception {
        manager.removeAllListeners();
        manager.contextDeactivated("1", "1");
        project.dispose();
    }
    
    public void testNavigation() throws JavaModelException, PartInitException {
        CompilationUnitEditor editorPart = (CompilationUnitEditor)JavaUI.openInEditor(caller);

        monitor.selectionChanged(part, new StructuredSelection(caller));
        
        Document document = new Document(typeFoo.getCompilationUnit().getSource());
        
        TextSelection callerSelection = new TextSelection(document,
                typeFoo.getCompilationUnit().getSource().indexOf("callee();"),
                "callee".length());
        editorPart.setHighlightRange(callerSelection.getOffset(), callerSelection.getLength(), true);
        monitor.selectionChanged(editorPart, callerSelection);
        
        TextSelection calleeSelection = new TextSelection(document,
                callee.getSourceRange().getOffset(),
                callee.getSourceRange().getLength());
        editorPart.setHighlightRange(callerSelection.getOffset(),callerSelection.getLength(), true);
        monitor.selectionChanged(editorPart, calleeSelection);
        
        IMylarContextNode callerNode = manager.getNode(caller.getHandleIdentifier());
        IMylarContextNode calleeNode = manager.getNode(callee.getHandleIdentifier());
        assertTrue(callerNode.getDegreeOfInterest().isInteresting());
        assertTrue(calleeNode.getDegreeOfInterest().isInteresting());
        assertEquals(1, callerNode.getEdges().size());
        
        TextSelection callerAgain = new TextSelection(document,
                typeFoo.getCompilationUnit().getSource().indexOf("callee();"),
                "callee".length());
        editorPart.setHighlightRange(callerAgain.getOffset(), callerAgain.getLength(), true);
        monitor.selectionChanged(editorPart, callerSelection);
        assertTrue(calleeNode.getEdges().size() == 1);
    }

}
