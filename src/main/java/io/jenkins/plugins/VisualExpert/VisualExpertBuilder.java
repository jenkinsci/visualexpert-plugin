package io.jenkins.plugins.VisualExpert;

import hudson.Launcher.LocalLauncher;
import hudson.util.ArgumentListBuilder;
import hudson.model.Result;

import com.google.common.base.Strings;

import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.ByteArrayOutputStream;
import hudson.model.StreamBuildListener;

import java.io.IOException;
import java.util.ArrayList;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.verb.POST;


public class VisualExpertBuilder extends Builder implements SimpleBuildStep {

    private String _projectName;
    private String _installPath;
    //private String _installationDir;
    private boolean _doAnalysis = true;
    private boolean _createReferenceDocument;
    private boolean _createCodeReviewDocument;
    private ArrayList<String> projectList;

    // Visual Expert Application default installation path
    private static final String DEFAULT_INSTALLATION_PATH = "C:\\Program Files\\Novalys\\Visual Expert 2023\\";
    
    // Generate Documentation Command Success Message
    private static final String GENERATE_DOCUMENTATION_SUCCESS_STRING = "Documentation generated for the project";
    
    // Analyze Project Command Success Message
    private static final String ANALYZE_PROJECT_SUCCESS_STRING = "Analysis completed successfully for the project";
    
    private static final String CONSOLE_EXE_NAME ="NOVALYS.VISUALEXPERT.CONSOLE.EXE";

    /**
     *
     * @param installPath: Visual Expert application (Executable) containing directory path
     * @param projectName: Visual Expert project name (it should be exactly as shown in title bar of Visual Expert)
     * @param doAnalysis: specifies if it should analyze Visual Expert project or not
     * @param createReferenceDocument: specifies if it should generate reference documentation for Visual Expert project or not
     * @param createCodeReviewDocument: specifies if it should generate code review documentation for Visual Expert project or not
     */
    @DataBoundConstructor
    public VisualExpertBuilder(String installPath, String projectName, Boolean doAnalysis, Boolean createReferenceDocument,Boolean createCodeReviewDocument) {
        //this._installationDir = installationDir;
        this._installPath = installPath;
        this._projectName = projectName;
        this._doAnalysis = doAnalysis;
        this._createReferenceDocument = createReferenceDocument;
        this._createCodeReviewDocument = createCodeReviewDocument;
    }

    public VisualExpertBuilder(String installPath) {
        this._installPath =  installPath;
    }

    public String getInstallPath() {

        if (StringUtils.isEmpty(_installPath)) {
            this._installPath = DEFAULT_INSTALLATION_PATH;
        }
        return _installPath;
    }

    public String getProjectName() {
        return _projectName;
    }

    public boolean isDoAnalysis() {
        return _doAnalysis;
    }

    public boolean isCreateReferenceDocument() {
        return _createReferenceDocument;
    }
    
    public boolean isCreateCodeReviewDocument() {
        return _createCodeReviewDocument;
    }
	// Returns Array list of Visual Expert Projects
    public ArrayList<String> getProjectList() {
        if (null == projectList || projectList.isEmpty()) {
            // Call Get Projects List Visual Expert Command and returns array list of Visual Expert Projects by reading project list file
            String veConsoleExe = getConsoleExePath(_installPath);
            projectList = VisualExpertHelper.ReadProjectsFile(veConsoleExe);
        }
        return projectList;
    }
    
    public static String getConsoleExePath(String installationfolder){
        
        Path folderPath = Paths.get(installationfolder);
                
        Path path = Paths.get(installationfolder,CONSOLE_EXE_NAME);
        if(!folderPath.endsWith(File.separator)){
            path = Paths.get(installationfolder,File.separator,CONSOLE_EXE_NAME);
        }          

        Path normalizePath = path.normalize();
        String pathVEExe = normalizePath.toString();
            
        return pathVEExe;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

        listener.getLogger().println("Installation Path: " + _installPath);
        String veConsoleExe = getConsoleExePath(_installPath);
        listener.getLogger().println("Console Exe Path: " + veConsoleExe);
        listener.getLogger().println("Visual Expert Project Name: " + _projectName);
        listener.getLogger().println("Analyze Project: " + _doAnalysis);
        listener.getLogger().println("Generate Reference Documentation: " + _createReferenceDocument);
        listener.getLogger().println("Generate Code Review Documentation: " + _createCodeReviewDocument);

        String visualExpertCommandOutputFileName = null;
        boolean isAnalysisSucceeded = true;
        boolean isReferenceDocumentGenerated = true;
        boolean isCodeReviewDocumentGenerated = true;

        if (_doAnalysis) {

            // Get Visual Expert Command Output file name, Visual Expert will write command output to this file
            visualExpertCommandOutputFileName = VisualExpertHelper.GetVisualExpertCommandOutputFile();

            // Get the output task listener
            TaskListener taskListener = VisualExpertHelper.GetVisualExpertCommandOutputListener(visualExpertCommandOutputFileName);
            
            // Call Analyze Visual Expert Project Command 
            launcher.launch().cmds(VisualExpertHelper.GetCommandLine(veConsoleExe + " " + " -a -p '" + _projectName + "'")).stdout(taskListener).join();
            
            // Verify Visual Expert Comamnd Output File for Command Success/failure
            isAnalysisSucceeded = VisualExpertHelper.VerifyOutput(visualExpertCommandOutputFileName, ANALYZE_PROJECT_SUCCESS_STRING, true, listener);
        }
        
        // if Generate Reference Documentation check box is selected
        if (_createReferenceDocument) {

            // Get Visual Expert Command Output file name, Visual Expert will write command output to this file
            if (null == visualExpertCommandOutputFileName) {
                visualExpertCommandOutputFileName = VisualExpertHelper.GetVisualExpertCommandOutputFile();
            }

            // Get the output task listener
            TaskListener taskListener = VisualExpertHelper.GetVisualExpertCommandOutputListener(visualExpertCommandOutputFileName);
            
            // Call Generate Reference Documenation Visual Expert Project Comamnd 
            launcher.launch().cmds(VisualExpertHelper.GetCommandLine(veConsoleExe + " " + " -d -p '" + _projectName + "'" + " -t reference")).stdout(taskListener).join();
            
            // Verify Visual Expert Comamnd Output File for Command Success/failure
            isReferenceDocumentGenerated = VisualExpertHelper.VerifyOutput(visualExpertCommandOutputFileName, GENERATE_DOCUMENTATION_SUCCESS_STRING, true, listener);
        }
		
	// if Generate Code Review Documentation check box is selected
        if (_createCodeReviewDocument) {

            // Get Visual Expert Command Output file name, Visual Expert will write command output to this file
            if (null == visualExpertCommandOutputFileName) {
                visualExpertCommandOutputFileName = VisualExpertHelper.GetVisualExpertCommandOutputFile();
            }

            // Get the output task listener
            TaskListener taskListener = VisualExpertHelper.GetVisualExpertCommandOutputListener(visualExpertCommandOutputFileName);
            
            // Call Generate Code Review Documenation Visual Expert Project Comamnd 
            launcher.launch().cmds(VisualExpertHelper.GetCommandLine(veConsoleExe + " " + " -d -p '" + _projectName + "'" + " -t codereview")).stdout(taskListener).join();
            
            // Verify Visual Expert Comamnd Output File for Command Success/failure
            isCodeReviewDocumentGenerated = VisualExpertHelper.VerifyOutput(visualExpertCommandOutputFileName, GENERATE_DOCUMENTATION_SUCCESS_STRING, true, listener);
        }

        // Fail the build if any of the command(s) are failed
        if (!isAnalysisSucceeded || !isReferenceDocumentGenerated || !isCodeReviewDocumentGenerated) {
            run.setResult(Result.FAILURE);
        }
    }

    @Symbol("visualexpert")
    @Extension
    public static final class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Visual Expert";
        }

        @POST
        public FormValidation doCheckProjectName(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {

            if (item == null || !item.hasPermission(Item.CONFIGURE)) { 
                return FormValidation.ok();
            }
  
            if (Util.fixEmptyAndTrim(value) == null || value.equals("null")) {
                return FormValidation.error(Messages.VisualExpertBuilder_DescriptorImpl_errors_missingName());
            }

            return FormValidation.ok();
        }

        
        public FormValidation doCheckCreateCodeReviewDocument(@AncestorInPath Item item, @QueryParameter boolean doAnalysis, @QueryParameter boolean createReferenceDocument, @QueryParameter String value)
                throws IOException, ServletException {

            if (item == null || !item.hasPermission(Item.CONFIGURE)) { 
                return FormValidation.ok();
            }
            
            // Check if at least one of Analyze Project or Generate Code Review Documentation or Generate Reference Documentation must be checked else show error message
            if (Util.fixEmptyAndTrim(value) == null || (!Boolean.parseBoolean(value) && !doAnalysis && !createReferenceDocument)) {
                return FormValidation.error(Messages.VisualExpertBuilder_DescriptorImpl_errors_atLeastOneCheck());
            }

            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckInstallPath(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) { 
                return FormValidation.ok();
            }
  
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.error(Messages.VisualExpertBuilder_DescriptorImpl_errors_missingInstallPath());
            }

            /*
            Noramlize the path received from user.
            Take path of folder containing Visual Expert Exes.
            */
            String pathVEExe = getConsoleExePath(value);
            
            /* 
                Fixed issue detected by Jenkins teams.
                Path traversal vulnerability 
                It will stop users from scanning the file system.
                We will only check for the valid exe(NOVALYS.VISUALEXPERT.CONSOLE.EXE) for rest of the case it will return error.
            */            
            if(!pathVEExe.toUpperCase().endsWith(CONSOLE_EXE_NAME)){
                     return FormValidation.error(Messages.VisualExpertBuilder_DescriptorImpl_errors_invalidPath());
            }
            
            // Check if the Visual Expert Application path entered by user is existing or not so that commands can be successfully sent to Visual Expert Application
            File f = new File(pathVEExe);

            if (!f.exists()) {
                return FormValidation.error(Messages.VisualExpertBuilder_DescriptorImpl_errors_installPathNotExist());
            }

            return FormValidation.ok();
        }

        @POST
        public ListBoxModel doFillProjectNameItems(@AncestorInPath Item item, @QueryParameter String projectName, @QueryParameter String installPath) {

            // Loads list of Visual Expert Projects in list box for user's selection for Project Analysis or Documentation generation tasks automation
            ListBoxModel model = new ListBoxModel();
            
            if (item == null || !item.hasPermission(Item.CONFIGURE)) { 
                return model;
            }

            model.add(new Option("--- Select Project ---", null, Strings.isNullOrEmpty(projectName)));

            if (installPath.isEmpty()) {
                return model;
            }
            
            String pathVEExe = getConsoleExePath(installPath);

            // Reads list of Visual Expert Projects by calling Visual Expert Get Projects List Command and reading projects list from text file
            ArrayList<String> projectList = VisualExpertHelper.ReadProjectsFile(pathVEExe);

            if (null == projectList) {
                return model;
            }

            for (String project : projectList) {
                model.add(new Option(project, project, project.equals(projectName)));
            }

            return model;
        }
    }

    // Calls Visual Expert Get Project List command and reads projects from text file generated by Visual Expert Application
    public static class VEProjectsLister {

        public static volatile VEProjectsLister instance;
        public static volatile String lastappPath;
        private static final Object mutex = new Object();

        /**
         *
         * @param installationPath: Visual Expert Application (Executable) path
         * @param defaultProjectsFilePath: Visual Expert Projects List file path
         * @return Singleton instance of VEProjectsLister
         */
        public static VEProjectsLister getInstance(String installationPath, String defaultProjectsFilePath) {

            VEProjectsLister result = instance;

            if (result == null) {

                synchronized (mutex) {
                    result = instance;

                    if (result == null) {
                        lastappPath = installationPath;
                        instance = result = new VEProjectsLister(installationPath, defaultProjectsFilePath);
                    }
                    else if(lastappPath == null || !lastappPath.equals(installationPath))
                    {
                        lastappPath = installationPath;
                        instance = result = new VEProjectsLister(installationPath, defaultProjectsFilePath);
                    }
                }
            }
            else if(lastappPath == null || !lastappPath.equals(installationPath))
            {
                lastappPath = installationPath;
                instance = result = new VEProjectsLister(installationPath, defaultProjectsFilePath);
            }

            return result;
        }

        private ArrayList<String> commonProjectList;

        public ArrayList<String> getProjects() {
            return commonProjectList;
        }

        // private constructor restricted to this class itself 
        private VEProjectsLister(String installationPath, String defaultProjectsFilePath) {
            this.readProjectsFile(installationPath, defaultProjectsFilePath);
        }

        /**
         *
         * @param installationPath: Visual Expert Application (Executable) path
         * @param defaultProjectsFilePath: Visual Expert Projects List file path
         * @return Singleton instance of VEProjectsLister
         */
        public ArrayList<String> readProjectsFile(String installationPath, String defaultProjectsFilePath) {
            if (commonProjectList != null && !commonProjectList.isEmpty()) {
                return commonProjectList;
            }

            ArgumentListBuilder commandArgument = new ArgumentListBuilder();

            commandArgument.addTokenized(installationPath + " " + " -L ");

            commonProjectList = new ArrayList<String>();
            String projectsFileName = null;

            try {
    
                TaskListener listener = new StreamBuildListener(new ByteArrayOutputStream());

                // Call Get Projects List Visual Expert Comamnd
                new LocalLauncher(listener).launch().cmds(commandArgument).stdout(listener).start().joinWithTimeout(300, TimeUnit.SECONDS, listener);
                
                Scanner projectScanner = null;

                try {
                    
                    
                    // Read List of Visual Expert Projects from the text file generated by Visual Expert Application
                    String programDataFolder = System.getenv("PROGRAMDATA");

                    projectsFileName = programDataFolder + defaultProjectsFilePath;

                    File projectFile = new File(projectsFileName);

                    if (!projectFile.exists()) {
                        return commonProjectList;
                    }

                    projectScanner = new Scanner(projectFile, "UTF-8");
		    		boolean isFirstLine = true;

                    while (projectScanner.hasNextLine()) {
                    	
						String line = projectScanner.nextLine();
			
						if(isFirstLine && line.length() > 1)
                        {
                            if(line.charAt(0) == 65279)
                            {
                                line = line.substring(1, line.length());
                            }
                            isFirstLine = false;
                        }
                        commonProjectList.add(line);
                    }

                    projectScanner.close();

                } catch (Exception e) {
                    System.out.println("ReadProjectsFile -> Exception occurred  in file: " + projectsFileName);
                } finally {

                    try {
                        if (projectScanner != null) {
                            projectScanner.close();
                        }
                    } catch (Exception eex) {

                    }

                }

            } catch (FileNotFoundException ex) {
                System.out.println("ReadProjectsFile -> FileNotFoundException occurred  in file: " + projectsFileName);
            } catch (IOException x) {
                System.out.println("ReadProjectsFile -> IOException occurred in file: " + projectsFileName);
            } catch (Exception e) {
                System.out.println("ReadProjectsFile -> Exception occurred");
            }

            System.out.println("Total Projects -> " + commonProjectList.size());
            return commonProjectList;
        }

    }
}
