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
package org.eclipse.mylar.xml.tests;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.mylar.core.IMylarContextNode;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.core.internal.CompositeContext;
import org.eclipse.mylar.core.internal.Context;
import org.eclipse.mylar.core.resources.ResourceStructureBridge;
import org.eclipse.mylar.core.search.IMylarSearchOperation;
import org.eclipse.mylar.core.tests.support.WorkspaceSetupHelper;
import org.eclipse.mylar.core.tests.support.search.ISearchPluginTest;
import org.eclipse.mylar.core.tests.support.search.SearchPluginTestHelper;
import org.eclipse.mylar.core.tests.support.search.SearchTaskscapeNotifier;
import org.eclipse.mylar.java.JavaStructureBridge;
import org.eclipse.mylar.xml.XmlReferencesProvider;
import org.eclipse.mylar.xml.pde.PdeStructureBridge;


/*TEST CASES TO HANDLE
 * 1. all dos
 * 		- with and without results
 * TODO 	- in both the plugin.xml and the build.xml
 * 2. different type of xml file with and without reference - shouldn't have result
 * 
 * DEGREE OF SEPARATIONS
 * 1 xml landmark files
 * 2 projects of any landmark
 * 3 workspace
 * 4 workspace
 * 5 NONE
 */

public class XMLSearchPluginTest extends TestCase implements ISearchPluginTest{

	private IType type1;
	private IType type2;
	private IFile plugin1;
	private IFile plugin2;
	private IFile tocRefs;
	private IFile tocNoRefs;
	private IJavaProject jp1;
	private IJavaProject jp2;
	private static final String SOURCE_ID = "XMLSearchTest";
	private SearchPluginTestHelper helper;
	
	
	@Override
    protected void setUp() throws Exception {
    	MylarPlugin.getContextManager().getRelationshipProviders().clear();
    	WorkspaceSetupHelper.setupWorkspace();
    	jp1 = WorkspaceSetupHelper.getProject1();
    	jp2 = WorkspaceSetupHelper.getProject2();
    	type1 = WorkspaceSetupHelper.getType(jp1, "org.eclipse.mylar.tests.project1.views.SampleView");
    	type2 = WorkspaceSetupHelper.getType(jp2, "org.eclipse.mylar.tests.project2.builder.SampleBuilder.SampleResourceVisitor");
    	plugin1 = WorkspaceSetupHelper.getFile(jp1, "plugin.xml");
    	tocRefs = WorkspaceSetupHelper.getFile(jp1, "toc-refs.xml");
    	tocNoRefs = WorkspaceSetupHelper.getFile(jp1, "toc-no-refs.xml");
    	plugin2 = WorkspaceSetupHelper.getFile(jp2, "plugin.xml");
    	
    	Context t = WorkspaceSetupHelper.getTaskscape();
    	MylarPlugin.getContextManager().taskActivated(t.getId(), t.getId());
    	helper = new SearchPluginTestHelper(this);
    }
    
    @Override
    protected void tearDown() throws Exception {
        WorkspaceSetupHelper.clearDoiModel();
    }
  
    public void testXMLSearchDOS1() throws IOException, CoreException{
    	
    	int dos = 1;
    	
    	
        CompositeContext t = MylarPlugin.getContextManager().getActiveContext();
		SearchTaskscapeNotifier notifier = new SearchTaskscapeNotifier(t, SOURCE_ID);
		IMylarContextNode searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
    	
    	//
    	// results should be null since the scope would be null.
    	// There are no landmarks to search over
		helper.searchResultsNull(notifier, searchNode, dos);
		//
		//
		
		//
		// add an element to the taskscape, results should still be null
		// There is a landmark, but not one that is an xml file that we care about
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNull(notifier, type1.getHandleIdentifier(), ResourceStructureBridge.EXTENSION, searchNode, dos);
		
		//
		//
		
		//
		// add an element to the taskscape, results should still be null
		// There is a landmark that has references in it, but not one that is an xml file that we care about
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNull(notifier, tocRefs.getFullPath().toString(), ResourceStructureBridge.EXTENSION, searchNode, dos);
		//
		//
		
		//
		// add an element to the taskscape, results should still be null
		// There is a landmark, but not one that is an xml file that we care about
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNull(notifier, tocNoRefs.getFullPath().toString(), ResourceStructureBridge.EXTENSION, searchNode, dos);
		//
		//
		
		//
		// add the plugin.xml from a different project to the taskscape, should have non null results, but 0 size
		// There is a lanmark that can be added to create a scope with the proper xml file type
		// but it is in the wrong project and shouldn't have any references
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, plugin2.getFullPath().toString(), PdeStructureBridge.EXTENSION, searchNode, dos, 0);
		//
		//
		
		//
		// add the plugin.xml to the taskscape, should have results now
		// We should get the results now since we have the proper xml file as the landmark now
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, plugin1.getFullPath().toString(), PdeStructureBridge.EXTENSION, searchNode, dos, 3);
		//
		//
    }
	
	public void testXMLSearchDOS2()throws IOException, CoreException{

		int dos = 2;
		
        CompositeContext t = MylarPlugin.getContextManager().getActiveContext();
		SearchTaskscapeNotifier notifier = new SearchTaskscapeNotifier(t, SOURCE_ID);
		IMylarContextNode searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		
		//
    	// results should be null since the scope would be null.
    	// There are no landmarks and therefore no projects to search over
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNull(notifier, searchNode, dos);
		//
		//
		
		//
		// add an element to the taskscape, results should not be null
		// There is a landmark with references in it, but not one that is an xml file that we care about
		// therefore, we still only get 3 references - landmark is in the same project
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, tocRefs.getFullPath().toString(), ResourceStructureBridge.EXTENSION, searchNode, dos, 3);
		//
		//
		
		//
		// add an element to the taskscape, results should not be null, but only 3
		// There is a landmark, but not one that is an xml file that we care about - landmark is in the same project
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, tocNoRefs.getFullPath().toString(), ResourceStructureBridge.EXTENSION, searchNode, dos, 3);
		//
		//
		
		//
		// add the plugin.xml from a different project to the taskscape, should have non null results, but 0 size
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, plugin2.getFullPath().toString(), PdeStructureBridge.EXTENSION, searchNode, dos, 0);
		//
		//
		
		//
		// add java element from the same project, should get result since we are looking at the projects
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 3);
		//
		//
		
		//
		// add a java element from a different project, should get non null result, but 0 size
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, type2.getHandleIdentifier(), JavaStructureBridge.EXTENSION, searchNode, dos, 0);
		//
		//
		
		//
		// add the plugin.xml from the same project to the taskscape, should have results
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, plugin1.getFullPath().toString(), PdeStructureBridge.EXTENSION, searchNode, dos, 3);
		//
		//
	}
	
	public void testXMLSearchDOS3()throws IOException, CoreException{
		
		int dos = 3;
		
        CompositeContext t = MylarPlugin.getContextManager().getActiveContext();
		SearchTaskscapeNotifier notifier = new SearchTaskscapeNotifier(t, SOURCE_ID);
		IMylarContextNode searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		
		//
		// add an element to the taskscape, results should not be null
		// There is a landmark with references in it, but not one that is an xml file that we care about
		// therefore, we still only get 3 references
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, tocRefs.getFullPath().toString(), ResourceStructureBridge.EXTENSION, searchNode, dos, 3);
		//
		//
		
		//
		// add an element to the taskscape, results should still be null
		// There is a landmark, but not one that is an xml file that we care about
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, tocNoRefs.getFullPath().toString(), ResourceStructureBridge.EXTENSION, searchNode, dos, 3);
		//
		//
		
		//
		// we should get all results since we are searching the entire workspace
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, searchNode, dos, 3);
		//
		//

		//
		// we should get 0 results since there should be no references to the type we are looking at
		searchNode = notifier.getElement(type2.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, searchNode, dos, 0);
		//
		//
	}
	
	public void testXMLSearchDOS4()throws IOException, CoreException{
		// right now, dos 3 and 4 are exactly the same, workspace scope
		
		int dos = 4;
		
        CompositeContext t = MylarPlugin.getContextManager().getActiveContext();
		SearchTaskscapeNotifier notifier = new SearchTaskscapeNotifier(t, SOURCE_ID);
		IMylarContextNode searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		
		//
		// add an element to the taskscape, results should not be null
		// There is a landmark with references in it, but not one that is an xml file that we care about
		// therefore, we still only get 3 references
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, tocRefs.getFullPath().toString(), ResourceStructureBridge.EXTENSION, searchNode, dos, 3);
		//
		//
		
		//
		// add an element to the taskscape, results should still be null
		// There is a landmark, but not one that is an xml file that we care about
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, tocNoRefs.getFullPath().toString(), ResourceStructureBridge.EXTENSION, searchNode, dos, 3);
		//
		//
		
		//
		// we should get all results since we are searching the entire workspace
		searchNode = notifier.getElement(type1.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, searchNode, dos, 3);
		//
		//

		//
		// we should get 0 results since there should be no references to the type we are looking at
		searchNode = notifier.getElement(type2.getHandleIdentifier(), JavaStructureBridge.EXTENSION);
		helper.searchResultsNotNull(notifier, searchNode, dos, 0);
		//
		//
	}
	
	
	public List<?> search(int dos, IMylarContextNode node) throws IOException, CoreException{
		if(node == null)
			return null;
		
		// test with each of the sepatations	
		XmlReferencesProvider prov = new XmlReferencesProvider();

		IMylarSearchOperation o = prov.getSearchOperation(node, 0, dos);
		if(o == null) return null;
		
		XMLTestActiveSearchListener l = new XMLTestActiveSearchListener(prov);
		SearchPluginTestHelper.search(o, l);

		return l.getResults();
	}
}
