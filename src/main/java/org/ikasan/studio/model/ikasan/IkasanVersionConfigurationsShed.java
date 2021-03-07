package org.ikasan.studio.model.ikasan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class will hold meta data for components driven by the ikasan version number
 */
public class IkasanVersionConfigurationsShed {
    public static final String V_3_0_0 = "V_3_0_0";
    // A series of maps will be created reflecting the configuarions for each components type for all released versions.
    private static Map<String, Map<IkasanFlowComponentType, List<IkasanComponentPropertyMeta>>> configCache = new HashMap<>();
    private static List<IkasanComponentPropertyMeta> emptyList =  new ArrayList<>();

    static {
        List<IkasanComponentPropertyMeta> commonProperties = new ArrayList<>();
        commonProperties.add(new IkasanComponentPropertyMeta(true, false, IkasanComponentPropertyMeta.NAME, "", String.class, "", "Display Name"));

        List<IkasanComponentPropertyMeta> scheduleConsumerProperties = new ArrayList<>();
        scheduleConsumerProperties.add(new IkasanComponentPropertyMeta(false, false, "ignoreMisfire", "", Boolean.class, "true", "Default is false. When false the scheduler will try to fire any misfires as soon as possible. When true this will tell the scheduler to ignore misfires and wait for the next scheduled time."));
        scheduleConsumerProperties.add(new IkasanComponentPropertyMeta(false, false, "eager", "", Boolean.class, "true", "Tells the consumer to immediately re-schedule itself to invoke again, regardless of the initial cron schedule. This is useful for consumers dealing with lots of data which can be continuously consumed until exhausted, at which point it returns to its initial cron schedule."));
        scheduleConsumerProperties.add(new IkasanComponentPropertyMeta(false, false, "maxEagerCallbacks", "", Integer.class, 0, "        scheduleConsumerProperties.add(new IkasanComponentPropertyMeta(\"maxEagerCallbacks\", Integer.class, \"Default is false. When false the scheduler will try to fire any misfires as soon as possible. When true this will tell the scheduler to ignore misfires and wait for the next scheduled time.\"));\n"));
        scheduleConsumerProperties.add(new IkasanComponentPropertyMeta(false, false, "timezone", "", String.class, null, "Optional timezone used by Quartz scheduler."));
        Map<IkasanFlowComponentType, List<IkasanComponentPropertyMeta>> flowElementTypeMap = new HashMap<>();

        flowElementTypeMap.put(IkasanFlowComponentType.SCHEDULED_CONSUMER, scheduleConsumerProperties);

        for (IkasanFlowComponentType ikasanComponent : IkasanFlowComponentType.values()) {
            flowElementTypeMap.put(ikasanComponent, commonProperties);
        }

        configCache.put(V_3_0_0, flowElementTypeMap);
    }

    /**
     * For the given ikasan versions, read the (yet to b created) resources file and get all the
     * properties that could be defined for the component.
     * @return
     */
    public static List<IkasanComponentPropertyMeta> getPropertiesForIkasanVersion(IkasanFlowComponentType ikasanFlowComponentType) {
        return getPropertiesForIkasanVersion(V_3_0_0, ikasanFlowComponentType);
    }

    public static List<IkasanComponentPropertyMeta> getPropertiesForIkasanVersion(String ikasanVersion, IkasanFlowComponentType ikasanFlowComponentType) {
        List<IkasanComponentPropertyMeta> returnList = null;

        Map<IkasanFlowComponentType, List<IkasanComponentPropertyMeta>> flowElementTypeMap = configCache.get(ikasanVersion);
        if (flowElementTypeMap != null) {
            returnList = flowElementTypeMap.get(ikasanFlowComponentType);
        }

        return returnList != null ? returnList : emptyList;
    }
}
