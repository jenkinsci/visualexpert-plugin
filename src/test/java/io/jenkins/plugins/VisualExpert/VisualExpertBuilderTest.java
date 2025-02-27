package io.jenkins.plugins.VisualExpert;

import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.*;

public class VisualExpertBuilderTest {

    @Test
    public void testDoCheckReportPathEmpty() throws IOException, ServletException {
        VisualExpertBuilder.Descriptor descriptor = new VisualExpertBuilder.Descriptor();
        FormValidation validation = descriptor.doCheckReportPath("");
        assertEquals("Expected error for empty report path", FormValidation.Kind.ERROR, validation.kind);
        assertTrue(validation.getMessage().contains("Report path is required"));
    }

    @Test
    public void testDoCheckReportPathRelativePath() throws IOException, ServletException {
        VisualExpertBuilder.Descriptor descriptor = new VisualExpertBuilder.Descriptor();
        FormValidation validation = descriptor.doCheckReportPath("some/../path");
        assertEquals("Expected error for relative path segments", FormValidation.Kind.ERROR, validation.kind);
        assertTrue(validation.getMessage().contains("Invalid report path"));
    }

    @Test
    public void testDoCheckReportPathOutsideSafeDirectory() throws IOException, ServletException {
        String jenkinsHome = System.getenv("JENKINS_HOME");
        Assume.assumeNotNull("JENKINS_HOME must be set for this test", jenkinsHome);
        VisualExpertBuilder.Descriptor descriptor = new VisualExpertBuilder.Descriptor();
        // Provide a report path that does not start with the safe directory
        FormValidation validation = descriptor.doCheckReportPath("/tmp/report.txt");
        assertEquals("Expected error for report path outside safe directory", FormValidation.Kind.ERROR, validation.kind);
        String safeDirectory = jenkinsHome + File.separator + "reports";
        assertTrue(validation.getMessage().contains("Report path must be within the safe directory: " + safeDirectory));
    }

    @Test
    public void testDoCheckReportPathValid() throws IOException, ServletException {
        VisualExpertBuilder.Descriptor descriptor = new VisualExpertBuilder.Descriptor();
        String jenkinsHome = System.getenv("JENKINS_HOME");
        if (jenkinsHome != null) {
            String safeDirectory = jenkinsHome + File.separator + "reports";
            FormValidation validation = descriptor.doCheckReportPath(safeDirectory + File.separator + "project-report.xml");
            assertEquals("Expected valid report path within safe directory", FormValidation.Kind.OK, validation.kind);
        } else {
            // If JENKINS_HOME is not set, any valid path without '..' should pass
            FormValidation validation = descriptor.doCheckReportPath("/any/path/report.xml");
            assertEquals("Expected valid report path", FormValidation.Kind.OK, validation.kind);
        }
    }
}
