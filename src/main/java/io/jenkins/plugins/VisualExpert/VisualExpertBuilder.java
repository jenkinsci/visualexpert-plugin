package io.jenkins.plugins.VisualExpert;

import hudson.Launcher.LocalLauncher;
import hudson.util.ArgumentListBuilder;
import hudson.model.Result;
import com.google.common.base.Strings;
import hudson.CopyOnWrite;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.ByteArrayOutputStream;
import hudson.model.StreamBuildListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
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
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import java.nio.file.Path;
import java.nio.file.Paths;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.verb.POST;

public class VisualExpertBuilder extends Builder implements SimpleBuildStep {

    private String _projectName;
    private boolean _doAnalysis = true;
    private boolean _createReferenceDocument;
    private boolean _createCodeReviewDocument;
    private ArrayList<String> projectList;
    private String _reportPath;
    private String _reportFormat;
    private boolean _generateReport = false;

    // Visual Expert Application default installation path
    private static final String DEFAULT_INSTALLATION_PATH = "C:\\Program Files\\Novalys\\Visual Expert 2024\\";
    
    // Generate Documentation Command Success Message
    private static final String GENERATE_DOCUMENTATION_SUCCESS_STRING = "Documentation generated for the project";
    
    // Analyze Project Command Success Message
    private static final String ANALYZE_PROJECT_SUCCESS_STRING = "Analysis completed successfully for the project";
    
    public static final String CONSOLE_EXE_NAME ="NOVALYS.VISUALEXPERT.CONSOLE.COMMANDLINE.EXE";

    /**
     * Constructor for VisualExpertBuilder.
     * 
     * @param projectName Visual Expert project name
     * @param reportPath Specifies the code inspection report file path
     * @param reportFormat Specifies the code inspection report format
     * @param generateReport specifies if you would like to generate code inspection report
     * @param doAnalysis specifies if the project should be analyzed
     * @param createReferenceDocument specifies if reference documentation should be generated
     * @param createCodeReviewDocument specifies if code review documentation should be generated
     */
    @DataBoundConstructor
    public VisualExpertBuilder(String projectName, String reportPath, String reportFormat, Boolean generateReport, Boolean doAnalysis, Boolean createReferenceDocument, Boolean createCodeReviewDocument) {
        this._projectName = projectName;
        this._reportPath = reportPath;
        this._reportFormat = reportFormat;
        this._generateReport = generateReport;
        this._doAnalysis = doAnalysis;
        this._createReferenceDocument = createReferenceDocument;
        this._createCodeReviewDocument = createCodeReviewDocument;
    }

    public VisualExpertInstallation getInstallPath() {
        Descriptor descriptor = (Descriptor) getDescriptor();
        for (VisualExpertInstallation i : descriptor.getInstallations()) {
            return i;
        }
        return null;
    }

    public String getReportPath() {
        return _reportPath;
    }
    
    public String getReportFormat() {
        return _reportFormat;
    }
    
    public String getProjectName() {
        return _projectName;
    }
    
    public boolean isGenerateReport() {
        return _generateReport;
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

    // Returns ArrayList of Visual Expert Projects
    public ArrayList<String> getProjectList() {
        if (projectList == null || projectList.isEmpty()) {
            VisualExpertInstallation veInstall = getInstallPath();
            String installPath = veInstall.getHome();
            String veConsoleExe = getConsoleExePath(installPath);
            projectList = VisualExpertHelper.ReadProjectsFile(veConsoleExe);
        }
        return projectList;
    }
    
    public static String getConsoleExePath(String installationfolder){
        Path folderPath = Paths.get(installationfolder);
        Path path = Paths.get(installationfolder, CONSOLE_EXE_NAME);
        if(!folderPath.endsWith(File.separator)){
            path = Paths.get(installationfolder, File.separator, CONSOLE_EXE_NAME);
        }          
        Path normalizePath = path.normalize();
        return normalizePath.toString();
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        VisualExpertInstallation ai = getInstallPath();
        if(ai == null){
            listener.getLogger().println("Do not find installation path.");
            listener.getLogger().println(Messages.VisualExpertBuilder_DescriptorImpl_errors_installPathNotSet());
            run.setResult(Result.FAILURE);
            return;
        }
        String installPath = ai.getHome();
        listener.getLogger().println("Installation Path: " + installPath);
        String veConsoleExe = getConsoleExePath(installPath);
        listener.getLogger().println("Console Exe Path: " + veConsoleExe);
        listener.getLogger().println("Visual Expert Project Name: " + _projectName);
        listener.getLogger().println("Generate code inspection report: " + _generateReport);
        listener.getLogger().println("Code Inspection Report Path: " + _reportPath);
        listener.getLogger().println("Code Inspection Report Format: " + _reportFormat);
        listener.getLogger().println("Analyze Project: " + _doAnalysis);
        listener.getLogger().println("Generate Reference Documentation: " + _createReferenceDocument);
        listener.getLogger().println("Generate Code Review Documentation: " + _createCodeReviewDocument);

        String visualExpertCommandOutputFileName = null;
        boolean isAnalysisSucceeded = true;
        boolean isReferenceDocumentGenerated = true;
        boolean isCodeReviewDocumentGenerated = true;

        if (_doAnalysis) {
            visualExpertCommandOutputFileName = VisualExpertHelper.GetVisualExpertCommandOutputFile();
            TaskListener taskListener = VisualExpertHelper.GetVisualExpertCommandOutputListener(visualExpertCommandOutputFileName);
            if(_generateReport){
                if (Util.fixEmptyAndTrim(_reportPath) == null) {
                     listener.getLogger().println(Messages.VisualExpertBuilder_DescriptorImpl_errors_missingOutputPath());
                     return;
                } else {
                    launcher.launch().cmds(VisualExpertHelper.GetCommandLine(veConsoleExe + " " + " -a -p '" + _projectName + "' -O '" + _reportPath + "' --ReportFormat '" + _reportFormat + "'"))
                            .stdout(taskListener).join();
                }
            } else {
                launcher.launch().cmds(VisualExpertHelper.GetCommandLine(veConsoleExe + " " + " -a -p '" + _projectName + "'"))
                        .stdout(taskListener).join();
            }
            isAnalysisSucceeded = VisualExpertHelper.VerifyOutput(visualExpertCommandOutputFileName, ANALYZE_PROJECT_SUCCESS_STRING, true, listener);
        }
        
        if (_createReferenceDocument) {
            if (visualExpertCommandOutputFileName == null) {
                visualExpertCommandOutputFileName = VisualExpertHelper.GetVisualExpertCommandOutputFile();
            }
            TaskListener taskListener = VisualExpertHelper.GetVisualExpertCommandOutputListener(visualExpertCommandOutputFileName);
            launcher.launch().cmds(VisualExpertHelper.GetCommandLine(veConsoleExe + " " + " -d -p '" + _projectName + "' -t reference"))
                    .stdout(taskListener).join();
            isReferenceDocumentGenerated = VisualExpertHelper.VerifyOutput(visualExpertCommandOutputFileName, GENERATE_DOCUMENTATION_SUCCESS_STRING, true, listener);
        }
        
        if (_createCodeReviewDocument) {
            if (visualExpertCommandOutputFileName == null) {
                visualExpertCommandOutputFileName = VisualExpertHelper.GetVisualExpertCommandOutputFile();
            }
            TaskListener taskListener = VisualExpertHelper.GetVisualExpertCommandOutputListener(visualExpertCommandOutputFileName);
            launcher.launch().cmds(VisualExpertHelper.GetCommandLine(veConsoleExe + " " + " -d -p '" + _projectName + "' -t codereview"))
                    .stdout(taskListener).join();
            isCodeReviewDocumentGenerated = VisualExpertHelper.VerifyOutput(visualExpertCommandOutputFileName, GENERATE_DOCUMENTATION_SUCCESS_STRING, true, listener);
        }

        if (!isAnalysisSucceeded || !isReferenceDocumentGenerated || !isCodeReviewDocumentGenerated) {
            run.setResult(Result.FAILURE);
        }
    }

    @Symbol("visualexpert")
    @Extension
    public static final class Descriptor extends BuildStepDescriptor<Builder> {
         
        @CopyOnWrite
        private volatile VisualExpertInstallation[] installations = new VisualExpertInstallation[0];

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Visual Expert";
        }
        
        public VisualExpertInstallation[] getInstallations() {
            return Arrays.copyOf(installations, installations.length);
        }

        public void setInstallations(VisualExpertInstallation... antInstallations) {
            this.installations = antInstallations;
            save();
        }

        public VisualExpertInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(VisualExpertInstallation.DescriptorImpl.class);
        }

        @POST
        public FormValidation doCheckProjectName(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            String installPath = getInstallationPath();
            if(Util.fixEmptyAndTrim(installPath) == null || installPath.equals("null")){
               return FormValidation.error(Messages.VisualExpertBuilder_DescriptorImpl_errors_installPathNotSet());
            }
            if (item == null || !item.hasPermission(Item.CONFIGURE)) { 
                return FormValidation.ok();
            }
            if (Util.fixEmptyAndTrim(value) == null || value.equals("null")) {
                return FormValidation.error(Messages.VisualExpertBuilder_DescriptorImpl_errors_missingName());
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckCreateCodeReviewDocument(@AncestorInPath Item item, @QueryParameter boolean doAnalysis, @QueryParameter boolean createReferenceDocument, @QueryParameter String value)
                throws IOException, ServletException {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) { 
                return FormValidation.ok();
            }
            if (Util.fixEmptyAndTrim(value) == null || (!Boolean.parseBoolean(value) && !doAnalysis && !createReferenceDocument)) {
                return FormValidation.error(Messages.VisualExpertBuilder_DescriptorImpl_errors_atLeastOneCheck());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckReportPath(@QueryParameter String reportPath) throws IOException, ServletException {
            if (Util.fixEmptyAndTrim(reportPath) == null) {
                return FormValidation.error("Report path is required.");
            }
            if (reportPath.contains("..")) {
                return FormValidation.error("Invalid report path: relative path segments are not allowed.");
            }
            String jenkinsHome = System.getenv("JENKINS_HOME");
            if (jenkinsHome != null) {
                String safeDirectory = jenkinsHome + File.separator + "reports";
                if (!reportPath.startsWith(safeDirectory)) {
                    return FormValidation.error("Report path must be within the safe directory: " + safeDirectory);
                }
            }
            return FormValidation.ok();
        }
        
        public ListBoxModel doFillReportFormatItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("JUNIT", "JUNIT");
            return items;
        }
       
        public ListBoxModel doFillProjectNameItems(@AncestorInPath Item item, @QueryParameter String projectName, @QueryParameter String installPath) {
            ListBoxModel model = new ListBoxModel();
            if (item == null || !item.hasPermission(Item.CONFIGURE)) { 
                return model;
            }
            model.add(new Option("--- Select Project ---", null, Strings.isNullOrEmpty(projectName)));
            installPath = getInstallationPath();
            if(Util.fixEmptyAndTrim(installPath) == null || installPath.equals("null")){
                return model;
            }
            String pathVEExe = VisualExpertBuilder.getConsoleExePath(installPath);
            ArrayList<String> projectList = VisualExpertHelper.ReadProjectsFile(pathVEExe);
            if (projectList == null) {
                return model;
            }
            for (String project : projectList) {
                model.add(new Option(project, project, project.equals(projectName)));
            }
            return model;
        }
        
        private String getInstallationPath() {
            String lInstallPath = "";
            VisualExpertInstallation install = null;
            for (VisualExpertInstallation i : getInstallations()) {
                install = i;
                break;
            }
            if (install != null) {
                lInstallPath = install.getHome();
            }
            return lInstallPath;
        }
    }

    public static class VEProjectsLister {
        public static volatile VEProjectsLister instance;
        public static volatile String lastappPath;
        private static final Object mutex = new Object();
        private ArrayList<String> commonProjectList;

        public ArrayList<String> getProjects() {
            return commonProjectList;
        }

        public static VEProjectsLister getInstance(String installationPath, String defaultProjectsFilePath) {
            VEProjectsLister result = instance;
            if (result == null) {
                synchronized (mutex) {
                    result = instance;
                    if (result == null) {
                        lastappPath = installationPath;
                        instance = result = new VEProjectsLister(installationPath, defaultProjectsFilePath);
                    } else if(lastappPath == null || !lastappPath.equals(installationPath)) {
                        lastappPath = installationPath;
                        instance = result = new VEProjectsLister(installationPath, defaultProjectsFilePath);
                    }
                }
            } else if(lastappPath == null || !lastappPath.equals(installationPath)) {
                lastappPath = installationPath;
                instance = result = new VEProjectsLister(installationPath, defaultProjectsFilePath);
            }
            return result;
        }

        private VEProjectsLister(String installationPath, String defaultProjectsFilePath) {
            this.readProjectsFile(installationPath, defaultProjectsFilePath);
        }

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
                new LocalLauncher(listener).launch().cmds(commandArgument).stdout(listener).start().joinWithTimeout(300, TimeUnit.SECONDS, listener);
                Scanner projectScanner = null;
                try {
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
                        if(isFirstLine && line.length() > 1) {
                            if(line.charAt(0) == 65279) {
                                line = line.substring(1, line.length());
                            }
                            isFirstLine = false;
                        }
                        commonProjectList.add(line);
                    }
                    projectScanner.close();
                } catch (Exception e) {
                    System.out.println("ReadProjectsFile -> Exception occurred in file: " + projectsFileName);
                } finally {
                    try {
                        if (projectScanner != null) {
                            projectScanner.close();
                        }
                    } catch (Exception eex) {}
                }
            } catch (FileNotFoundException ex) {
                System.out.println("ReadProjectsFile -> FileNotFoundException occurred in file: " + projectsFileName);
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
