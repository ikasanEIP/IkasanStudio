package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.io.GenericPojo;
import org.ikasan.studio.io.PojoDeserialisation;

import javax.swing.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.ikasan.studio.StudioUtils.getDirectories;

/**
 * This class aggregates all the defined Ikasan components
 */
public class IkasanComponentLibrary {
    private static final String VERSION_PACK_BASE_DIR = "studio/";
    public static final String STD_IKASAN_PACK = "V3.3.x";  // Short term convenience, long term this must be pak driven
    public static final String TEST_IKASAN_PACK = "Vtest.x";
    private static String SMALL_ICON_NAME = "paletteSmall.png";
    private static String NORMAL_ICON_NAME = "paletteNormal.png";
    private static String LARGE_ICON_NAME = "paletteLarge.png";
    private static final Logger LOG = Logger.getInstance("#IkasanComponentLibrary");

    // IkasanVersionPack -> Ikasan Component Name -> Ikasan Component Meta
    private static Map<String, Map<String, IkasanComponentMetan>> versionedComponenetsLibrary = new HashMap<>(new HashMap<>());

    /**
     * refresh the component library, by this point the version of Ikasan will have been chosen.
     */
    public static void refreshComponentLibrary() {
        refreshComponentLibrary(STD_IKASAN_PACK);
    }

    /**
     * Refresh the component library.
     * By making this protected, we intend to limit that only the test can state an alternate root
     * At some point we may need to key this by project or version since all open projects will share this.
     * @param ikasanVersionPack to search for components
     */
    public static void refreshComponentLibrary(final String ikasanVersionPack) {
        Map<String, IkasanComponentMetan> newIkasanComponentMetanMap
                = new HashMap<>();
        String baseDirectory = VERSION_PACK_BASE_DIR + ikasanVersionPack + "/components";
        String[] componentDirectories = null;
        try {
            componentDirectories = getDirectories(baseDirectory);
        } catch (URISyntaxException e) {
            LOG.error("Could not scan the directory " + baseDirectory + " in order to populate the component library.", e);
        }

        assert componentDirectories != null;
        for(String componentName : componentDirectories) {
            final String componentBaseDirectory = baseDirectory+"/"+componentName;
            IkasanComponentMetan ikasanComponentMetan = null;
            try {
                ikasanComponentMetan = PojoDeserialisation.deserializePojo(
                        componentBaseDirectory+"/attributes_en_GB.json",
                        new TypeReference<GenericPojo<IkasanComponentMetan>>() {});
            } catch (StudioException e) {
                LOG.warn("While trying to populate the component library from base directory " + baseDirectory +
                        " there was an error generating the details for component " + componentName +
                        " review the Ikasan version pack, perhaps reinstall or use an alternate version");
            }
            ikasanComponentMetan.setSmallIcon(getImageIcon(componentBaseDirectory + "/" + SMALL_ICON_NAME));
            ikasanComponentMetan.setCanvasIcon(getImageIcon(componentBaseDirectory + "/" + NORMAL_ICON_NAME));
            newIkasanComponentMetanMap.put(componentName, ikasanComponentMetan);
        }

        synchronized (IkasanComponentLibrary.class) {
            versionedComponenetsLibrary.put(ikasanVersionPack, newIkasanComponentMetanMap);
        }
    }

    /**
     * Attempt to minimize the synchronized lock. We will guarentee the Map is valid and not in the process
     * of being updated, but it is possible that by the time the consumer of this method returns, the map may have
     * been updated. This must be the working assumption.
     * @return the reference to the current component library
     */
    protected synchronized static Map<String, IkasanComponentMetan> geIkasanComponentMetanMap(final String version) {
        return versionedComponenetsLibrary.get(version);
    }

    // Currently, restrict access to the Map
    public static IkasanComponentMetan getIkasanComponent(String version, String key) {
        Map<String, IkasanComponentMetan> safeIkasanComponentMetanMap = geIkasanComponentMetanMap(version);
        return safeIkasanComponentMetanMap.get(key);
    }

    public static Set<String> getIkasanComponentList(String version) {
        Map<String, IkasanComponentMetan> safeIkasanComponentMetanMap = geIkasanComponentMetanMap(version);
        return safeIkasanComponentMetanMap.keySet();
    }

    public static int getNumberOfComponents(String version) {
        return getIkasanComponentList(version).size();
    }



    private static ImageIcon getImageIcon(String iconLocation) {
        ImageIcon imageIcon = null;
        URL iconURL = StudioUtils.class.getResource(iconLocation);
        if (iconURL == null) {
            LOG.warn("Could not create Icon for " + iconLocation);
        } else {
            imageIcon = new ImageIcon(iconURL);
        }
        return imageIcon;
    }
}
