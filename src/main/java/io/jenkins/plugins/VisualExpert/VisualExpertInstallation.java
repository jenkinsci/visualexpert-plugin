/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.jenkins.plugins.VisualExpert;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import static io.jenkins.plugins.VisualExpert.VisualExpertBuilder.CONSOLE_EXE_NAME;
import static io.jenkins.plugins.VisualExpert.VisualExpertBuilder.getConsoleExePath;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 *
 * @author AndrinIvanko
 */
public final class VisualExpertInstallation extends ToolInstallation implements NodeSpecific<VisualExpertInstallation>, EnvironmentSpecific<VisualExpertInstallation> {

    private static final long serialVersionUID = 1L;
    private final String defaultArgs;

    @DataBoundConstructor
    public VisualExpertInstallation(String name, String home, String defaultArgs) {
        super(name, home, null);
        this.defaultArgs = Util.fixEmpty(defaultArgs);
    }

    @Override
    public VisualExpertInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new VisualExpertInstallation(getName(), translateFor(node, log), getDefaultArgs());
    }

    @Override
    public VisualExpertInstallation forEnvironment(EnvVars environment) {
        return new VisualExpertInstallation(getName(), environment.expand(getHome()), getDefaultArgs());
    }

    public String getDefaultArgs() {
        return this.defaultArgs;
    }

    @Extension @Symbol("visualexpertinstallation")
    public static class DescriptorImpl extends ToolDescriptor<VisualExpertInstallation> {

        @Override
        public String getDisplayName() {
            return "Visual Expert";
        }

        @Override
        public VisualExpertInstallation[] getInstallations() {
            return getDescriptor().getInstallations();
        }

        @Override
        public void setInstallations(VisualExpertInstallation... installations) {
            getDescriptor().setInstallations(installations);
        }
        
        private VisualExpertBuilder.Descriptor getDescriptor() {
            Jenkins jenkins = Jenkins.get();
            if (jenkins != null && jenkins.getDescriptorByType(VisualExpertBuilder.Descriptor.class) != null) {
                return jenkins.getDescriptorByType(VisualExpertBuilder.Descriptor.class);
            } else {
                // To stick with current behavior and meet findbugs requirements
                throw new NullPointerException("VisualExpertBuilder.Descriptor is null");
            }
        }
        
        @POST
        @Override
        public FormValidation doCheckHome( @QueryParameter File value) {
            
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            
//            if (item == null || !item.hasPermission(Item.CONFIGURE)) { 
//                return FormValidation.ok();
//            }

            String installationPath =value.getPath();
  
            if (Util.fixEmptyAndTrim(installationPath) == null) {
                return FormValidation.error(Messages.VisualExpertBuilder_DescriptorImpl_errors_missingInstallPath());
            }

            /*
            Noramlize the path received from user.
            Take path of folder containing Visual Expert Exes.
            */
            String pathVEExe = getConsoleExePath(installationPath);
            
            /* 
                Fixed issue detected by Jenkins teams.
                Path traversal vulnerability 
                It will stop users from scanning the file system.
                We will only check for the valid exe(NOVALYS.VISUALEXPERT.CONSOLE.EXE) for rest of the case it will return error.
            */            
            if(!pathVEExe.toUpperCase().endsWith(VisualExpertBuilder.CONSOLE_EXE_NAME)){
                     return FormValidation.error(Messages.VisualExpertBuilder_DescriptorImpl_errors_invalidPath());
            }
            
            // Check if the Visual Expert Application path entered by user is existing or not so that commands can be successfully sent to Visual Expert Application
            File f = new File(pathVEExe);

            if (!f.exists()) {
                return FormValidation.error(Messages.VisualExpertBuilder_DescriptorImpl_errors_installPathNotExist());
            }

            return FormValidation.ok();
        }
    }
}
