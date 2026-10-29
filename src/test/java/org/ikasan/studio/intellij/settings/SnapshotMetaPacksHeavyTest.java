package org.ikasan.studio.intellij.settings;

import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.JCheckBox;
import java.awt.Component;
import java.awt.Container;

public class SnapshotMetaPacksHeavyTest extends HeavyPlatformTestCase {
    public void testSettingsApplyResetAndPersistSnapshotOptIn() {
        var settings = IkasanStudioSettings.getInstance();
        assertNotNull(settings);
        var previous = settings.getState();
        assertNotNull(previous);
        var configurable = new IkasanStudioSettingsConfigurable();
        try {
            settings.loadState(new IkasanStudioSettings.State());
            assertFalse(IkasanStudioSettings.areSnapshotMetaPacksAllowed());
            var ui = configurable.createComponent();
            var checkbox = checkbox(ui);
            assertNotNull(checkbox);
            assertFalse(checkbox.isSelected());
            checkbox.setSelected(true);
            assertTrue(configurable.isModified());
            configurable.apply();
            assertTrue(IkasanStudioSettings.areSnapshotMetaPacksAllowed());
            assertFalse(configurable.isModified());
            var appliedState = settings.getState();
            assertNotNull(appliedState);
            var persisted = com.intellij.util.xmlb.XmlSerializer.serialize(appliedState);
            var restored = com.intellij.util.xmlb.XmlSerializer.deserialize(persisted, IkasanStudioSettings.State.class);
            settings.loadState(restored);
            configurable.reset();
            assertTrue(checkbox.isSelected());
            checkbox.setSelected(false);
            configurable.apply();
            assertFalse(IkasanStudioSettings.areSnapshotMetaPacksAllowed());
        } finally {
            settings.loadState(previous);
            configurable.disposeUIResources();
        }
    }

    public void testOptOutHidesSnapshotSelectionsButKeepsExistingPackUsable() throws Exception {
        assertTrue(ComponentLibrary.getMetapackList(true).contains("V5.0.1-SNAPSHOT"));
        assertFalse(ComponentLibrary.getMetapackList(false).contains("V5.0.1-SNAPSHOT"));
        assertTrue(ComponentLibrary.getMetapackList(false).containsAll(java.util.List.of("V3.3.9", "V4.1.6")));
        assertEquals("26", ComponentLibrary.getMetaPackManifest("V5.0.1-SNAPSHOT").javaVersion());
    }

    public void testReleasedIkasan5IsAvailableWithoutSnapshotOptIn() throws Exception {
        assertTrue(ComponentLibrary.getMetapackList(false).contains("V5.1.0"));
        var manifest = ComponentLibrary.getMetaPackManifest("V5.1.0");
        assertEquals("5.1.0", manifest.ikasanVersion());
        assertEquals("25", manifest.javaVersion());
        assertFalse(ComponentLibrary.isSnapshotRuntime(manifest));
    }

    public void testSnapshotDetectionUsesRuntimeRatherThanPackArtifactRevision() {
        var released = new org.ikasan.studio.core.metapack.model.MetaPackManifest(
                2, "custom-pack", "4.1.6", "17", null, null, "0.1.0-SNAPSHOT", 1);
        var snapshot = new org.ikasan.studio.core.metapack.model.MetaPackManifest(
                2, "custom-pack", "5.0.1-SNAPSHOT", "26", null, null, "1.0.0", 1);
        assertFalse(ComponentLibrary.isSnapshotRuntime(released));
        assertTrue(ComponentLibrary.isSnapshotRuntime(snapshot));
    }

    private JCheckBox checkbox(Component component) {
        if (component instanceof JCheckBox box && StudioBundle.message("metaPack.AllowSnapshots").equals(box.getText())) return box;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                var found = checkbox(child);
                if (found != null) return found;
            }
        }
        return null;
    }
}
