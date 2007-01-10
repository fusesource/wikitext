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

package org.eclipse.mylar.bugzilla.tests;

import junit.framework.TestCase;

import org.eclipse.mylar.internal.bugzilla.core.BugzillaAttributeFactory;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.internal.bugzilla.ui.editor.NewBugzillaTaskEditor;
import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.TasksUiUtil;
import org.eclipse.mylar.tasks.ui.editors.NewTaskEditorInput;
import org.eclipse.mylar.tasks.ui.editors.TaskEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * @author Jeff Pound
 */
public class DuplicateDetetionTest extends TestCase {

	private TaskRepository repository;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		repository = new TaskRepository(BugzillaCorePlugin.REPOSITORY_KIND, IBugzillaConstants.TEST_BUGZILLA_222_URL);

	}

	public void testDuplicateDetection() throws Exception {

		String stackTrace = "java.lang.NullPointerException\nat jeff.testing.stack.trace.functionality(jeff.java:481)";

		RepositoryTaskData model = new RepositoryTaskData(new BugzillaAttributeFactory(), BugzillaCorePlugin.REPOSITORY_KIND, repository.getUrl(), TasksUiPlugin.getDefault()
				.getTaskDataManager().getNewRepositoryTaskId());
		model.setDescription(stackTrace);
		model.setHasLocalChanges(true);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		NewTaskEditorInput input = new NewTaskEditorInput(repository, model);
		TasksUiUtil.openEditor(input, TaskListPreferenceConstants.TASK_EDITOR_ID, page);

		TaskEditor taskEditor = (TaskEditor) page.getActiveEditor();
		NewBugzillaTaskEditor editor = (NewBugzillaTaskEditor) taskEditor.getActivePageInstance();
		assertTrue(editor.searchForDuplicates());

		editor.markDirty(false);
		editor.close();
	}

	public void testNoStackTrace() throws Exception {
		String fakeStackTrace = "this is not really a stacktrace";
		RepositoryTaskData model = new RepositoryTaskData(new BugzillaAttributeFactory(), BugzillaCorePlugin.REPOSITORY_KIND, repository.getUrl(), TasksUiPlugin.getDefault()
				.getTaskDataManager().getNewRepositoryTaskId());
		model.setDescription(fakeStackTrace);
		model.setHasLocalChanges(true);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		NewTaskEditorInput input = new NewTaskEditorInput(repository, model);
		TasksUiUtil.openEditor(input, TaskListPreferenceConstants.TASK_EDITOR_ID, page);

		TaskEditor taskEditor = (TaskEditor) page.getActiveEditor();
		NewBugzillaTaskEditor editor = (NewBugzillaTaskEditor) taskEditor.getActivePageInstance();
		assertNull(editor.getStackTraceFromDescription());

		editor.markDirty(false);
		editor.close();
	}

	public void testStackTraceWithAppendedText() throws Exception {

		String stackTrace = "java.lang.NullPointerException\nat jeff.testing.stack.trace.functionality(jeff.java:481)";
		String extraText = "\nExtra text that isnt' part of the stack trace java:";

		RepositoryTaskData model = new RepositoryTaskData(new BugzillaAttributeFactory(), BugzillaCorePlugin.REPOSITORY_KIND, repository.getUrl(), TasksUiPlugin.getDefault()
				.getTaskDataManager().getNewRepositoryTaskId());

		model.setDescription(extraText + "\n" + stackTrace + "\n");
		model.setHasLocalChanges(true);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		NewTaskEditorInput input = new NewTaskEditorInput(repository, model);
		TasksUiUtil.openEditor(input, TaskListPreferenceConstants.TASK_EDITOR_ID, page);

		TaskEditor taskEditor = (TaskEditor) page.getActiveEditor();
		NewBugzillaTaskEditor editor = (NewBugzillaTaskEditor) taskEditor.getActivePageInstance();
		assertEquals(stackTrace, editor.getStackTraceFromDescription().trim());

		editor.markDirty(false);
		editor.close();
	}
	
	public void testStackTraceMisaligned() throws Exception {

		String stackTrace = "java.lang.IllegalStateException: zip file closed\n" +
		"     at java.util.zip.ZipFile.ensureOpen (ZipFile.java:518)\n" +
		"at java.util.zip.ZipFile.getEntry (ZipFile.java:251)\n" +
		"   at java.util.jar.JarFile.getEntry(JarFile.java:200)\n" +
		"at sun.net.www.protocol.jar.URLJarFile.getEntry\n" +
		"     (URLJarFile.java:90)\n" +
		"at sun.net.www.protocol.jar.JarURLConnection.connect(JarURLConnection.java:112)\n" +
		"at sun.net.www.protocol.jar.JarURLConnection.getInputStream\n" +
		"(JarURLConnection.java:124)\n" +
		"at org.eclipse.jdt.internal.core.JavaElement\n.getURLContents(JavaElement.java:734)";

		RepositoryTaskData model = new RepositoryTaskData(new BugzillaAttributeFactory(), BugzillaCorePlugin.REPOSITORY_KIND, repository.getUrl(), TasksUiPlugin.getDefault()
				.getTaskDataManager().getNewRepositoryTaskId());

		model.setDescription(stackTrace);
		model.setHasLocalChanges(true);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		NewTaskEditorInput input = new NewTaskEditorInput(repository, model);
		TasksUiUtil.openEditor(input, TaskListPreferenceConstants.TASK_EDITOR_ID, page);

		TaskEditor taskEditor = (TaskEditor) page.getActiveEditor();
		NewBugzillaTaskEditor editor = (NewBugzillaTaskEditor) taskEditor.getActivePageInstance();
		assertEquals(stackTrace, editor.getStackTraceFromDescription().trim());

		editor.markDirty(false);
		editor.close();
	}
	
	public void testStackTraceSUN() throws Exception {

		// SUN, IBM (no space before brackets, one set of brackets)
		String stackTrace = "java.lang.IllegalStateException: zip file closed\n" +
		"     at java.util.zip.ZipFile.ensureOpen(ZipFile.java:518)\n" +
		"     at java.util.zip.ZipFile.getEntry(ZipFile.java:251)\n" +
		"     at java.util.jar.JarFile.getEntry(JarFile.java:200)\n" +
		"     at sun.net.www.protocol.jar.URLJarFile.getEntry(URLJarFile.java:90)\n" +
		"     at sun.net.www.protocol.jar.JarURLConnection.connect(JarURLConnection.java:112)\n" +
		"     at sun.net.www.protocol.jar.JarURLConnection.getInputStream(JarURLConnection.java:124)\n" +
		"     at org.eclipse.jdt.internal.core.JavaElement.getURLContents(JavaElement.java:734)";

		RepositoryTaskData model = new RepositoryTaskData(new BugzillaAttributeFactory(), BugzillaCorePlugin.REPOSITORY_KIND, repository.getUrl(), TasksUiPlugin.getDefault()
				.getTaskDataManager().getNewRepositoryTaskId());

		model.setDescription(stackTrace);
		model.setHasLocalChanges(true);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		NewTaskEditorInput input = new NewTaskEditorInput(repository, model);
		TasksUiUtil.openEditor(input, TaskListPreferenceConstants.TASK_EDITOR_ID, page);

		TaskEditor taskEditor = (TaskEditor) page.getActiveEditor();
		NewBugzillaTaskEditor editor = (NewBugzillaTaskEditor) taskEditor.getActivePageInstance();
		assertEquals(stackTrace, editor.getStackTraceFromDescription().trim());

		editor.markDirty(false);
		editor.close();
	}
	
	public void testStackTraceGCJ() throws Exception {

		// gcj/gij (path and lib names in additional brackets)
		String stackTrace = "java.lang.Error: Something bad happened\n" +
		"	   at testcase.main(java.lang.String[]) (Unknown Source)\n" +
		"	   at gnu.java.lang.MainThread.call_main() (/usr/lib/libgcj.so.6.0.0)\n" +
		"	   at gnu.java.lang.MainThread.run() (/usr/lib/libgcj.so.6.0.0)";
		
		RepositoryTaskData model = new RepositoryTaskData(new BugzillaAttributeFactory(), BugzillaCorePlugin.REPOSITORY_KIND, repository.getUrl(), TasksUiPlugin.getDefault()
				.getTaskDataManager().getNewRepositoryTaskId());

		model.setDescription(stackTrace);
		model.setHasLocalChanges(true);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		NewTaskEditorInput input = new NewTaskEditorInput(repository, model);
		TasksUiUtil.openEditor(input, TaskListPreferenceConstants.TASK_EDITOR_ID, page);

		TaskEditor taskEditor = (TaskEditor) page.getActiveEditor();
		NewBugzillaTaskEditor editor = (NewBugzillaTaskEditor) taskEditor.getActivePageInstance();
		assertEquals(stackTrace, editor.getStackTraceFromDescription().trim());
		
		editor.markDirty(false);
		editor.close();
	}
	
	public void testStackTraceNoLineNums() throws Exception {

		// ikvm (no line numbers)
		String stackTrace = "java.lang.Error: Something bad happened\n" +
		"	at testcase.main (testcase.java)\n" +
		"	at java.lang.reflect.Method.Invoke (Method.java)";
		
		RepositoryTaskData model = new RepositoryTaskData(new BugzillaAttributeFactory(), BugzillaCorePlugin.REPOSITORY_KIND, repository.getUrl(), TasksUiPlugin.getDefault()
				.getTaskDataManager().getNewRepositoryTaskId());

		model.setDescription(stackTrace);
		model.setHasLocalChanges(true);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		NewTaskEditorInput input = new NewTaskEditorInput(repository, model);
		TasksUiUtil.openEditor(input, TaskListPreferenceConstants.TASK_EDITOR_ID, page);

		TaskEditor taskEditor = (TaskEditor) page.getActiveEditor();
		NewBugzillaTaskEditor editor = (NewBugzillaTaskEditor) taskEditor.getActivePageInstance();
		assertEquals(stackTrace, editor.getStackTraceFromDescription().trim());
		
		editor.markDirty(false);
		editor.close();
	}
	
	public void testStackTraceJRockit() throws Exception {

		// jrockit (slash delimiters)
		String stackTrace = "java.lang.Error: Something bad happened\n" +
		"	at java/io/BufferedReader.readLine(BufferedReader.java:331)\n" +
		"	at java/io/BufferedReader.readLine(BufferedReader.java:362)\n" +
		"	at java/util/Properties.load(Properties.java:192)\n" +
		"	at java/util/logging/LogManager.readConfiguration(L:555)";
	    
		RepositoryTaskData model = new RepositoryTaskData(new BugzillaAttributeFactory(), BugzillaCorePlugin.REPOSITORY_KIND, repository.getUrl(), TasksUiPlugin.getDefault()
				.getTaskDataManager().getNewRepositoryTaskId());

		model.setDescription(stackTrace);
		model.setHasLocalChanges(true);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		NewTaskEditorInput input = new NewTaskEditorInput(repository, model);
		TasksUiUtil.openEditor(input, TaskListPreferenceConstants.TASK_EDITOR_ID, page);

		TaskEditor taskEditor = (TaskEditor) page.getActiveEditor();
		NewBugzillaTaskEditor editor = (NewBugzillaTaskEditor) taskEditor.getActivePageInstance();
		assertEquals(stackTrace, editor.getStackTraceFromDescription().trim());
		
		editor.markDirty(false);
		editor.close();
	}
	
	public void testStackTraceOther() throws Exception {

		// jamvm, sablevm, kaffe, cacao (space before brackets, one set of brackets)
		String stackTrace = "java.lang.Error: Something bad happened\n" +
		"	   at testcase.main (testcase.java:3)\n" +
		"	   at java.lang.VirtualMachine.invokeMain (VirtualMachine.java)\n" +
		"	   at java.lang.VirtualMachine.main (VirtualMachine.java:108)";
		
		RepositoryTaskData model = new RepositoryTaskData(new BugzillaAttributeFactory(), BugzillaCorePlugin.REPOSITORY_KIND, repository.getUrl(), TasksUiPlugin.getDefault()
				.getTaskDataManager().getNewRepositoryTaskId());

		model.setDescription(stackTrace);
		model.setHasLocalChanges(true);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		NewTaskEditorInput input = new NewTaskEditorInput(repository, model);
		TasksUiUtil.openEditor(input, TaskListPreferenceConstants.TASK_EDITOR_ID, page);

		TaskEditor taskEditor = (TaskEditor) page.getActiveEditor();
		NewBugzillaTaskEditor editor = (NewBugzillaTaskEditor) taskEditor.getActivePageInstance();
		assertEquals(stackTrace, editor.getStackTraceFromDescription().trim());
		
		editor.markDirty(false);
		editor.close();
	}
}
