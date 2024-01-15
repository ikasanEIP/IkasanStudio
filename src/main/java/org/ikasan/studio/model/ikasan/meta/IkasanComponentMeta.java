package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.model.ikasan.instance.IkasanComponentProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Focuses on the ikasan technical details of a component i.e. type, properties etc
 * No UI specific elements should be present in this class (@see org.ikasan.studio.ui.model.IkasanFlowUIComponent for that)
 */
public enum IkasanComponentMeta implements Serializable {
    MODULE(IkasanComponentCategory.MODULE, false, true, "org.ikasan.spec.module.Module"),
    FLOW(IkasanComponentCategory.FLOW, false, true, "org.ikasan.spec.flow.Flow"),
    BROKER(IkasanComponentCategory.BROKER, false, true, "broker"),
    DB_BROKER(IkasanComponentCategory.BROKER, false, true, "DbBroker"),
    DELAY_GENERATION_BROKER(IkasanComponentCategory.BROKER, false, true, "DelayGenerationBroker"),
    EXCEPTION_GENERATING_BROKER(IkasanComponentCategory.BROKER, false, true, "ExceptionGenerationgBroker"),
    SCHEDULE_RULE_CHECK_BROKER(IkasanComponentCategory.BROKER, false, true, "ScheduledRuleCheckBroker"),

    DB_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "DBConsumer"),
//    EVENT_DRIVEN_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "eventDrivenConsumer", IkasanComponentDependency.NONE),  cant seem to find this in ikasan code base.
    EVENT_GENERATING_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "eventGeneratingConsumer"),
    SCHEDULED_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "scheduledConsumer"),
    FTP_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "ftpConsumer"),
//    JMS_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "jmsConsumer", IkasanComponentDependency.NONE),
    SPRING_JMS_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "jmsConsumer"),
    SPRING_JMS_CONSUMER_BASIC_AMQ(IkasanComponentCategory.CONSUMER, false, true, "jmsConsumer"),
    SFTP_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "sftpConsumer"),
    LOCAL_FILE_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "fileConsumer"),
    MONGO_CONSUMER(IkasanComponentCategory.CONSUMER, false, true, "mongoConsumer"),

    // @todo, maybe introduce the base class into this list, might conflict though when matching custom components.
    CUSTOM_CONVERTER(IkasanComponentCategory.CONVERTER, true, false, "Converter"),  // This will really be the implemented interface
    JSON_XML_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "JsonXmlConverter"),
    MAP_MESSAGE_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "MapMessageToObjectConverter"),
    MAP_MESSAGE_TO_PAYLOAD_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "MapMessageToPayloadConverter"),
    OBJECT_MESSAGE_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "org.ikasan.component.converter.jms.ObjectMessageToObjectConverter"),
    OBJECT_MESSAGE_TO_XML_STRING_CONVERTER(IkasanComponentCategory.CONVERTER, false, true, "objectToXmlStringConverter"),
//    OBJECT_TO_XML_CONVERTER(IkasanComponentCategory.CONVERTER, false, "ObjectToXmlStringConverter", IkasanComponentDependency.CONVERTER),
    PAYLOAD_TO_MAP_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "PayloadToMapConverter"),
    TEXT_MESSAGE_TO_STRING_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "TextMessageToStringConverter"),
    THREAD_SAFE_XSLT_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "ThreadSafeXsltConverter"),
    XML_BYTE_ARRAY_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "XmlByteArrayToObjectConverter"),
    XML_STRING_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, true, "xmlStringToObjectConverter"),
    XML_TO_JSON_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "XmlJsonConverter"),
    XSLT_CONFIGURATION_PARAMETER_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "XsltConfigurationParameterConverter"),
    XSLT_CONVERTER(IkasanComponentCategory.CONVERTER, false, false, "XsltConverter"),

    MESSAGE_FILTER(IkasanComponentCategory.FILTER, true, false, "FilterInvokerConfiguration"),

    CHANNEL(IkasanComponentCategory.ENDPOINT, false, true, "messageChannel"),
    FTP_LOCATION(IkasanComponentCategory.ENDPOINT, false, true, "ftpLocation"),
    SFTP_LOCATION(IkasanComponentCategory.ENDPOINT, false, true, "sftpLocation"),
    DB(IkasanComponentCategory.ENDPOINT, false, true, "message-store"),

    EXCEPTION_RESOLVER(IkasanComponentCategory.EXCEPTION_RESOLVER, false, true, "getExceptionResolverBuilder"),
    ON_EXCEPTION(IkasanComponentCategory.EXCEPTION_RESOLVER, false, true, "addExceptionToAction"),

    LIST_SPLITTER(IkasanComponentCategory.SPLITTER, false, true, "listSplitter"),
    SPLITTER(IkasanComponentCategory.SPLITTER, false, true, "splitter"),

    TRANSLATOR(IkasanComponentCategory.TRANSLATER, false, true, "Translator"),

    DB_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "DBProducer"),
    DEV_NULL_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "devNullProducer"),
    EMAIL_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "emailProducer"),
    FTP_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "ftpProducer"),
    SFTP_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "sftpProducer"),
    JMS_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "jmsProducer"),
    JMS_PRODUCER_BASIC_AMQ(IkasanComponentCategory.PRODUCER, false, true, "jmsProducer"),
    LOG_PRODUCER(IkasanComponentCategory.PRODUCER, false, true, "logProducer"),

    SINGLE_RECIPIENT_ROUTER(IkasanComponentCategory.ROUTER, false, true, "SingleRecipientRouter"),
    MULTI_RECIPIENT_ROUTER(IkasanComponentCategory.ROUTER, false, true, "MultiRecipientRouter"),

    BESPOKE(IkasanComponentCategory.UNKNOWN, true, false, "bespoke"),
    UNKNOWN(IkasanComponentCategory.UNKNOWN, false, false, "unknown");

    // --- Population ----
    // Remember, to make the below available to constructor, they must be an instance and not static
    public final String componentType;
    public final boolean bespokeClass;
    //@todo move this to component copnfig.csv
    public final boolean usesBuilder;
    public final IkasanComponentCategory elementCategory;
//    @JsonIgnore
//    public final IkasanComponentDependency componentDependency;
    @JsonIgnore
    final
    Map<String, IkasanComponentPropertyMeta> metadataMap;

    /**
     * Represents a flow element e.g. JMS Consumer, DB Consumer et
     * @param elementCategory e.g. CONSUMER, PRODUCER
     * @param bespokeClass is the class generated bespoke i.e. generated outside the Studio package structure
     * @param usesBuilder does the component use the component builder (some components need to be instantiated with 'new')
     * @param componentType in the source code that can be used to identify this element, typically used by ComponentBuilder.
     */
    IkasanComponentMeta(IkasanComponentCategory elementCategory, boolean bespokeClass, boolean usesBuilder, String componentType) {
        this.bespokeClass = bespokeClass;
        this.usesBuilder = usesBuilder;
        this.componentType = componentType;
        this.elementCategory = elementCategory;

        // @todo having this in the constructor is too risky, and exception even if caught will prevent the plugin from working.
        // @todo may be more efficient to split into mandatory and optional properties
        metadataMap = StudioUtils.readIkasanComponentProperties(this.toString());
    }

    /**
     * Get a list of the mandatory properties for this component.
     * @return A map of the mandatory properties for this component
     */
    public Map<String, IkasanComponentProperty> getMandatoryProperties() {
        Map<String, IkasanComponentProperty> mandatoryProperties = new TreeMap<>();
        for (Map.Entry<String, IkasanComponentPropertyMeta> entry : metadataMap.entrySet()) {
//            if (!entry.getValue().subProperties && entry.getValue().isMandatory()) {
            if (entry.getValue().isMandatory()) {
                mandatoryProperties.put(entry.getKey(), new IkasanComponentProperty(entry.getValue()));
            }
        }
        return mandatoryProperties;
    }

    public Map<String, IkasanComponentPropertyMeta> getMetadataMap() {
        return metadataMap;
    }

//    public IkasanComponentPropertyMeta getMetadata(String propertyName) {
//        // If we just supply the propertyName, assume it is the simple type i.e. 1 group, 1 constructor
//        return metadataMap.get(new IkasanComponentPropertyMetaKey(propertyName));
//    }

    public List<IkasanComponentPropertyMeta> getMetadataList(String propertyName) {
        List<IkasanComponentPropertyMeta> metadataList = new ArrayList<>();
        IkasanComponentPropertyMeta meta = metadataMap.get(propertyName);
//        if (meta.hasSubProperties()) {
//            metadataList = new ArrayList<>(meta.getSubProperties().values());
//        } else {
            metadataList.add(meta);
//        }
        return metadataList;
    }

    public IkasanComponentPropertyMeta getMetadata(String propertyName) {
        return metadataMap.get(propertyName);
    }

//    public IkasanComponentPropertyMeta getMetaDataForPropertyName(final String propertyName) {
//        // If we just supply the propertyName, assume it is the simple type i.e. 1 group, 1 constructor
//        return metadataMap.get(new IkasanComponentPropertyMetaKey(propertyName));
//    }

    /**
     * Return the list of properties for the given component
     * @return the list of properties
     */
    public List<String> getPropertyNames() {
        return metadataMap.keySet().stream()
                .distinct().collect(Collectors.toList());

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
        return metadataMap.containsKey(propertyName);
//        return metadataMap.keySet().stream()
//                .anyMatch(x->x.getPropertyName().equals(propertyName));
//        for(IkasanComponentPropertyMetaKey key : metadataMap.keySet()) {
//            if (key.getPropertyName().equals(propertyName)) {
//                return true;
//            }
//        }
//        return false;
    }

    public IkasanComponentPropertyMeta getMetaDataForPropertyName(final String propertyName) {
        return metadataMap.get(propertyName);
    }

//    public static IkasanComponentTypeMeta parseMethodName(String methodName) {
//        if (methodName != null) {
//            for (IkasanComponentTypeMeta name : IkasanComponentTypeMeta.values()) {
//                if (name.componentType != null &&
//                    !name.componentType.isEmpty() &&
//                    StringUtils.equalsIgnoreCase(methodName, name.componentType)) {
//                    return name;
//                }
//            }
//        }
//        return UNKNOWN;
//    }

//    public static FlowElement getEndpointForFlowElement(FlowElement ikasanFlowComponent, Flow ikasanFlow) {
//        FlowElement endpointFlowElement = null ;
//        if (ikasanFlowComponent.getIkasanComponentTypeMeta() != null) {
//            switch (ikasanFlowComponent.getIkasanComponentTypeMeta()) {
//                case SFTP_CONSUMER :
//                    endpointFlowElement = new FlowElement(SFTP_LOCATION, ikasanFlow);
//                    break;
//                case FTP_PRODUCER :
//                case FTP_CONSUMER :
//                    endpointFlowElement = new FlowElement(FTP_LOCATION, ikasanFlow);
//                    break;
//                case JMS_PRODUCER :
//                case JMS_PRODUCER_BASIC_AMQ:
//                case SPRING_JMS_CONSUMER :
//                case SPRING_JMS_CONSUMER_BASIC_AMQ:
//                    String destinationName = ikasanFlowComponent.getDestinationName();
//                    endpointFlowElement = new FlowElement(CHANNEL, ikasanFlow, destinationName, destinationName);
//                    break;
//                case DB_PRODUCER :
//                case DB_CONSUMER :
//                    endpointFlowElement = new FlowElement(DB, ikasanFlow);
//                    break;
//            }
//        }
//        return endpointFlowElement;
//    }

    public IkasanComponentCategory getElementCategory() {
        return elementCategory;
    }

    public boolean isBespokeClass() {
        return bespokeClass;
    }

//    public boolean isUsesBuilder() {
//        return usesBuilder;
//    }

    public String getComponentType() {
        return componentType;
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
