package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ikasan.studio.StudioUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Focuses on the ikasan technical details of a component i.e. type, properties etc
 *
 * No UI specific elements should be present in this class (@see org.ikasan.studio.ui.model.IkasanFlowUIComponent for that)
 */
public enum IkasanComponentType implements Serializable {
    MODULE(IkasanComponentCategory.MODULE, false, true, "", IkasanComponentDependency.BASIC),
    FLOW(IkasanComponentCategory.FLOW, false, true, "", IkasanComponentDependency.NONE),
    BROKER(IkasanComponentCategory.BROKER, false, true, "broker", IkasanComponentDependency.NONE),
    DB_BROKER(IkasanComponentCategory.BROKER, false, true, "DbBroker", IkasanComponentDependency.NONE),
    DELAY_GENERATION_BROKER(IkasanComponentCategory.BROKER, false, true, "DelayGenerationBroker", IkasanComponentDependency.NONE),
    EXCEPTION_GENERATING_BROKER(IkasanComponentCategory.BROKER, false, true, "ExceptionGenerationgBroker", IkasanComponentDependency.NONE),
    SCHEDULE_RULE_CHECK_BROKER(IkasanComponentCategory.BROKER, false, true, "ScheduledRuleCheckBroker", IkasanComponentDependency.NONE),

    DB_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "DBConsumer", IkasanComponentDependency.NONE),
//    EVENT_DRIVEN_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "eventDrivenConsumer", IkasanComponentDependency.NONE),  cant seem to find this in ikasan code base.
    EVENT_GENERATING_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "eventGeneratingConsumer", IkasanComponentDependency.NONE),
    SCHEDULED_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "scheduledConsumer", IkasanComponentDependency.NONE),
    FTP_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "ftpConsumer", IkasanComponentDependency.NONE),
//    JMS_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "jmsConsumer", IkasanComponentDependency.NONE),
    SPRING_JMS_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "jmsConsumer", IkasanComponentDependency.JMS),
    SPRING_JMS_CONSUMER_BASIC_AMQ(IkasanComponentCategory.CONSUMER, false, true, "jmsConsumer", IkasanComponentDependency.JMS),
    SFTP_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "sftpConsumer", IkasanComponentDependency.NONE),
    LOCAL_FILE_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "fileConsumer", IkasanComponentDependency.NONE),
    MONGO_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "mongoConsumer", IkasanComponentDependency.NONE),

    // @todo, maybe introduce the base class into this list, might conflict though when matching custom components.
    CUSTOM_CONVERTER(IkasanComponentCategory.CONVERTER, true, false, "Converter", IkasanComponentDependency.NONE),  // This will really be the implemented interface
    JSON_XML_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "JsonXmlConverter", IkasanComponentDependency.CONVERTER),
    MAP_MESSAGE_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "MapMessageToObjectConverter", IkasanComponentDependency.CONVERTER),
    MAP_MESSAGE_TO_PAYLOAD_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "MapMessageToPayloadConverter", IkasanComponentDependency.CONVERTER),
    OBJECT_MESSAGE_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "org.ikasan.component.converter.jms.ObjectMessageToObjectConverter", IkasanComponentDependency.CONVERTER),
    OBJECT_MESSAGE_TO_XML_STRING_CONVERTER(IkasanComponentCategory.CONVERTER, false, true, "objectToXmlStringConverter", IkasanComponentDependency.CONVERTER),
//    OBJECT_TO_XML_CONVERTER(IkasanComponentCategory.CONVERTER, false, "ObjectToXmlStringConverter", IkasanComponentDependency.CONVERTER),
    PAYLOAD_TO_MAP_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "PayloadToMapConverter", IkasanComponentDependency.CONVERTER),
    TEXT_MESSAGE_TO_STRING_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "TextMessageToStringConverter", IkasanComponentDependency.CONVERTER),
    THREAD_SAFE_XSLT_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "ThreadSafeXsltConverter", IkasanComponentDependency.CONVERTER),
    XML_BYTE_ARRAY_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "XmlByteArrayToObjectConverter", IkasanComponentDependency.CONVERTER),
    XML_STRING_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, true, "xmlStringToObjectConverter", IkasanComponentDependency.CONVERTER),
    XML_TO_JSON_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "XmlJsonConverter", IkasanComponentDependency.CONVERTER),
    XSLT_CONFIGURATION_PARAMETER_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "XsltConfigurationParameterConverter", IkasanComponentDependency.CONVERTER),
    XSLT_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "XsltConverter", IkasanComponentDependency.CONVERTER),

    MESSAGE_FILTER(IkasanComponentCategory.FILTER, true, false, "FilterInvokerConfiguration", IkasanComponentDependency.NONE),

    CHANNEL(IkasanComponentCategory.ENDPOINT, false, true, "messageChannel", IkasanComponentDependency.NONE),
    FTP_LOCATION(IkasanComponentCategory.ENDPOINT, false, true, "ftpLocation", IkasanComponentDependency.NONE),
    SFTP_LOCATION(IkasanComponentCategory.ENDPOINT, false, true, "sftpLocation", IkasanComponentDependency.NONE),
    DB(IkasanComponentCategory.ENDPOINT, false, true, "message-store", IkasanComponentDependency.NONE),

    EXCEPTION_RESOLVER(IkasanComponentCategory.EXCEPTION_RESOLVER, false, true, "getExceptionResolverBuilder", IkasanComponentDependency.NONE),
    ON_EXCEPTION(IkasanComponentCategory.EXCEPTION_RESOLVER, false, true, "addExceptionToAction", IkasanComponentDependency.NONE),

    LIST_SPLITTER(IkasanComponentCategory.SPLITTER, false, true, "listSplitter", IkasanComponentDependency.NONE),
    SPLITTER(IkasanComponentCategory.SPLITTER, false, true, "splitter", IkasanComponentDependency.NONE),

    TRANSLATOR(IkasanComponentCategory.TRANSLATER, false, true, "Translator", IkasanComponentDependency.NONE),

    DB_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "DBProducer", IkasanComponentDependency.NONE),
    DEV_NULL_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "devNullProducer", IkasanComponentDependency.NONE),
    EMAIL_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "emailProducer", IkasanComponentDependency.NONE),
    FTP_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "ftpProducer", IkasanComponentDependency.NONE),
    SFTP_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "sftpProducer", IkasanComponentDependency.NONE),
    JMS_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "jmsProducer", IkasanComponentDependency.JMS),
    JMS_PRODUCER_BASIC_AMQ(IkasanComponentCategory.PRODUCER, false, true, "jmsProducer", IkasanComponentDependency.JMS),
    LOG_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "logProducer", IkasanComponentDependency.NONE),

    SINGLE_RECIPIENT_ROUTER(IkasanComponentCategory.ROUTER, false, true, "SingleRecipientRouter", IkasanComponentDependency.NONE),
    MULTI_RECIPIENT_ROUTER(IkasanComponentCategory.ROUTER, false, true, "MultiRecipientRouter", IkasanComponentDependency.NONE),

    BESPOKE(IkasanComponentCategory.UNKNOWN, true, false, "bespoke", IkasanComponentDependency.NONE),
    UNKNOWN(IkasanComponentCategory.UNKNOWN, false, false, "unknown", IkasanComponentDependency.NONE);

    // --- Population ----
    // Remember, to make the below available to constructor, they must be instance and not static
    private static final Logger LOG = Logger.getLogger(IkasanComponentType.class);
    public final String associatedMethodName;
    public final boolean bespokeClass;
    //@todo move this to component copnfig.csv
    public final boolean usesBuilder;
    public final IkasanComponentCategory elementCategory;
    @JsonIgnore
    public final IkasanComponentDependency componentDependency;
    @JsonIgnore
    protected Map<IkasanComponentPropertyMetaKey, IkasanComponentPropertyMeta> metadataMap;

    /**
     * Represents a flow element e.g. JMS Consumer, DB Consumer et
     * @param elementCategory e.g. CONSUMER, PRODUCER
     * @param bespokeClass is the class generated bespoke i.e. generated outside the Studio package structure
     * @param usesBuilder does the component use the component builder (some components need to be instantiated with 'new')
     * @param associatedMethodName in the source code that can be used to identify this element, typically used by ComponentBuilder.
     * @param componentDependency the pom jars associated with this component.
     */
    IkasanComponentType(IkasanComponentCategory elementCategory, boolean bespokeClass, boolean usesBuilder, String associatedMethodName, IkasanComponentDependency componentDependency) {
        this.bespokeClass = bespokeClass;
        this.usesBuilder = usesBuilder;
        this.associatedMethodName = associatedMethodName;
        this.elementCategory = elementCategory;
        this.componentDependency = componentDependency;

        // @todo having this in the constructor is too risky, and exception even if caught will prevent the plugin from working.
        // @todo may be more efficient to split into mandatory and optional properties
        metadataMap = StudioUtils.readIkasanComponentProperties(this.toString());
    }

    /**
     * Get a list of the mandatory properties for this component.
     * @return A map of the mandatory properties for this component
     */
    public Map<IkasanComponentPropertyMetaKey, IkasanComponentProperty> getMandatoryProperties() {
        Map<IkasanComponentPropertyMetaKey, IkasanComponentProperty> mandatoryProperties = new TreeMap<>();
        for (Map.Entry<IkasanComponentPropertyMetaKey, IkasanComponentPropertyMeta> entry : metadataMap.entrySet()) {
            if (!entry.getValue().subProperties && entry.getValue().isMandatory()) {
                mandatoryProperties.put(entry.getKey(), new IkasanComponentProperty(entry.getValue()));
            }
        }
        return mandatoryProperties;
    }

    public Map<IkasanComponentPropertyMetaKey, IkasanComponentPropertyMeta> getMetadataMap() {
        return metadataMap;
    }

    public IkasanComponentPropertyMeta getMetadata(String propertyName) {
        // If we just supply the propertyName, assume it is the simple type i.e. 1 group, 1 constructor
        return metadataMap.get(new IkasanComponentPropertyMetaKey(propertyName));
    }

    public List<IkasanComponentPropertyMeta> getMetadataList(String propertyName) {
        List<IkasanComponentPropertyMeta> metadataList = new ArrayList<>();
        IkasanComponentPropertyMeta meta = metadataMap.get(new IkasanComponentPropertyMetaKey(propertyName));
        if (meta.hasSubProperties()) {
            metadataList = new ArrayList<>(meta.getSubProperties().values());
        } else {
            metadataList.add(meta);
        }
        return metadataList;
    }

    public IkasanComponentPropertyMeta getMetadata(IkasanComponentPropertyMetaKey propertyName) {
        return metadataMap.get(propertyName);
    }

    public IkasanComponentPropertyMeta getMetaDataForPropertyName(final String propertyName) {
        // If we just supply the propertyName, assume it is the simple type i.e. 1 group, 1 constructor
        return metadataMap.get(new IkasanComponentPropertyMetaKey(propertyName));
    }

    /**
     * Return the list of properties for the given component
     * @return the list of properties
     */
    public List<String> getPropertyNames() {
        return new ArrayList<>(
                metadataMap.keySet().stream()
                .map(x->x.getPropertyName())
                // guarantee no duplicates
                .collect(Collectors.toSet())
            );

//        Set<String> uniqueProperties = new HashSet<>();
//        for(IkasanComponentPropertyMetaKey key : metadataMap.keySet()) {
//            uniqueProperties.add(key.getPropertyName());
//        }
//        return new ArrayList<>(uniqueProperties);
    }

    /**
     * Determine if the given property name already exists for this component type
     * @return true if there exists a property with the same name as the supplied property
     */
    public boolean hasProperty(String propertyName) {
        return metadataMap.keySet().stream()
                .filter(x->x.getPropertyName().equals(propertyName))
                .findAny()
                .isPresent();
//        for(IkasanComponentPropertyMetaKey key : metadataMap.keySet()) {
//            if (key.getPropertyName().equals(propertyName)) {
//                return true;
//            }
//        }
//        return false;
    }

    public IkasanComponentPropertyMeta getMetaDataForPropertyName(final IkasanComponentPropertyMetaKey propertyName) {
        return metadataMap.get(propertyName);
    }

    public static IkasanComponentType parseMethodName(String methodName) {
        if (methodName != null) {
            for (IkasanComponentType name : IkasanComponentType.values()) {
                if (name.associatedMethodName != null &&
                    name.associatedMethodName.length() > 0 &&
                    StringUtils.equalsIgnoreCase(methodName, name.associatedMethodName)) {
                    return name;
                }
            }
        }
        return UNKNOWN;
    }

    /**
     * Given the methodname e.g. Filter, identify and return the corresponding Component Type
     * @param methodName from the code that created the component
     * @return the Component Type associated with that creator method name.
     */
    public static IkasanComponentType parseCategoryType(String methodName) {
        if (methodName != null) {
            // First try for an exact match
            for (IkasanComponentType name : IkasanComponentType.values()) {
                if (!name.elementCategory.associatedMethodName.isEmpty() &&
                    StringUtils.equalsIgnoreCase(methodName, name.elementCategory.associatedMethodName)) {
                    return name;
                }
            }
            // Now more fuzzy match
            for (IkasanComponentType name : IkasanComponentType.values()) {
                if (!name.elementCategory.associatedMethodName.isEmpty() &&
                    StringUtils.containsIgnoreCase(methodName, name.elementCategory.associatedMethodName)) {
                    return name;
                }
            }
        }
        return UNKNOWN;
    }

    public static IkasanFlowComponent getEndpointForFlowElement(IkasanFlowComponent ikasanFlowComponent, IkasanFlow ikasanFlow) {
        IkasanFlowComponent endpointFlowElement = null ;
        if (ikasanFlowComponent.getType() != null) {
            switch (ikasanFlowComponent.getType()) {
                case SFTP_CONSUMER :
                    endpointFlowElement = new IkasanFlowComponent(SFTP_LOCATION, ikasanFlow);
                    break;
                case FTP_PRODUCER :
                case FTP_CONSUMER :
                    endpointFlowElement = new IkasanFlowComponent(FTP_LOCATION, ikasanFlow);
                    break;
                case JMS_PRODUCER :
                case JMS_PRODUCER_BASIC_AMQ:
                case SPRING_JMS_CONSUMER :
                case SPRING_JMS_CONSUMER_BASIC_AMQ:
                    String destinationName = ikasanFlowComponent.getDestinationName();
                    endpointFlowElement = new IkasanFlowComponent(CHANNEL, ikasanFlow, destinationName, destinationName);
                    break;
                case DB_PRODUCER :
                case DB_CONSUMER :
                    endpointFlowElement = new IkasanFlowComponent(DB, ikasanFlow);
                    break;
            }
        }
        return endpointFlowElement;
    }

    public IkasanComponentCategory getElementCategory() {
        return elementCategory;
    }

    public boolean isBespokeClass() {
        return bespokeClass;
    }

    public boolean isUsesBuilder() {
        return usesBuilder;
    }

    public String getAssociatedMethodName() {
        return associatedMethodName;
    }

    public IkasanComponentDependency getComponentDependency() {
        return componentDependency;
    }

    public boolean isJms() {
        boolean isJms = false;
        switch (this) {
            case JMS_PRODUCER:
            case JMS_PRODUCER_BASIC_AMQ:
            case SPRING_JMS_CONSUMER:
            case SPRING_JMS_CONSUMER_BASIC_AMQ:
                isJms = true;
                break;
        }
        return isJms;
    }
}
