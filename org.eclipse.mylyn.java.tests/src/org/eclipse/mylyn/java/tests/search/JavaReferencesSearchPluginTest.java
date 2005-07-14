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
package org.eclipse.mylar.java.tests.search;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.mylar.core.IMylarContextNode;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.core.internal.CompositeContext;
import org.eclipse.mylar.core.internal.Context;
import org.eclipse.mylar.core.search.IMylarSearchOperation;
import org.eclipse.mylar.core.tests.support.WorkspaceSetupHelper;
import org.eclipse.mylar.core.tests.support.search.ISearchPluginTest;
import org.eclipse.mylar.core.tests.support.search.SearchPluginTestHelper;
import org.eclipse.mylar.core.tests.support.search.SearchTaskscapeNotifier;
import org.eclipse.mylar.core.tests.support.search.TestActiveSearchListener;
import org.eclipse.mylar.java.JavaStructureBridge;
import org.eclipse.mylar.java.search.JavaReferencesProvider;
import org.eclipse.mylar.xml.pde.PdeStructureBridge;

public class JavaReferencesSearchPluginTest extends TestCase implements ISearchPluginTest{

	private IType type1;
	private IType type11;
	private IType type2;
	private IFile plugin1;
	private IJavaProject jp1;
	private IJavaProject jp2;
	private static final String SOURCE_ID = "JavaReferencesSearchTest";
	private SearchPluginTestHelper helper;
	
	@Override
    protected void setUp() throws Exception {
    	MylarPlugin.getTaskscapeManager().getRelationshipProviders().clear();
    	WorkspaceSetupHelper.setupWorkspace();
    	jp1 = WorkspaceSetupHelper.getProject1();
    	jp2 = WorkspaceSetupHelper.getProject2();
    	type1 = WorkspaceSetupHelper.getType(jp1, "org.eclipse.mylar.tests.project1.views.SampleView");
    	type11 = WorkspaceSetupHelper.getType(jp1, "org.eclipse.mylar.tests.project1.Project1Plugin");
    	type2 = WorkspaceSetupHelper.getType(jp2, "org.eclipse.mylar.tests.project2.builder.ToggleNatureAction");
    	plugin1 = WorkspaceSetupHelper.getFile(jp1, "plugin.xml");
    	
    	Context t = WorkspaceSetupHelper.getTaskscape();
    	MylarPlugin.getTaskscapeManager().taskActivated(t.getId(), t.getId());
    	helper = new SearchPluginTestHelper(this);
    }
    
    @Override
    protected void tearDown() throws Exception {
        WorkspaceSetupHelper.clearDoiModel();
    }
	
	public void testJavaReferencesSearchDOS1() throws IOException, CoreException{
		
		int dos = 1;
    	
        CompositeContext t = MylarPlugin.getTaskscapeManager().getActiveTaskscape();
		SearchTaskscapeNotifier notifier = new SearchTaskscapeNotifier(t, SOURCE_ID);
		IMylarContextNode searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);

    	//
    	// results should be null since the scope would be null.
    	// There are no landmarks to search over
		helper.searchResultsNull(notifier, searchNode, dos);
		//
		//
		
		//
    	// results should be not null, but have no references since the landmark
		// is an element in a different project
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type2.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 0);
		//
		//
		
		//
    	// results should be not null, but have no references since the landmark
		// is an element in the same project, but there are no references in it 
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type11.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 0);
		//
		//
		
		//
    	// results should be not null, but have no references
		// This file type should never affect the scope
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNull(notifier, plugin1.getFullPath().toString(), PdeStructureBridge.EXTENSION, searchNode, dos);
		//
		//
		
		//
    	// results should be not null, and there should be 1 reference since we are searching
		// the file with the element in it
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 1);
		//
		//
	}
	
	public void testJavaReferencesSearchDOS2() throws CoreException, IOException{
		int dos = 2;
		
        CompositeContext t = MylarPlugin.getTaskscapeManager().getActiveTaskscape();
		SearchTaskscapeNotifier notifier = new SearchTaskscapeNotifier(t, SOURCE_ID);
		IMylarContextNode searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
    	
    	
    	//
    	// results should be null since the scope would be null.
    	// There are no landmarks to search over
		helper.searchResultsNull(notifier, searchNode, dos);
		//
		//
		
		//
    	// results should be not null, but have no references since the landmark
		// is an element in a different project
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type2.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 0);
		//
		//
		
		//
    	// results should be not null, but have no references since the interesting element
		// is an element in the same project, but no references in it
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type11.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 0);
		//
		//
		
		//
    	// results should be not null, but have no references
		// This file type should never affect the scope
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNull(notifier, plugin1.getFullPath().toString(), PdeStructureBridge.EXTENSION, searchNode, dos);
		//
		//
		
		//
    	// results should be not null, and we should get 1 result back
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 1);
		//
		//
		
		//
    	// results should be null, since we have nothing to search 
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNull(notifier, searchNode, dos);
		
		//
		//
		
		//
    	// results should be not null, and we should get 1 result back
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNullInteresting(notifier, type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 1);
		//
		//
	}
	
	public void testJavaReferencesSearchDOS3() throws Exception{
		int dos = 3;
    	
        CompositeContext t = MylarPlugin.getTaskscapeManager().getActiveTaskscape();
		SearchTaskscapeNotifier notifier = new SearchTaskscapeNotifier(t, SOURCE_ID);
		IMylarContextNode searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
    	
    	//
    	// results should be null since the scope would be null.
    	// There are no landmarks to search over
		helper.searchResultsNull(notifier, searchNode, dos);
		//
		//
		
		//
    	// results should be not null, but have no references since the landmark
		// is an element in a different project
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type2.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 0);
		//
		//
		
		//
    	// results should be not null, and have 1 reference since the project is the same
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNullInteresting(notifier, type11.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 1);
		//
		//
		
		// 
    	// results should be not null, and have 1 reference
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, plugin1.getFullPath().toString(), PdeStructureBridge.EXTENSION, searchNode, dos, 1);
		//
		//
		
		//
    	// results should be not null, and we should get 1 result back
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 1);
		//
		//
		
		//
    	// results should be null, since we have nothing to search 
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNull(notifier, searchNode, dos);
		//
		//
		
		//
    	// results should be not null, and we should get 1 result back
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNullInteresting(notifier, type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 1);
		//
		//
	}
	
	public void testJavaReferencesSearchDOS4() throws Exception{
		// TODO this is the same as 3, but there are some flags to search libraries...we should check this too
		
		int dos = 4;
    	
        CompositeContext t = MylarPlugin.getTaskscapeManager().getActiveTaskscape();
		SearchTaskscapeNotifier notifier = new SearchTaskscapeNotifier(t, SOURCE_ID);
		IMylarContextNode searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
    	
    	//
    	// results should be null since the scope would be null.
    	// There are no landmarks to search over
		helper.searchResultsNull(notifier, searchNode, dos);
		//
		//
		
		//
    	// results should be not null, but have no references since the landmark
		// is an element in a different project
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type2.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 0);
		//
		//
		
		//
    	// results should be not null, and have 1 reference since the project is the same
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNullInteresting(notifier, type11.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 1);
		//
		//
		
		// 
    	// results should be not null, and have 1 reference
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, plugin1.getFullPath().toString(), PdeStructureBridge.EXTENSION, searchNode, dos, 1);
		//
		//
		
		//
    	// results should be not null, and we should get 1 result back
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 1);
		//
		//
		
		//
    	// results should be null, since we have nothing to search 
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNull(notifier, searchNode, dos);
		//
		//
		
		//
    	// results should be not null, and we should get 1 result back
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNullInteresting(notifier, type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 1);
		//
		//
	}
	
	public void testJavaReferencesSearchDOS5() throws IOException, CoreException{
		int dos = 5;
		
        CompositeContext t = MylarPlugin.getTaskscapeManager().getActiveTaskscape();
		SearchTaskscapeNotifier notifier = new SearchTaskscapeNotifier(t, SOURCE_ID);
		IMylarContextNode searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
    	
    	//
    	// we should have 1 result since we are searching the entire workspace
		helper.searchResultsNotNull(notifier, searchNode, dos, 1);
		//
		//
		
		//
    	// we should have no results since there are no java references in the workspace
		searchNode = notifier.getElement(type2.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, searchNode, dos, 0);
		//
		//
	}
	
	public List<?> search(int dos, IMylarContextNode node){

		if(node == null)
			return null;
		
		// test with each of the sepatations	
		JavaReferencesProvider prov = new JavaReferencesProvider();

		TestActiveSearchListener l =new TestActiveSearchListener(prov);
		IMylarSearchOperation o = prov.getSearchOperation(node, IJavaSearchConstants.REFERENCES, dos);
		if(o == null) return null;
		
		SearchPluginTestHelper.search(o, l);
		return l.getResults();
	}	
}
