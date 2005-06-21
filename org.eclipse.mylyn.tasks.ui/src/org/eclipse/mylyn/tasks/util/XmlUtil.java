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
 * Created on Jan 17, 2005
 */
package org.eclipse.mylar.tasks.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.tasks.BugzillaTask;
import org.eclipse.mylar.tasks.ITask;
import org.eclipse.mylar.tasks.Task;
import org.eclipse.mylar.tasks.TaskList;
import org.eclipse.mylar.tasks.BugzillaTask.BugTaskState;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * @author Ken Sueda
 */
public class XmlUtil {
	
	private static String readVersion = "";

	/**
	 * 
	 * @param tlist
	 * @param outFile
	 */
	public static void writeTaskList(TaskList tlist, File outFile) {
    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document doc = null;

		try {
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		Element root = doc.createElement("TaskList");
		root.setAttribute("Version", "1.0.0");

		// iterate through each subtask and externalize those
		//
		for (int i = 0; i < tlist.getRootTasks().size(); i++) {
			writeTask(tlist.getRootTasks().get(i), doc, root);
		}
		doc.appendChild(root);
		writeDOMtoFile(doc, outFile);
		return;
	}
	
	/**
	 * Writes an XML file from a DOM.
	 * 
	 * doc  - the document to write
	 * file - the file to be written to
	 */
	public static void writeDOMtoFile(Document doc, File file) {
		try {
			// A file output stream is an output stream for writing data to a File
			//
			OutputStream outputStream = new FileOutputStream(file);
			writeDOMtoStream(doc, outputStream);
			outputStream.flush();
			outputStream.close();
		} catch (Exception fnfe) {
			MylarPlugin.log("Tasklist could not be found");
		}
	}

	/**
	 * Writes the provided XML document out to the specified output stream.
	 * 
	 * doc - the document to be written
	 * outputStream - the stream to which the document is to be written
	 */
	public static void writeDOMtoStream(Document doc, OutputStream outputStream) {
		// Prepare the DOM document for writing
		// DOMSource - Acts as a holder for a transformation Source tree in the 
		// form of a Document Object Model (DOM) tree
		//
		Source source = new DOMSource(doc);

		// StreamResult - Acts as an holder for a XML transformation result
		// Prepare the output stream
		//
		Result result = new StreamResult(outputStream);

		// An instance of this class can be obtained with the 
		// TransformerFactory.newTransformer  method. This instance may 
		// then be used to process XML from a variety of sources and write 
		// the transformation output to a variety of sinks
		//

		Transformer xformer = null;
		try {
			xformer = TransformerFactory.newInstance().newTransformer();
			//Transform the XML Source to a Result
			//
			xformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * 
	 * @param t
	 * @param doc
	 * @param root
	 */
	public static void writeTask(ITask t, Document doc, Element root) {

		// create node and set attributes
		//    	
		Element node = doc.createElement("Task");
		node.setAttribute("Path", t.getPath());
		node.setAttribute("Label", t.getLabel());
		node.setAttribute("Handle", t.getHandle());
		node.setAttribute("Priority", t.getPriority());

		if (t.isCategory()) {
			node.setAttribute("IsCategory", "true");
		} else {
			node.setAttribute("IsCategory", "false");
		}
		if (t.isCompleted()) {
			node.setAttribute("Complete", "true");
		} else {
			node.setAttribute("Complete", "false");
		}
		if (t.isActive()) {
			node.setAttribute("Active", "true");
		} else {
			node.setAttribute("Active", "false");
		}
		if (t instanceof BugzillaTask) {
			BugzillaTask bt = (BugzillaTask) t;
			node.setAttribute("Bugzilla", "true");
			node.setAttribute("LastDate", new Long(bt.getLastRefreshTime()
					.getTime()).toString());
			if (bt.isDirty()) {
				node.setAttribute("Dirty", "true");
			} else {
				node.setAttribute("Dirty", "false");
			}
			bt.saveBugReport(false);
		} else {
			node.setAttribute("Bugzilla", "false");
		}
		node.setAttribute("Notes", t.getNotes());
		node.setAttribute("Elapsed", t.getElapsedTime());
		node.setAttribute("Estimated", t.getEstimatedTime());
		List<String> rl = t.getRelatedLinks().getLinks();
		int i = 0;
		for (String link : rl) {
			node.setAttribute("link"+i, link);
			i++;
		}
		
		List<ITask> children = t.getChildren();

		i = 0; 
		for (i = 0; i < children.size(); i++) {
			writeTask(children.get(i), doc, node);
		}

		// append new node to root node
		//
		root.appendChild(node);
		return;
	}

	public static void readTaskList(TaskList tlist, File inFile) {
		try {
			// parse file
			//
			Document doc = openAsDOM(inFile);

			// read root node to get version number
			//
			Element root = doc.getDocumentElement();
			readVersion = root.getAttribute("Version");

			NodeList list = root.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				Node child = list.item(i);
				tlist.addRootTask(readTask(child, null, tlist));
			}
		} catch (Exception e) {
			String name = inFile.getAbsolutePath();
			name = name.substring(0, name.lastIndexOf('.')) + "-save.xml";
			inFile.renameTo(new File(name));
			MylarPlugin.log("XmlUtil", e);
		}
	}

	/**
	 * Opens the specified XML file and parses it into a DOM Document.
	 * 
	 * Filename - the name of the file to open
	 * Return   - the Document built from the XML file
	 * Throws   - XMLException if the file cannot be parsed as XML
	 *          - IOException if the file cannot be opened
	 */
	public static Document openAsDOM(File inputFile) throws IOException {

		// A factory API that enables applications to obtain a parser 
		// that produces DOM object trees from XML documents
		//
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		// Using DocumentBuilder, obtain a Document from XML file.
		//
		DocumentBuilder builder = null;
		Document document = null;
		try {
			// create new instance of DocumentBuilder
			//
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			inputFile.renameTo(new File(inputFile.getName() + "save.xml"));
			MylarPlugin.log("Failed to load XML file", pce);
		}
		try {
			// Parse the content of the given file as an XML document 
			// and return a new DOM Document object. Also throws IOException
			document = builder.parse(inputFile);
		} catch (SAXException se) {
			inputFile.renameTo(new File(inputFile.getName() + "save.xml"));
			MylarPlugin.log("Failed to parse XML file", se);
		}
		return document;
	}
	
	public static ITask readTask(Node node, ITask root, TaskList tlist) {
		//extract node and create new sub task
		//
		Element e = (Element) node;
		ITask t;
		String handle = "";
		if (e.hasAttribute("ID")) {
			handle = e.getAttribute("ID");
		} else {
			handle = e.getAttribute("Handle");
		}
		
		String label = e.getAttribute("Label");
		String priority = e.getAttribute("Priority");

		if (e.getAttribute("Bugzilla").compareTo("true") == 0) {
			t = new BugzillaTask(handle, label, true);
			BugzillaTask bt = (BugzillaTask) t;
			bt.setState(BugTaskState.FREE);
			bt.setLastRefresh(new Date(new Long(e.getAttribute("LastDate"))
					.longValue()));
			if (e.getAttribute("Dirty").compareTo("true") == 0) {
				bt.setDirty(true);
			} else {
				bt.setDirty(false);
			}
			if (bt.readBugReport() == false) {
				MylarPlugin.log("Failed to read bug report");
			}
		} else {
			t = new Task(handle, label);			
		}
		t.setPriority(priority);
		t.setPath(e.getAttribute("Path"));
		
		if (e.getAttribute("Active").compareTo("true") == 0) {
			t.setActive(true);
			tlist.setActive(t, true);
		} else {
			t.setActive(false);
		}

		if (e.getAttribute("Complete").compareTo("true") == 0) {
			t.setCompleted(true);
		} else {
			t.setCompleted(false);
		}
		if (e.getAttribute("IsCategory").compareTo("true") == 0) {
			t.setIsCategory(true);
		} else {
			t.setIsCategory(false);
		}

		if (e.hasAttribute("Notes")) {
			t.setNotes(e.getAttribute("Notes"));			
		} else {
			t.setNotes("");
		}
		if (e.hasAttribute("Elapsed")) {
			t.setElapsedTime(e.getAttribute("Elapsed"));			
		} else {
			t.setElapsedTime("");
		}
		if (e.hasAttribute("Estimated")) {
			t.setEstimatedTime(e.getAttribute("Estimated"));			
		} else {
			t.setEstimatedTime("");
		}
		
		int i = 0;
		while (e.hasAttribute("link"+i)) {
			t.getRelatedLinks().add(e.getAttribute("link"+i));
			i++;
		}
				
		if (!readVersion.equals("1.0.0")) {
			// for newer revisions
			// XXX: readVersion had to be read once to remove warning..
		}

		i = 0;
		NodeList list = e.getChildNodes();
		for (i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			t.addSubtask(readTask(child, t, tlist));
		}
		if (root != null) {
			t.setParent(root);
		}
		return t;
	}	
}

