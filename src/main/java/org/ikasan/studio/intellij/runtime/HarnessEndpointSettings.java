package org.ikasan.studio.intellij.runtime;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.analysis.TestFtpServerConfiguration;
import org.ikasan.studio.core.model.analysis.TestMailServerLinks;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.intellij.execution.HarnessOverrideService;

/** Harness-owned endpoints; configured component properties are never changed. */
public final class HarnessEndpointSettings {
    private HarnessEndpointSettings() { }
    public static TestFtpServerConfiguration localFtp() {
        return new TestFtpServerConfiguration("127.0.0.1", 2121, "ikasan", "ikasan");
    }
    public static boolean enabled(Project project, FlowElement element) {
        var service = project.getService(HarnessOverrideService.class);
        return service != null && service.isEnabled(element);
    }
    public static boolean effective(Project project, FlowElement element) {
        var selections = project.getService(HarnessOverrideService.class);
        return selections != null && (selections.hasRunningLaunches() ? selections.isActive(element) : selections.isEnabled(element));
    }
    public static TestFtpServerConfiguration ftp(Project project, FlowElement element) {
        return enabled(project, element) ? localFtp() : TestFtpServerConfiguration.from(element);
    }
    public static TestFtpServerConfiguration effectiveFtp(Project project, FlowElement element) {
        return effective(project, element) ? localFtp() : TestFtpServerConfiguration.from(element);
    }
    public static String effectiveMailHost(Project project, FlowElement element) {
        return effective(project, element) ? TestMailServerLinks.DEFAULT_SMTP_HOST : TestMailServerLinks.resolveSmtpHost(element);
    }
    public static int effectiveMailPort(Project project, FlowElement element) {
        return effective(project, element) ? TestMailServerLinks.DEFAULT_SMTP_PORT : TestMailServerLinks.resolveSmtpPort(element);
    }
    public static String mailHost(Project project, FlowElement element) {
        return enabled(project, element) ? TestMailServerLinks.DEFAULT_SMTP_HOST : TestMailServerLinks.resolveSmtpHost(element);
    }
    public static int mailPort(Project project, FlowElement element) {
        return enabled(project, element) ? TestMailServerLinks.DEFAULT_SMTP_PORT : TestMailServerLinks.resolveSmtpPort(element);
    }
}
