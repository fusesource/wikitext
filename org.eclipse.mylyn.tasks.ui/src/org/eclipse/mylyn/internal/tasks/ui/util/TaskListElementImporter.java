/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.internal.tasks.ui.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.context.core.InteractionContextExternalizer;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.AbstractTaskCategory;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.internal.tasks.core.RepositoryModel;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.TaskExternalizationException;
import org.eclipse.mylyn.internal.tasks.core.TaskList;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoriesExternalizer;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryManager;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractTaskListFactory;
import org.eclipse.mylyn.internal.tasks.core.externalization.DelegatingTaskExternalizer;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.AbstractTaskListMigrator;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Mik Kersten
 * @author Ken Sueda
 * @author Rob Elves
 * @author Jevgeni Holodkov
 */
// API-3.0: rewrite this class
// - move to core
// - separate error handling and backup mechanism from externalization
// - make the externalization stream based instead of file base
// - separate repository externalization and task list externalization
// - provide roll-back when import fails
/**
 * @deprecated
 */
@Deprecated
public class TaskListElementImporter {

	private static final String TRANSFORM_PROPERTY_VERSION = "version";

	// May 2007: There was a bug when reading in 1.1
	// Result was an infinite loop within the parser
	private static final String XML_VERSION = "1.0";

	public static final String ATTRIBUTE_VERSION = "Version";

	public static final String ELEMENT_TASK_LIST = "TaskList";

	private static final String VALUE_VERSION = "1.0.1";

	private static final String VALUE_VERSION_1_0_0 = "1.0.0";

	private static final String FILE_SUFFIX_SAVE = "save.xml";

	private List<AbstractTaskListFactory> externalizers;

	private DelegatingTaskExternalizer delegatingExternalizer;

	private final TaskRepositoriesExternalizer repositoriesExternalizer;

	private final InteractionContextExternalizer contextExternalizer;

	private final List<Node> orphanedTaskNodes = new ArrayList<Node>();

	private final List<Node> orphanedQueryNodes = new ArrayList<Node>();

	private String readVersion = "";

	private boolean hasCaughtException = false;

	private final TaskRepositoryManager repositoryManager;

	private static final String MESSAGE_RESTORE = "Could not read task list.  Consider restoring via File -> Import -> Mylyn Task Data";

	public TaskListElementImporter(TaskRepositoryManager repositoryManager, RepositoryModel repositoryModel) {
		this.repositoryManager = repositoryManager;
		this.delegatingExternalizer = new DelegatingTaskExternalizer(repositoryModel, repositoryManager);
		this.repositoriesExternalizer = new TaskRepositoriesExternalizer();
		this.contextExternalizer = new InteractionContextExternalizer();
	}

	public void setDelegateExternalizers(List<AbstractTaskListFactory> externalizers,
			List<AbstractTaskListMigrator> migrators) {
		this.externalizers = externalizers;
		this.delegatingExternalizer.initialize(externalizers, migrators);
	}

	public void setDelegateExternalizers(List<AbstractTaskListFactory> externalizers) {
		this.externalizers = externalizers;
		this.delegatingExternalizer.initialize(externalizers, new ArrayList<AbstractTaskListMigrator>(0));
	}

	public void writeTaskList(TaskList taskList, File outFile) {
		try {
			FileOutputStream outStream = new FileOutputStream(outFile);
			try {
				writeTaskList(taskList, outStream);
			} finally {
				outStream.close();
			}
		} catch (Exception e) {
			StatusHandler.log(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Task data was not written", e));
		}
	}

	public void writeTaskList(TaskList taskList, OutputStream outputStream) throws IOException {
		Document doc = createTaskListDocument();
		if (doc == null) {
			return;
		}

		Element root = createTaskListRoot(doc);

		// create task nodes...
		for (AbstractTask task : taskList.getAllTasks()) {
			delegatingExternalizer.createTaskElement(task, doc, root);
		}

		// create the categorie nodes...
		for (AbstractTaskCategory category : taskList.getCategories()) {
			delegatingExternalizer.createCategoryElement(category, doc, root);
		}

		// create query nodes...
		for (RepositoryQuery query : taskList.getQueries()) {
			try {
				delegatingExternalizer.createQueryElement(query, doc, root);
			} catch (Throwable t) {
				// FIXME use log?
				StatusHandler.fail(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Did not externalize: "
						+ query.getSummary(), t));
			}
		}

		// Persist orphaned tasks...
		for (Node orphanedTaskNode : orphanedTaskNodes) {
			Node tempNode = doc.importNode(orphanedTaskNode, true);
			if (tempNode != null) {
				root.appendChild(tempNode);
			}
		}

		// Persist orphaned queries....
		for (Node orphanedQueryNode : orphanedQueryNodes) {
			Node tempNode = doc.importNode(orphanedQueryNode, true);
			if (tempNode != null) {
				root.appendChild(tempNode);
			}
		}

		ZipOutputStream zipOutStream = new ZipOutputStream(outputStream);
		writeTaskList(doc, zipOutStream);
		zipOutStream.finish();
	}

	/**
	 * @param doc
	 * @param outputStream
	 * @throws IOException
	 */
	private void writeTaskList(Document doc, ZipOutputStream outputStream) throws IOException {
		ZipEntry zipEntry = new ZipEntry(ITasksCoreConstants.OLD_TASK_LIST_FILE);
		outputStream.putNextEntry(zipEntry);
		outputStream.setMethod(ZipOutputStream.DEFLATED);
		// OutputStream outputStream = new FileOutputStream(file);
		writeDOMtoStream(doc, outputStream);
		outputStream.flush();
		outputStream.closeEntry();
	}

	/**
	 * Writes the provided XML document out to the specified output stream.
	 * 
	 * doc - the document to be written outputStream - the stream to which the document is to be written
	 */
	private void writeDOMtoStream(Document doc, OutputStream outputStream) {
		// Prepare the DOM document for writing
		// DOMSource - Acts as a holder for a transformation Source tree in the
		// form of a Document Object Model (DOM) tree
		Source source = new DOMSource(doc);

		// StreamResult - Acts as an holder for a XML transformation result
		// Prepare the output stream
		Result result = new StreamResult(outputStream);

		// An instance of this class can be obtained with the
		// TransformerFactory.newTransformer method. This instance may
		// then be used to process XML from a variety of sources and write
		// the transformation output to a variety of sinks

		Transformer xformer = null;
		try {
			xformer = TransformerFactory.newInstance().newTransformer();
			xformer.setOutputProperty(TRANSFORM_PROPERTY_VERSION, XML_VERSION);
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
	 * @deprecated
	 */
	@Deprecated
	public void readTaskList(TaskList taskList, File inFile) {
		hasCaughtException = false;
		delegatingExternalizer.getLegacyParentCategoryMap().clear();
		Map<AbstractTask, NodeList> tasksWithSubtasks = new HashMap<AbstractTask, NodeList>();
		orphanedTaskNodes.clear();
		orphanedQueryNodes.clear();
		try {
			if (!inFile.exists()) {
				return;
			}
			Document doc = openAsDOM(inFile, false);
			if (doc == null) {
				handleException(inFile, null, new TaskExternalizationException("TaskList was not well formed XML"));
				return;
			}
			Element root = doc.getDocumentElement();
			readVersion = root.getAttribute(ATTRIBUTE_VERSION);

			if (readVersion.equals(VALUE_VERSION_1_0_0)) {
				// make an error? propagate exception?
				StatusHandler.log(new Status(IStatus.INFO, TasksUiPlugin.ID_PLUGIN, "Task list version \""
						+ readVersion + "\" not supported"));
			} else {
				NodeList list = root.getChildNodes();

				// Read Tasks
				for (int i = 0; i < list.getLength(); i++) {
					Node child = list.item(i);
					try {
						if (!child.getNodeName().endsWith(DelegatingTaskExternalizer.KEY_CATEGORY)
								&& !child.getNodeName().endsWith(AbstractTaskListFactory.KEY_QUERY)) {

							AbstractTask task = delegatingExternalizer.readTask(child, null, null);
							if (task == null) {
								orphanedTaskNodes.add(child);
							} else {
								taskList.addTask(task);
								if (child.getChildNodes() != null && child.getChildNodes().getLength() > 0) {
									tasksWithSubtasks.put(task, child.getChildNodes());
								}
							}
						}
					} catch (Exception e) {
						// TODO: Save orphans here too?
						// If data is source of exception then error will just repeat
						// now that orphans are re-saved upon task list save. So for now we
						// log the error warning the user and make a copy of the bad tasklist.
						handleException(inFile, child, e);
					}
				}

				for (AbstractTask task : tasksWithSubtasks.keySet()) {
					NodeList nodes = tasksWithSubtasks.get(task);
					delegatingExternalizer.readTaskReferences(task, nodes, taskList);
				}

				// Read Queries
				for (int i = 0; i < list.getLength(); i++) {
					Node child = list.item(i);
					try {
						if (child.getNodeName().endsWith(AbstractTaskListFactory.KEY_QUERY)) {
							delegatingExternalizer.readQuery(child);
						}
					} catch (Exception e) {
						handleException(inFile, child, e);
					}
				}

				// Read Categories
				for (int i = 0; i < list.getLength(); i++) {
					Node child = list.item(i);
					try {
						if (child.getNodeName().endsWith(DelegatingTaskExternalizer.KEY_CATEGORY)) {
							delegatingExternalizer.readCategory(child, taskList);
						}
					} catch (Exception e) {
						handleException(inFile, child, e);
					}
				}

				// Legacy migration for task nodes that have the old Category handle on the element
				if (delegatingExternalizer.getLegacyParentCategoryMap().size() > 0) {
					for (AbstractTask task : delegatingExternalizer.getLegacyParentCategoryMap().keySet()) {
						AbstractTaskCategory category = taskList.getContainerForHandle(delegatingExternalizer.getLegacyParentCategoryMap()
								.get(task));
						if (category != null) {
							taskList.addTask(task, category);
						}
					}
				}
			}
		} catch (Exception e) {
			handleException(inFile, null, e);
		}
		if (hasCaughtException) {
			// if exception was caught, write out the new task file, so that it
			// doesn't happen again.
			// this is OK, since the original (corrupt) tasklist is saved.
			writeTaskList(taskList, inFile);
		}
	}

	private Document createTaskListDocument() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document doc = null;

		try {
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
		} catch (ParserConfigurationException e) {
			// FIXME propagate exception?
			StatusHandler.log(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Could not create document", e));
			return doc;
		}

		return doc;
	}

	private Element createTaskListRoot(Document doc) {
		Element root = doc.createElement(ELEMENT_TASK_LIST);
		root.setAttribute(ATTRIBUTE_VERSION, VALUE_VERSION);
		doc.appendChild(root);
		return root;
	}

//	/**
//	 * Reads the Query from the specified Node. If taskList is not null, then also adds this query to the TaskList
//	 * 
//	 * @throws TaskExternalizationException
//	 */
//	private RepositoryQuery readQuery(TaskList taskList, Node child) throws TaskExternalizationException {
//		RepositoryQuery query = null;
//		for (AbstractTaskListFactory externalizer : externalizers) {
//			Set<String> queryTagNames = externalizer.getQueryElementNames();
//			if (queryTagNames != null && queryTagNames.contains(child.getNodeName())) {
//				Element childElement = (Element) child;
//				// TODO: move this stuff into externalizer
//				String repositoryUrl = childElement.getAttribute(DelegatingTaskExternalizer.KEY_REPOSITORY_URL);
//				String queryString = childElement.getAttribute(AbstractTaskListFactory.KEY_QUERY_STRING);
//				if (queryString.length() == 0) { // fallback for legacy
//					queryString = childElement.getAttribute(AbstractTaskListFactory.KEY_QUERY);
//				}
//				String label = childElement.getAttribute(DelegatingTaskExternalizer.KEY_NAME);
//				if (label.length() == 0) { // fallback for legacy
//					label = childElement.getAttribute(DelegatingTaskExternalizer.KEY_LABEL);
//				}
//
//				query = externalizer.createQuery(repositoryUrl, queryString, label, childElement);
//				if (query != null) {
//					if (childElement.getAttribute(DelegatingTaskExternalizer.KEY_LAST_REFRESH) != null
//							&& !childElement.getAttribute(DelegatingTaskExternalizer.KEY_LAST_REFRESH).equals("")) {
//						query.setLastSynchronizedStamp(childElement.getAttribute(DelegatingTaskExternalizer.KEY_LAST_REFRESH));
//					}
//				}
//
//				// add created Query to the TaskList and read QueryHits (Tasks related to the Query)
//				if (taskList != null) {
//					if (query != null) {
//						taskList.addQuery(query);
//					}
//
//					NodeList queryChildren = child.getChildNodes();
////					try {
//					delagatingExternalizer.readTaskReferences(query, queryChildren, taskList);
////					} catch (TaskExternalizationException e) {
////						hasCaughtException = true;
////					}
//				}
//
//				break;
//			}
//		}
//		if (query == null) {
//			orphanedQueryNodes.add(child);
//		}
//
//		return query;
//	}

	/**
	 * Opens the specified XML file and parses it into a DOM Document.
	 * 
	 * Filename - the name of the file to open Return - the Document built from the XML file Throws - XMLException if
	 * the file cannot be parsed as XML - IOException if the file cannot be opened
	 */
	private Document openAsDOM(File inputFile, boolean propagateException) throws IOException {

		// A factory API that enables applications to obtain a parser
		// that produces DOM object trees from XML documents
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		// Using DocumentBuilder, obtain a Document from XML file.
		DocumentBuilder builder = null;
		Document document = null;
		try {
			// create new instance of DocumentBuilder
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			inputFile.renameTo(new File(inputFile.getName() + FILE_SUFFIX_SAVE));
			IOException ioe = new IOException("Failed to load XML file");
			ioe.initCause(pce);
			throw ioe;
		}
		try {
			// Parse the content of the given file as an XML document
			// and return a new DOM Document object. Also throws IOException
			InputStream inputStream = null;
			if (inputFile.getName().endsWith(ITasksCoreConstants.FILE_EXTENSION)) {
				// is zipped context
				inputStream = new ZipInputStream(new FileInputStream(inputFile));
				// search for TaskList entry
				ZipEntry entry = ((ZipInputStream) inputStream).getNextEntry();
				while (entry != null) {
					if (ITasksCoreConstants.OLD_TASK_LIST_FILE.equals(entry.getName())) {
						break;
					}
					entry = ((ZipInputStream) inputStream).getNextEntry();
				}
				if (entry == null) {
					return null;
				}
			} else {
				inputStream = new FileInputStream(inputFile);
			}
			document = builder.parse(inputStream);
			// document = builder.parse(inputFile);
		} catch (SAXException se) {
			// TODO: Use TaskListBackupManager to attempt restore from backup
			if (propagateException) {
				throw new IOException("The task list format is invalid");
			} else {
				MessageDialog.openWarning(null, "Mylyn task list corrupt",
						"Unable to read the Mylyn task list. Please restore from previous backup via File > Import > Mylyn Task Data");
			}
		}
		return document;
	}

	private void handleException(File inFile, Node child, Exception e) {
		hasCaughtException = true;
		String name = inFile.getAbsolutePath();
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("yy-MM-dd-ss");
		name = name.substring(0, name.lastIndexOf('.')) + "-failed-" + sdf.format(date) + ".zip";
		File save = new File(name);
		if (save.exists()) {
			if (!save.delete()) {
				StatusHandler.log(new Status(IStatus.WARNING, TasksUiPlugin.ID_PLUGIN,
						"Unable to delete old backup tasklist file"));
				return;
			}
		}
		if (!copy(inFile, save)) {
			inFile.renameTo(new File(name));
		}
		if (child == null) {
			StatusHandler.log(new Status(IStatus.WARNING, TasksUiPlugin.ID_PLUGIN, MESSAGE_RESTORE, e));
		} else {
			StatusHandler.log(new Status(IStatus.WARNING, TasksUiPlugin.ID_PLUGIN, "Tasks may have been lost from "
					+ child.getNodeName(), e));
		}
	}

	private boolean copy(File src, File dst) {
		try {
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dst);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			return true;
		} catch (IOException ioe) {
			return false;
		}
	}

	public void setDelegatingExternalizer(DelegatingTaskExternalizer delagatingExternalizer) {
		this.delegatingExternalizer = delagatingExternalizer;
	}

	public List<AbstractTaskListFactory> getExternalizers() {
		return externalizers;
	}

	public void writeQueries(List<RepositoryQuery> queries, File outFile) {
		Set<TaskRepository> repositories = new HashSet<TaskRepository>();
		for (IRepositoryQuery query : queries) {
			TaskRepository repository = TasksUi.getRepositoryManager().getRepository(query.getConnectorKind(),
					query.getRepositoryUrl());
			if (repository != null) {
				repositories.add(repository);
			}
		}

		Document doc = createQueryDocument(queries);
		if (doc != null) {
			try {
				ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(outFile));
				writeTaskList(doc, outputStream);
				repositoriesExternalizer.writeRepositories(repositories, outputStream);
				outputStream.close();
			} catch (Exception e) {
				StatusHandler.log(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Task list could not be found", e));
			}
		}
		return;
	}

	/**
	 * @return null if it was not possible to create the Query document.
	 */
	public Document createQueryDocument(List<RepositoryQuery> queries) {
		Document doc = createTaskListDocument();
		if (doc == null) {
			return null;
		}

		Element root = createTaskListRoot(doc);
		for (RepositoryQuery query : queries) {
			try {
				delegatingExternalizer.createQueryElement(query, doc, root);
			} catch (Throwable t) {
				StatusHandler.fail(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Did not externalize: "
						+ query.getSummary(), t));
				return null;
			}
		}
		return doc;
	}

	public List<RepositoryQuery> readQueries(File inFile) throws IOException {
		if (!inFile.exists()) {
			throw new FileNotFoundException("File does not exist: " + inFile);
		}

		Document doc = openAsDOM(inFile, true);
		if (doc == null) {
			throw new IOException("TaskList was not well formed XML");
		}
		return readQueryDocument(doc);
	}

	/**
	 * @param Query
	 *            document to read.
	 */
	public List<RepositoryQuery> readQueryDocument(Document doc) {
		List<RepositoryQuery> queries = new ArrayList<RepositoryQuery>();
		Element root = doc.getDocumentElement();
		readVersion = root.getAttribute(ATTRIBUTE_VERSION);

		if (!readVersion.equals(VALUE_VERSION_1_0_0)) {
			NodeList list = root.getChildNodes();

			// read queries
			for (int i = 0; i < list.getLength(); i++) {
				Node child = list.item(i);
				try {
					if (child.getNodeName().endsWith(AbstractTaskListFactory.KEY_QUERY)) {
						RepositoryQuery query = delegatingExternalizer.readQuery(child);
						if (query != null) {
							queries.add(query);
						}
					}
				} catch (Exception e) {
					StatusHandler.log(new Status(IStatus.WARNING, TasksUiPlugin.ID_PLUGIN,
							"Tasks may have been lost from " + child.getNodeName(), e));
				}
			}
		} else {
			// FIXME propagate error?
			StatusHandler.log(new Status(IStatus.WARNING, TasksUiPlugin.ID_PLUGIN, "Version \"" + readVersion
					+ "\" not supported"));
		}

		return queries;
	}

	public Set<TaskRepository> readRepositories(File file) {
		Set<TaskRepository> repository = repositoriesExternalizer.readRepositoriesFromXML(file);
		if (repository == null) {
			repository = new HashSet<TaskRepository>();
		}
		return repository;
	}

	public void writeTask(AbstractTask task, File outFile) {
		try {
			writeTask(task, new FileOutputStream(outFile));
		} catch (Exception e) {
			StatusHandler.log(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Task data was not written", e));
		}
	}

	public void writeTask(AbstractTask task, OutputStream stream) {
		Set<TaskRepository> repositories = new HashSet<TaskRepository>();
		if (!task.isLocal()) {
			repositories.add(repositoryManager.getRepository(task.getConnectorKind(), task.getRepositoryUrl()));
		}

		Document doc = createTaskListDocument();
		if (doc == null) {
			return;
		}

		Element root = createTaskListRoot(doc);

		delegatingExternalizer.createTaskElement(task, doc, root);
		try {
			ZipOutputStream outputStream = new ZipOutputStream(stream);
			// write task data 
			writeTaskList(doc, outputStream);

			// write context data
			ContextCorePlugin.getContextStore().export(task.getHandleIdentifier(), outputStream);

			if (repositories.size() > 0) {
				repositoriesExternalizer.writeRepositories(repositories, outputStream);
			}

			outputStream.close();
		} catch (Exception e) {
			StatusHandler.log(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN, "Task data was not written", e));
		}
	}

	public List<AbstractTask> readTasks(File inFile) {
		List<AbstractTask> tasks = new ArrayList<AbstractTask>();
		try {
			if (!inFile.exists()) {
				return tasks;
			}
			Document doc = openAsDOM(inFile, false);
			if (doc == null) {
				handleException(inFile, null, new TaskExternalizationException("TaskList was not well formed XML"));
				return tasks;
			}

			// read task document
			Element root = doc.getDocumentElement();
			readVersion = root.getAttribute(ATTRIBUTE_VERSION);

			if (!readVersion.equals(VALUE_VERSION_1_0_0)) {
				NodeList list = root.getChildNodes();

				// read tasks
				for (int i = 0; i < list.getLength(); i++) {
					Node child = list.item(i);
					try {
						if (!child.getNodeName().endsWith(DelegatingTaskExternalizer.KEY_CATEGORY)
								&& !child.getNodeName().endsWith(AbstractTaskListFactory.KEY_QUERY)) {
							AbstractTask task = delegatingExternalizer.readTask(child, null, null);
							if (task != null) {
								tasks.add(task);
							}
						}
					} catch (Exception e) {
						StatusHandler.log(new Status(IStatus.WARNING, TasksUiPlugin.ID_PLUGIN,
								"Tasks may have been lost from " + child.getNodeName(), e));
					}
				}
			} else {
				// FIXME propagate error?
				StatusHandler.log(new Status(IStatus.WARNING, TasksUiPlugin.ID_PLUGIN, "Version \"" + readVersion
						+ "\" not supported"));
			}
		} catch (Exception e) {
			handleException(inFile, null, e);
		}

		return tasks;
	}

}
