package org.eclipse.mylar.tasklist.ui.wizards;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.tasklist.MylarTasklistPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * Wizard Page for the Task Data Export Wizard
 * 
 * @author Wesley Coelho
 */
public class TaskDataExportWizardPage extends WizardPage {

	protected final static String PAGE_TITLE = "Export Mylar Task Data";
	public final static String PAGE_NAME = PAGE_TITLE;
	public final static String ZIP_FILE_NAME = "MylarTaskData.zip";
	
	//Control fields
	private Button taskListCheckBox = null;
	private Button taskActivationHistoryCheckBox = null;
	private Button taskContextsCheckBox = null;
	private Button zipCheckBox = null;
	private Button browseButton = null;
	private Text destDirText = null; 
	private Button overwriteCheckBox = null;
	
	//Key values for the dialog settings object
	private final static String SETTINGS_SAVED = "Settings saved";
	private final static String TASKLIST_SETTING = "Tasklist setting";
	private final static String ACTIVATION_HISTORY_SETTING = "Activation history setting";
	private final static String CONTEXTS_SETTING = "Contexts setting";
	private final static String DEST_DIR_SETTING = "Destination directory setting";
	private final static String OVERWRITE_SETTING = "Overwrite setting";
	private final static String ZIP_SETTING = "Zip Setting";

	public TaskDataExportWizardPage(){
		super("org.eclipse.mylar.tasklist.exportPage", PAGE_TITLE, 
				MylarTasklistPlugin.imageDescriptorFromPlugin(MylarTasklistPlugin.PLUGIN_ID, "icons/wizban/banner-export.gif"));
		setPageComplete(false);
	}
	
	public String getName(){
		return PAGE_NAME;
	}
	
	/**
	 * Create the widgets on the page
	 */
	public void createControl(Composite parent) {
		try {
			Composite container = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout(1, false);
			container.setLayout(layout);
			
			createFileSelectionControl(container);
			createExportDirectoryControl(container);
			
			zipCheckBox = createCheckBox(container, "Export to zip file: " + ZIP_FILE_NAME);
			overwriteCheckBox = createCheckBox(container, "Overwrite existing files without warning");
			
			initSettings();
			
			setControl(container);
			
			setPageComplete(validate());
		} catch (RuntimeException e) {
			MylarPlugin.fail(e, "Could not create export wizard page", true);
		} 
	}

	/**
	 * Create widgets for selecting the data files to export
	 */
	private void createFileSelectionControl(Composite parent){
		Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);	
		GridLayout gl = new GridLayout(1, false);
		group.setLayout(gl);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gridData);
		group.setText("Select data to export:");
		
		taskListCheckBox = createCheckBox(group, "Task List");
		taskActivationHistoryCheckBox = createCheckBox(group, "Task Activation History");
		taskContextsCheckBox = createCheckBox(group, "Task Contexts");
	}
	
	/**
	 * Create widgets for specifying the destination directory
	 */
	private void createExportDirectoryControl(Composite parent) {
		Group destDirGroup= new Group(parent, SWT.SHADOW_ETCHED_IN);
		destDirGroup.setText("Export destination folder");
		destDirGroup.setLayout(new GridLayout(2, false));
		destDirGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		destDirText = new Text(destDirGroup, SWT.BORDER);		
		destDirText.setEditable(false);
		destDirText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		destDirText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				controlChanged();
			}
		});
		
		
		browseButton = new Button(destDirGroup, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText("Folder Selection");
				dialog.setMessage("Specify the destination folder for task data");
				String dir = destDirText.getText();
				dialog.setFilterPath(dir);
				dir = dialog.open();
				if(dir == null || dir.equals(""))
					return;
				destDirText.setText(dir);
			}
		});      
	}	
	
	
	/**
	 * Initializes controls with values from the Dialog Settings object
	 */
	protected void initSettings(){
		IDialogSettings settings = getDialogSettings();
		
		if (settings.get(SETTINGS_SAVED) == null){
			//Set default values
			taskListCheckBox.setSelection(true);
			taskActivationHistoryCheckBox.setSelection(true);
			taskContextsCheckBox.setSelection(true);
			destDirText.setText("");
			overwriteCheckBox.setSelection(true);
			zipCheckBox.setSelection(false);
		}
		else{
			//Retrieve previous values from the dialog settings
			taskListCheckBox.setSelection(settings.getBoolean(TASKLIST_SETTING));
			taskActivationHistoryCheckBox.setSelection(settings.getBoolean(ACTIVATION_HISTORY_SETTING));
			taskContextsCheckBox.setSelection(settings.getBoolean(CONTEXTS_SETTING));
			String directory = settings.get(DEST_DIR_SETTING);
			if(directory != null){
				destDirText.setText(settings.get(DEST_DIR_SETTING));
			}	
			overwriteCheckBox.setSelection(settings.getBoolean(OVERWRITE_SETTING));
			zipCheckBox.setSelection(settings.getBoolean(ZIP_SETTING));
		}
	}
	
	/**
	 * Saves the control values in the dialog settings to be used as
	 * defaults the next time the page is opened
	 */
	public void saveSettings(){
		IDialogSettings settings = getDialogSettings();
		
		settings.put(TASKLIST_SETTING, taskListCheckBox.getSelection());
		settings.put(ACTIVATION_HISTORY_SETTING, taskActivationHistoryCheckBox.getSelection());
		settings.put(CONTEXTS_SETTING, taskContextsCheckBox.getSelection());
		settings.put(DEST_DIR_SETTING, destDirText.getText());
		settings.put(OVERWRITE_SETTING, overwriteCheckBox.getSelection());
		settings.put(ZIP_SETTING, zipCheckBox.getSelection());

		settings.put(SETTINGS_SAVED, SETTINGS_SAVED);
	}
	
	/** Convenience method for creating a new checkbox */
	protected Button createCheckBox(Composite parent, String text){
		Button newButton= new Button(parent, SWT.CHECK);
		newButton.setText(text);
		
		newButton.addSelectionListener(new SelectionListener(){

			public void widgetSelected(SelectionEvent e) {
				controlChanged();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				//No action required
			}
		});
		
		return newButton;
	}
	
	/** Called to indicate that a control's value has changed */
	public void controlChanged(){
		setPageComplete(validate());
	}

	/** Returns true if the information entered by the user is valid */
	protected boolean validate(){
		
		//Check that at least one type of data has been selected
		if (!taskListCheckBox.getSelection() && 
				!taskActivationHistoryCheckBox.getSelection() && 
				!taskContextsCheckBox.getSelection()){
			return false;
		}
		
		//Check that a destination dir has been specified
		if (destDirText.getText().equals("")){
			return false;
		}
		
		return true;
	}
	
	/** Returns the directory where data files are to be saved */
	public String getDestinationDirectory(){
		return destDirText.getText();
	}
	
	/** True if the user wants to export the task list */
	public boolean exportTaskList(){
		return taskListCheckBox.getSelection();
	}
	
	/** True if the user wants to export task activation history */
	public boolean exportActivationHistory(){
		return taskActivationHistoryCheckBox.getSelection();
	}
	
	/** True if the user wants to export task context files */
	public boolean exportTaskContexts(){
		return taskContextsCheckBox.getSelection();
	}
	
	/** True if the user wants to overwrite files by default*/
	public boolean overwrite(){
		return overwriteCheckBox.getSelection();
	}
	
	/** True if the user wants to write to a zip file */
	public boolean zip(){
		return zipCheckBox.getSelection();
	}
	
	/** For testing only. Sets controls to the specified values */
	public void setParameters(boolean overwrite, boolean exportTaskList, boolean exportActivationHistory, boolean exportTaskContexts, boolean zip, String destinationDir){
		overwriteCheckBox.setSelection(overwrite);
		taskListCheckBox.setSelection(exportTaskList);
		taskActivationHistoryCheckBox.setSelection(exportActivationHistory);
		taskContextsCheckBox.setSelection(exportTaskContexts);
		destDirText.setText(destinationDir);
		zipCheckBox.setSelection(zip);
	}
}
