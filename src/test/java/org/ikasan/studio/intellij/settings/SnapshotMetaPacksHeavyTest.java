package org.ikasan.studio.intellij.settings;

import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.JCheckBox;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JComponent;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings.State;
import org.jdom.Element;

public class SnapshotMetaPacksHeavyTest extends HeavyPlatformTestCase {
    public void testSettingsApplyResetAndPersistSnapshotOptIn() {
        IkasanStudioSettings settings = IkasanStudioSettings.getInstance();
        assertNotNull(settings);
        State previous = settings.getState();
        assertNotNull(previous);
        IkasanStudioSettingsConfigurable configurable = new IkasanStudioSettingsConfigurable();
        try {
            settings.loadState(new IkasanStudioSettings.State());
            assertFalse(IkasanStudioSettings.areSnapshotMetaPacksAllowed());
            JComponent ui = configurable.createComponent();
            JCheckBox checkbox = checkbox(ui);
            assertNotNull(checkbox);
            assertFalse(checkbox.isSelected());
            checkbox.setSelected(true);
            assertTrue(configurable.isModified());
            configurable.apply();
            assertTrue(IkasanStudioSettings.areSnapshotMetaPacksAllowed());
            assertFalse(configurable.isModified());
            State appliedState = settings.getState();
            assertNotNull(appliedState);
            Element persisted = com.intellij.util.xmlb.XmlSerializer.serialize(appliedState);
            State restored = com.intellij.util.xmlb.XmlSerializer.deserialize(persisted, IkasanStudioSettings.State.class);
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

    public void testRetiredSnapshotIsNotBundled() {
        assertFalse(ComponentLibrary.getMetapackList(true).contains("V5.0.1-SNAPSHOT"));
        assertFalse(ComponentLibrary.getMetapackList(false).contains("V5.0.1-SNAPSHOT"));
        assertNull(getClass().getClassLoader().getResource("studio/development-packs/studio-pack-v5.jar"));
    }

    public void testReleasedIkasan5IsAvailableWithoutSnapshotOptIn() throws Exception {
        assertTrue(ComponentLibrary.getMetapackList(false).contains("V5.1.0"));
        MetaPackManifest manifest = ComponentLibrary.getMetaPackManifest("V5.1.0");
        assertEquals("5.1.0", manifest.ikasanVersion());
        assertEquals("25", manifest.javaVersion());
        assertFalse(ComponentLibrary.isSnapshotRuntime(manifest));
    }

    public void testSnapshotDetectionUsesRuntimeRatherThanPackArtifactRevision() {
        MetaPackManifest released = new org.ikasan.studio.core.metapack.model.MetaPackManifest(
                2, "custom-pack", "4.1.6", "17", null, null, "0.1.0-SNAPSHOT", 1);
        MetaPackManifest snapshot = new org.ikasan.studio.core.metapack.model.MetaPackManifest(
                2, "custom-pack", "9.0.0-SNAPSHOT", "25", null, null, "1.0.0", 1);
        assertFalse(ComponentLibrary.isSnapshotRuntime(released));
        assertTrue(ComponentLibrary.isSnapshotRuntime(snapshot));
    }

    private JCheckBox checkbox(Component component) {
        if (component instanceof JCheckBox box && StudioBundle.message("metaPack.AllowSnapshots").equals(box.getText())) return box;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JCheckBox found = checkbox(child);
                if (found != null) return found;
            }
        }
        return null;
    }
}
