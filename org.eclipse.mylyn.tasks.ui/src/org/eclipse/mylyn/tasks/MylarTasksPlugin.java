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
package org.eclipse.mylar.tasks;

import java.io.File;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.tasks.bugzilla.BugzillaContentProvider;
import org.eclipse.mylar.tasks.bugzilla.BugzillaEditingMonitor;
import org.eclipse.mylar.tasks.bugzilla.BugzillaMylarBridge;
import org.eclipse.mylar.tasks.bugzilla.BugzillaReferencesProvider;
import org.eclipse.mylar.tasks.bugzilla.BugzillaStructureBridge;
import org.eclipse.mylar.tasks.bugzilla.ui.BugzillaUiBridge;
import org.eclipse.mylar.ui.MylarUiPlugin;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Mik Kersten
 */
public class MylarTasksPlugin extends AbstractUIPlugin implements IStartup {
    
    private static MylarTasksPlugin plugin;
    private static TaskListManager taskListManager;
    private BugzillaContentProvider bugzillaProvider;
    
    public static final String REFRESH_QUERIES = "org.eclipse.mylar.tasks.queries.refresh";
    public static final String REPORT_OPEN_EDITOR = "org.eclipse.mylar.tasks.report.open.editor";
    public static final String REPORT_OPEN_INTERNAL = "org.eclipse.mylar.tasks.report.open.internal";
    public static final String REPORT_OPEN_EXTERNAL = "org.eclipse.mylar.tasks.report.open.external";
    
    public static final String FILE_EXTENSION = ".xml";
    public static final String TASK_ID = "org.eclipse.mylar.tasks.userid";
    public static final String DEFAULT_TASK_LIST_FILE = "tasklist" + FILE_EXTENSION;
    public static final String TASK_EDITOR_ID = "org.eclipse.mylar.tasks.ui.taskEditor";
    public static final String SELECTED_PRIORITY = "org.eclipse.mylar.tasks.filter.priority";
    public static final String FILTER_COMPLETE_MODE = "org.eclipse.mylar.tasks.filter.complete";
    public static final String FILTER_INCOMPLETE_MODE = "org.eclipse.mylar.tasks.filter.incomplete";
    
	private ResourceBundle resourceBundle;

	public enum Report_Open_Mode {
		EDITOR,
		INTERNAL_BROWSER,
		EXTERNAL_BROWSER;
	}
	public enum PriorityLevel {
        P1,
        P2,
        P3,
        P4,
        P5;               
        
        @Override
        public String toString() {
            switch(this) {
                case P1: return "P1";
                case P2: return "P2";
                case P3: return "P3";
                case P4: return "P4";
                case P5: return "P5";
                default: return "P5";
            }
        }
        public static PriorityLevel fromString(String string) {
            if (string == null) return null;
            if (string.equals("P1")) return P1;
            if (string.equals("P2")) return P2;
            if (string.equals("P3")) return P3;
            if (string.equals("P4")) return P4;
            if (string.equals("P5")) return P5;
            return null;
        }
    }
	
    /** The bridge between Bugzilla and mylar */
    private static BugzillaMylarBridge bridge = null;

    private BugzillaStructureBridge structureBridge;
    
    private static BugzillaReferencesProvider referencesProvider = new BugzillaReferencesProvider();
    
    private static ITaskActivityListener TASK_LIST_LISTENER = new ITaskActivityListener() {

        public void taskActivated(ITask task) {
            MylarPlugin.getTaskscapeManager().taskActivated(task.getHandle(), task.getPath());
        }

        public void tasksActivated(List<ITask> tasks) {
            for (ITask task : tasks) {
                MylarPlugin.getTaskscapeManager().taskActivated(task.getHandle(), task.getPath());
            }
        }

        public void taskDeactivated(ITask task) {
            MylarPlugin.getTaskscapeManager().taskDeactivated(task.getHandle(), task.getPath());
        }

		public void taskPropertyChanged(ITask updatedTask, String property) {
			// don't care about property change	
		}
        
    };
    
    private static ShellListener SHELL_LISTENER = new ShellListener() {
        private void saveState() {
            taskListManager.saveTaskList();
            for(ITask task : taskListManager.getTaskList().getActiveTasks()) {
                MylarPlugin.getTaskscapeManager().saveTaskscape(task.getHandle(), task.getPath());
            }
        }
        
        public void shellClosed(ShellEvent arg0) {
            saveState();
        }  
        
        public void shellDeactivated(ShellEvent arg0) { 
        	// bug 1002249: too slow to save state here
        }
        public void shellActivated(ShellEvent arg0) { }
        
        public void shellDeiconified(ShellEvent arg0) { }
        
        public void shellIconified(ShellEvent arg0) { 
        	saveState();
        }
    };
    
    private static IPropertyChangeListener PREFERENCE_LISTENER = new IPropertyChangeListener() {

		public void propertyChange(PropertyChangeEvent event) {
			// TODO Auto-generated method stub
			if (event.getProperty().equals(MylarPlugin.MYLAR_DIR)) {				
				if (event.getOldValue() instanceof String) {
					String prevDir = (String) event.getOldValue();				
					MylarPlugin.getTaskscapeManager().updateMylarDirContents(prevDir);
					getTaskListManager().updateTaskscapeReference(prevDir);
					
					String path = MylarPlugin.getDefault().getUserDataDirectory() + File.separator + DEFAULT_TASK_LIST_FILE;        
					getTaskListManager().setFile(new File(path));
				}
			} else {
			}
		}        
    };
    
	public MylarTasksPlugin() {
		super();
		plugin = this;
		initializeDefaultPreferences(getPrefs());
	}

    public void earlyStartup() {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.getDisplay().asyncExec(new Runnable() {
            public void run() {
                
            	structureBridge = new BugzillaStructureBridge();
            	
                MylarPlugin.getDefault().addBridge(structureBridge);
                MylarPlugin.getTaskscapeManager().addListener(referencesProvider);
                MylarUiPlugin.getDefault().addAdapter(BugzillaStructureBridge.EXTENSION, new BugzillaUiBridge());
                MylarPlugin.getDefault().getSelectionMonitors().add(new BugzillaEditingMonitor());             
                
                IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                
                Workbench.getInstance().getActiveWorkbenchWindow().getShell().addShellListener(SHELL_LISTENER);
                MylarPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(PREFERENCE_LISTENER);
                                
                if (window != null) {
                    // create a new bridge and initialize it
                    bridge = new BugzillaMylarBridge();
                }
            }
        });
    }
    
    @Override
	public void start(BundleContext context) throws Exception {
        bugzillaProvider = new BugzillaContentProvider();
        String path = MylarPlugin.getDefault().getUserDataDirectory() + File.separator + DEFAULT_TASK_LIST_FILE;        
        File taskListFile = new File(path);
        taskListManager = new TaskListManager(taskListFile);
        taskListManager.addListener(TASK_LIST_LISTENER);
        taskListManager.readTaskList();
        if (taskListManager.getTaskList() == null) taskListManager.createNewTaskList();
        
		super.start(context);
	}

    @Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
		resourceBundle = null;
	}

    @Override
    protected void initializeDefaultPreferences(IPreferenceStore store) {
//    	store.setDefault(SHOW_P1_MODE, true);
//    	store.setDefault(SHOW_P2_MODE, true);
//    	store.setDefault(SHOW_P3_MODE, true);
//    	store.setDefault(SHOW_P4_MODE, true);
//    	store.setDefault(SHOW_P5_MODE, true);
    	store.setDefault(SELECTED_PRIORITY, "P5");
    	store.setDefault(MylarPlugin.CLOSE_EDITORS, true);
    	store.setDefault(REFRESH_QUERIES, false);
    	store.setDefault(REPORT_OPEN_EDITOR, true);
    	store.setDefault(REPORT_OPEN_INTERNAL, false);
    	store.setDefault(REPORT_OPEN_EXTERNAL, false);
    }
    /**
     * Get the bridge for this plugin
     * 
     * @return The bugzilla mylar bridge
     */
    public static BugzillaMylarBridge getBridge() {
        // make sure that the bridge initialized, if not, make a new one
        if (bridge == null) {
            bridge = new BugzillaMylarBridge();
//            MylarPlugin.getTaskscapeManager().addRelationshipProvider(new BugzillaRelationshipProvider());
//            MylarUiPlugin.getDefault().getAdapters().put(ITaskscapeNode.Kind.Bugzilla, new BugzillaUiAdapter());
        }
        return bridge;
    }
    
    
    public static TaskListManager getTaskListManager() {
        return taskListManager;
    }
    
	/**
	 * Returns the shared instance.
	 */
	public static MylarTasksPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = MylarTasksPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		try {
			if (resourceBundle == null)
				resourceBundle = ResourceBundle.getBundle("taskListPlugin.TaskListPluginPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}
    
    public BugzillaContentProvider getBugzillaProvider() {
        return bugzillaProvider;
    }
    
    public void setBugzillaProvider(BugzillaContentProvider bugzillaProvider) {
        this.bugzillaProvider = bugzillaProvider;
    }
    
    public BugzillaStructureBridge getStructureBridge() {
        return structureBridge;
    }

	public static BugzillaReferencesProvider getReferenceProvider() {
		return referencesProvider;
		
	}
	
	public static IPreferenceStore getPrefs() {
		return MylarPlugin.getDefault().getPreferenceStore();
	}
	
	public static void setPriorityLevel(PriorityLevel pl) {
		getPrefs().setValue(SELECTED_PRIORITY, pl.toString());
	}
	public static String getPriorityLevel() {
		if(getPrefs().contains(SELECTED_PRIORITY)) {
			return getPrefs().getString(SELECTED_PRIORITY);
		} else {			
			return PriorityLevel.P5.toString();
		}		
	}
	
	public void setFilterCompleteMode(boolean isFilterOn) {
		getPrefs().setValue(FILTER_COMPLETE_MODE, isFilterOn);
	}
	
	public boolean isFilterCompleteMode() {
		if (getPrefs().contains(FILTER_COMPLETE_MODE)) {
			return getPrefs().getBoolean(FILTER_COMPLETE_MODE);
		} else {
			return false;
		}
	}
	
	public void setFilterInCompleteMode(boolean isFilterOn) {
		getPrefs().setValue(FILTER_INCOMPLETE_MODE, isFilterOn);
	}
	
	public boolean isFilterInCompleteMode() {
		if (getPrefs().contains(FILTER_INCOMPLETE_MODE)) {
			return getPrefs().getBoolean(FILTER_INCOMPLETE_MODE);
		} else {
			return false;
		}
	}

	public boolean refreshOnStartUpEnabled() {
		return getPrefs().getBoolean(REFRESH_QUERIES);
	}
	
	public Report_Open_Mode getReportMode() {
		if (getPrefs().getBoolean(REPORT_OPEN_EDITOR)) {
			return Report_Open_Mode.EDITOR;
		} else if (getPrefs().getBoolean(REPORT_OPEN_INTERNAL)) {
			return Report_Open_Mode.INTERNAL_BROWSER;
		} else {
			return Report_Open_Mode.EXTERNAL_BROWSER;
		} 
	}
}
