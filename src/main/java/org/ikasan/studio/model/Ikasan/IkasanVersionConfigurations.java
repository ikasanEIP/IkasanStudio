package org.ikasan.studio.model.Ikasan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class will hold meta data for componets driven by the Ikasan version number
 */
public class IkasanVersionConfigurations {
    public static final String V_3_0_0 = "V_3_0_0";
    // A series of maps will be created reflecting the configuarions for each components type for all released versions.
    private static Map<String, Map<IkasanFlowElementType, List<IkasanComponentProperty>>> configCache = new HashMap<>();
    private List<IkasanComponentProperty> emptyList =  new ArrayList<>();

    static {
        List<IkasanComponentProperty> scheduleConsumerProperties = new ArrayList<>();
        scheduleConsumerProperties.add(new IkasanComponentProperty("ignoreMisfire", boolean.class, "Default is false. When false the scheduler will try to fire any misfires as soon as possible. When true this will tell the scheduler to ignore misfires and wait for the next scheduled time."));
        scheduleConsumerProperties.add(new IkasanComponentProperty("eager", boolean.class, "Tells the consumer to immediately re-schedule itself to invoke again, regardless of the initial cron schedule. This is useful for consumers dealing with lots of data which can be continuously consumed until exhausted, at which point it returns to its initial cron schedule."));
        scheduleConsumerProperties.add(new IkasanComponentProperty("maxEagerCallbacks", Integer.class, "        scheduleConsumerProperties.add(new IkasanComponentProperty(\"maxEagerCallbacks\", Integer.class, \"Default is false. When false the scheduler will try to fire any misfires as soon as possible. When true this will tell the scheduler to ignore misfires and wait for the next scheduled time.\"));\n"));
        scheduleConsumerProperties.add(new IkasanComponentProperty("timezone", String.class, "Optional timezone used by Quartz scheduler."));
        Map<IkasanFlowElementType, List<IkasanComponentProperty>> flowElementTypeMap = new HashMap<>();
        flowElementTypeMap.put(IkasanFlowElementType.SCHEDULED_CONSUMER, scheduleConsumerProperties);
        configCache.put(V_3_0_0, flowElementTypeMap);
    }

    /**
     * For the given Ikasan versions, read the (yet to b created) resources file and get all the
     * properties that could be defined for the component.
     * @return
     */
    public List<IkasanComponentProperty> getPropertiesForIkasanVersion(String ikasanVersion, IkasanFlowElementType ikasanFlowElementType) {
        List<IkasanComponentProperty> returnList = null;

        Map<IkasanFlowElementType, List<IkasanComponentProperty>> flowElementTypeMap = configCache.get(ikasanVersion);
        if (flowElementTypeMap != null) {
            returnList = flowElementTypeMap.get(ikasanFlowElementType);
        }

        return returnList != null ? returnList : emptyList;
    }
}
