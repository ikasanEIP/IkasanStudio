package org.ikasan.studio.intellij.configuration;

import com.intellij.execution.RunManager;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.ui.UiContext;

import java.util.ArrayList;
import java.util.List;

public class HarnessOverridesDialogHeavyTest extends HeavyPlatformTestCase {
    public void testDialogFitsSmallListsAndCapsLongLists() throws Exception {
        var manager = RunManager.getInstance(getProject());
        var settings = manager.createConfiguration("Harness dialog sizing",
                ApplicationConfigurationType.getInstance().getConfigurationFactories()[0]);
        ((ApplicationConfiguration) settings.getConfiguration()).setMainClassName("org.ikasan.studio.boot.Application");
        manager.addConfiguration(settings);
        manager.setSelectedConfiguration(settings);
        int smallHeight = panelHeight(1);
        int largeHeight = panelHeight(40);
        assertTrue("A short list should fit its contents", smallHeight < JBUI.scale(300));
        assertTrue("Additional rows should grow the dialog", largeHeight > smallHeight);
        assertTrue("Long lists should scroll within a bounded dialog", largeHeight < JBUI.scale(550));
    }

    private int panelHeight(int components) throws Exception {
        var flow = new Flow("V4.1.6");
        flow.setName("Orders");
        for (int index = 0; index < components; index++) {
            var producer = TestFixtures.getEmailProducer("V4.1.6");
            producer.setComponentName("Email " + index);
            producer.setContainingFlow(flow);
            producer.setContainingFlowRoute(flow.getFlowRoute());
            flow.getFlowRoute().getFlowElements().add(producer);
        }
        getProject().getService(UiContext.class).setIkasanModule(
                TestFixtures.getMyFirstModuleIkasanModule("V4.1.6", new ArrayList<>(List.of(flow))));
        var dialog = new HarnessOverridesDialog(getProject());
        try {
            return dialog.createCenterPanel().getPreferredSize().height;
        } finally {
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }
}
