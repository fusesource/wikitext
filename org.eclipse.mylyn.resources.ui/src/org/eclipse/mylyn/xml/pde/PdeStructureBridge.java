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
 * Created on Apr 20, 2005
  */
package org.eclipse.mylar.xml.pde;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.mylar.core.AbstractRelationshipProvider;
import org.eclipse.mylar.core.IDegreeOfSeparation;
import org.eclipse.mylar.core.IMylarStructureBridge;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.core.internal.DegreeOfSeparation;
import org.eclipse.mylar.xml.XmlNodeHelper;
import org.eclipse.mylar.xml.XmlReferencesProvider;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.model.build.BuildEntry;
import org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.markers.internal.ProblemMarker;


/**
 * @author Mik Kersten
 * 
 */
public class PdeStructureBridge implements IMylarStructureBridge {

    public final static String CONTENT_TYPE = "plugin.xml";
    private List<AbstractRelationshipProvider> providers;
    private IMylarStructureBridge parentBridge;
    
    public PdeStructureBridge() {
        providers = new ArrayList<AbstractRelationshipProvider>();
        providers.add(new XmlReferencesProvider());
        
    }
    
    /**
     * @see org.eclipse.mylar.core.IMylarStructureBridge#getResourceExtension()
     */
    public String getResourceExtension() {
        return CONTENT_TYPE;
    }
    
    public String getResourceExtension(String elementHandle) {
        if (elementHandle.endsWith(".xml")) {
            return parentBridge.getResourceExtension();
        } else {
            return CONTENT_TYPE;
        }
    }
    
    /**
     * @see org.eclipse.mylar.core.IMylarStructureBridge#getParentHandle(java.lang.String)
     */
    public String getParentHandle(String handle) {
        // we can only get the parent if we have a PluginObjectNode
        
        Object o = getObjectForHandle(handle);
        if(o instanceof PluginObjectNode){
       
            // try to get the parent
            PluginObjectNode parent = (PluginObjectNode)((PluginObjectNode)o).getParentNode();

            if(parent != null){
                // get the handle for the parent
                return getHandleIdentifier(parent);
            } else{
                // the parent is the plugin.xml file, so return that handle
                int delimeterIndex = handle.indexOf(";");
                if (delimeterIndex != -1) {
                    String parentHandle = handle.substring(0, delimeterIndex);
                    return parentHandle;
                } else{
                    return null;
                }
            }
        } else if (o instanceof IFile) {
//            String fileHandle = parentBridge.getParentHandle(handle);
            return parentBridge.getParentHandle(handle);
        } else {
            return null;
        }
    }

    /**
     * @see org.eclipse.mylar.core.IMylarStructureBridge#getObjectForHandle(java.lang.String)
     */
    public Object getObjectForHandle(String handle) {
        if (handle == null) return null;
        int first = handle.indexOf(";");
        String filename = "";
        if(first == -1){
            return parentBridge.getObjectForHandle(handle);
            // the handle is for the plugin.xml file, so just return the File
//            filename = handle;
//            IPath path = new Path(filename);
//            IFile f = (IFile)((Workspace)ResourcesPlugin.getWorkspace()).newResource(path, IResource.FILE);
//            return f;
        }
        else{
            // extract the filename from the handle since it represents a node
            filename = handle.substring(0, first);
        }
        
        try{
            // get the file and create a FileEditorInput
            IPath path = new Path(filename);
            IFile f = (IFile)((Workspace)ResourcesPlugin.getWorkspace()).newResource(path, IResource.FILE);
            
            // get the start line for the element
            int start = Integer.parseInt(handle.substring(first + 1));
            
//            // get the content and the document so that we can get the offset
//            String content = XmlNodeHelper.getContents(f.getContents());
//            IDocument d = new Document(content);
            
            // get the offsets for the element
            // make sure that we are on a character and not whitespace
//            int offset = d.getLineOffset(start);
//            while(d.getChar(offset) == ' ')
//                offset++;
            
            // get the current editor which should be the ManifestEditor so that we can get the element that we want
            IEditorPart editorPart = null;
            try{
                editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            }catch(NullPointerException e){
                // do nothing, this just means that there is no active page
            }
            if(editorPart != null && editorPart instanceof ManifestEditor){
                PluginObjectNode node = PdeEditingMonitor.getNode((ManifestEditor)editorPart, start, true);
                // get the element based on the offset
                return node;
            }else{
				String content = XmlNodeHelper.getContents(f.getContents());
				IDocument d = new Document(content);
                PluginObjectNode node = PdeEditingMonitor.getNode(d, f, start, true);
                return node;
            }
        }catch(Exception e){
        	// ignore, means file doesn't exist
//        	MylarPlugin.log(e, "handle failed");
        }
        return null;
    }
    
    /**
     * Handle is filename;hashcodeOfElementAndAttributes
     * @see org.eclipse.mylar.core.IMylarStructureBridge#getHandleIdentifier(java.lang.Object)
     */
    public String getHandleIdentifier(Object object) {
        // we can only create handles for PluginObjectNodes and plugin.xml files
        if (object instanceof XmlNodeHelper) {
            return ((XmlNodeHelper)object).getHandle();
        } else if (object instanceof PluginObjectNode) {
            PluginObjectNode node = (PluginObjectNode)object;
            try{
                // get the handle for the PluginObjectNode
            	if(node.getModel() == null || node.getModel().getUnderlyingResource() == null || node.getModel().getUnderlyingResource().getFullPath() == null){
            		MylarPlugin.log("PDE xml node's resource or model is null: " + node.getName(), this);
            		return null;
            	}
                IPath path = new Path(node.getModel().getUnderlyingResource().getFullPath().toString());
                IFile file = (IFile)((Workspace)ResourcesPlugin.getWorkspace()).newResource(path, IResource.FILE);
                String handle = new XmlNodeHelper(file.getFullPath().toString(), PdeEditingMonitor.getStringOfNode(node).hashCode()).getHandle();
                return handle;
            }catch(Exception e){
            	MylarPlugin.log(e, "pde handle failed");
            }
            
        }else if (object instanceof File) {
            // get the handle for the file if it is plugin.xml
            File file = (File)object;
            if (file.getFullPath().toString().endsWith("plugin.xml")) return file.getFullPath().toString();
        }
        return null;
    }

    /**
     * @see org.eclipse.mylar.core.IMylarStructureBridge#getName(java.lang.Object)
     */
    public String getName(Object object) {
        if(object instanceof PluginObjectNode){
            PluginObjectNode node = (PluginObjectNode)object;
            String name = node.getXMLAttributeValue("name");
            if (name == null)
                name = node.getXMLTagName();
            name = node.getModel().getUnderlyingResource().getName() + ": " + name;
            return name;
        }else if (object instanceof File) {
            File file = (File)object;
            if (file.getFullPath().toString().endsWith("plugin.xml")) return "plugin.xml";
        }
        return "";
    }

    /**
     * @see org.eclipse.mylar.core.IMylarStructureBridge#canBeLandmark(Object)
     * 
     * TODO: make a non-handle based test
     */
    public boolean canBeLandmark(String handle) {
        return handle.indexOf(';') == -1;
    }

    /**
     * @see org.eclipse.mylar.core.IMylarStructureBridge#acceptsObject(java.lang.Object)
     */
    public boolean acceptsObject(Object object) {
        // we only accept PluginObjectNodes and plugin.xml Files
        if (object instanceof PluginObjectNode || object instanceof BuildEntry) {
            return true;
        } else if (object instanceof XmlNodeHelper){
            if (((XmlNodeHelper)object).getFilename().endsWith("plugin.xml")) return true;
        } else if (object instanceof File) {
            File file = (File)object;
            if (file.getFullPath().toString().endsWith("plugin.xml")) return true;
        }
        return false;
    }

    /**
     * @see org.eclipse.mylar.core.IMylarStructureBridge#canFilter(java.lang.Object)
     */
    public boolean canFilter(Object element) {
        return true;
    }

    /**
     * @see org.eclipse.mylar.core.IMylarStructureBridge#isDocument(java.lang.String)
     */
    public boolean isDocument(String handle) {
        return handle.indexOf(';') == -1;
    }

    /**
     * @see org.eclipse.mylar.core.IMylarStructureBridge#getHandleForMarker(org.eclipse.ui.views.markers.internal.ProblemMarker)
     */
    public String getHandleForOffsetInObject(Object resource, int offset) {
    	if (resource == null) return null;
    	if(resource instanceof ProblemMarker) {
    		ProblemMarker marker = (ProblemMarker)resource;

	        // we can only get a handle for a marker with the resource plugin.xml
	        try {
	            IResource res= marker.getResource();
	
	            if (res instanceof IFile) {
	                IFile file = (IFile)res; 
	                if (file.getFullPath().toString().endsWith("plugin.xml")) { 
	                    return file.getFullPath().toString();
	                } else {
	                    return null;
	                }
	            }
	            return null;
	        }
	        catch (Throwable t) {
	            MylarPlugin.log(t, "Could not find element for: " + marker);
	            return null;
	        }
    	} else if(resource instanceof IFile){
    		try {
	            IFile file = (IFile)resource; 
	            if (file.getFullPath().toString().endsWith("plugin.xml")) {
	                String content = XmlNodeHelper.getContents(file.getContents());
					IDocument d = new Document(content);
	                PluginObjectNode node = PdeEditingMonitor.getNode(d, file, offset, false);
	                String handle = new XmlNodeHelper(file.getFullPath().toString(), PdeEditingMonitor.getStringOfNode(node).hashCode()).getHandle();
	                return handle;
	            }
    		} catch(Exception e){
    			MylarPlugin.log(e, "Unable to get handle for offset in object");
    		}
    	}
    	return null;
    }
    
	public IProject getProjectForObject(Object object) {
		while(!(object instanceof IFile)){
    		String handle = getParentHandle(getHandleIdentifier(object));
    		if(handle == null)
    			break;
    		object = getObjectForHandle(handle);
    	}
    	if(object instanceof IFile && acceptsObject(object)){
    		return((IFile)object).getProject();
    	}
		return null;
	}

	/**
	 * HACK: This is weird that the relationship provider is only here.
	 * There are relly 3 different bridges, 2 specific and 1 generic
	 */
	public List<AbstractRelationshipProvider> getProviders() {
		return providers;
	}
	
	public List<IDegreeOfSeparation> getDegreesOfSeparation() {
		List <IDegreeOfSeparation> separations = new ArrayList<IDegreeOfSeparation>();
		separations.add(new DegreeOfSeparation("disabled", 0));
		separations.add(new DegreeOfSeparation("landmark files", 1));
		separations.add(new DegreeOfSeparation("interesting files", 2));
		separations.add(new DegreeOfSeparation("interesting project", 3));
		separations.add(new DegreeOfSeparation("project dependencies", 4));
		separations.add(new DegreeOfSeparation("entire workspace", 5));

		return separations;
	}

	public void setParentBridge(IMylarStructureBridge bridge) {
		parentBridge = bridge;
	}
}
