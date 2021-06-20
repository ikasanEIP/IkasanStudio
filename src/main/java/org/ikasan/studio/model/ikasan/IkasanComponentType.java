package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ikasan.studio.StudioUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Focuses on the ikasan technical details of a component i.e. type, properties etc
 *
 * No UI specific elements should be present in this class (@see org.ikasan.studio.ui.model.IkasanFlowUIComponent for that)
 */
public enum IkasanComponentType implements Serializable {
    MODULE(IkasanComponentCategory.MODULE, false, "", IkasanComponentDependency.BASIC),
    FLOW(IkasanComponentCategory.FLOW, false, "",IkasanComponentDependency.NONE),
    BROKER(IkasanComponentCategory.BROKER, false, "broker",IkasanComponentDependency.NONE),
    DB_BROKER(IkasanComponentCategory.BROKER, false, "DbBroker",IkasanComponentDependency.NONE),
    DELAY_GENERATION_BROKER(IkasanComponentCategory.BROKER, false, "DelayGenerationBroker",IkasanComponentDependency.NONE),
    EXCEPTION_GENERATING_BROKER(IkasanComponentCategory.BROKER, false, "ExceptionGenerationgBroker",IkasanComponentDependency.NONE),
    SCHEDULE_RULE_CHECK_BROKER(IkasanComponentCategory.BROKER, false, "ScheduledRuleCheckBroker",IkasanComponentDependency.NONE),

    DB_CONSUMER(IkasanComponentCategory.CONSUMER, false, "DBConsumer",IkasanComponentDependency.NONE),
//    EVENT_DRIVEN_CONSUMER(IkasanComponentCategory.CONSUMER, false, "eventDrivenConsumer",IkasanComponentDependency.NONE),  cant seem to find this in ikasan code base.
    EVENT_GENERATING_CONSUMER(IkasanComponentCategory.CONSUMER, false, "eventGeneratingConsumer",IkasanComponentDependency.NONE),
    SCHEDULED_CONSUMER(IkasanComponentCategory.CONSUMER, false, "scheduledConsumer",IkasanComponentDependency.NONE),
    FTP_CONSUMER(IkasanComponentCategory.CONSUMER, false, "ftpConsumer",IkasanComponentDependency.NONE),
//    JMS_CONSUMER(IkasanComponentCategory.CONSUMER, false, "jmsConsumer",IkasanComponentDependency.NONE),
    SPRING_JMS_CONSUMER(IkasanComponentCategory.CONSUMER, false, "jmsConsumer", IkasanComponentDependency.JMS),
    SPRING_JMS_CONSUMER_BASIC_AMQ(IkasanComponentCategory.CONSUMER, false, "jmsConsumer", IkasanComponentDependency.JMS),
    SFTP_CONSUMER(IkasanComponentCategory.CONSUMER, false, "sftpConsumer",IkasanComponentDependency.NONE),
    LOCAL_FILE_CONSUMER(IkasanComponentCategory.CONSUMER, false, "fileConsumer",IkasanComponentDependency.NONE),
    MONGO_CONSUMER(IkasanComponentCategory.CONSUMER, false, "mongoConsumer",IkasanComponentDependency.NONE),

    // @todo, maybe introduce the base class into this list, might conflict though when matching custom components.
    CUSTOM_CONVERTER(IkasanComponentCategory.CONVERTER, true, "Converter",IkasanComponentDependency.NONE),  // This will really be the implemented interface
    JSON_XML_CONVERTER(IkasanComponentCategory.CONVERTER, false, "JsonXmlConverter",IkasanComponentDependency.NONE),
    MAP_MESSAGE_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "MapMessageToObjectConverter",IkasanComponentDependency.NONE),
    MAP_MESSAGE_TO_PAYLOAD_CONVERTER(IkasanComponentCategory.CONVERTER, false, "MapMessageToPayloadConverter",IkasanComponentDependency.NONE),
    OBJECT_MESSAGE_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "ObjectMessageToObjectConverter",IkasanComponentDependency.NONE),
    OBJECT_MESSAGE_TO_XML_STRING_CONVERTER(IkasanComponentCategory.CONVERTER, false, "objectToXmlStringConverter",IkasanComponentDependency.NONE),
//    OBJECT_TO_XML_CONVERTER(IkasanComponentCategory.CONVERTER, false, "ObjectToXmlStringConverter",IkasanComponentDependency.NONE),
    PAYLOAD_TO_MAP_CONVERTER(IkasanComponentCategory.CONVERTER, false, "PayloadToMapConverter",IkasanComponentDependency.NONE),
    TEXT_MESSAGE_TO_STRING_CONVERTER(IkasanComponentCategory.CONVERTER, false, "TextMessageToStringConverter",IkasanComponentDependency.NONE),
    THREAD_SAFE_XSLT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "ThreadSafeXsltConverter",IkasanComponentDependency.NONE),
    XML_BYTE_ARRAY_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "XmlByteArrayToObjectConverter",IkasanComponentDependency.NONE),
    XML_STRING_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "xmlStringToObjectConverter",IkasanComponentDependency.NONE),
    XML_TO_JSON_CONVERTER(IkasanComponentCategory.CONVERTER, false, "XmlJsonConverter",IkasanComponentDependency.NONE),
    XSLT_CONFIGURATION_PARAMETER_CONVERTER(IkasanComponentCategory.CONVERTER, false, "XsltConfigurationParameterConverter",IkasanComponentDependency.NONE),
    XSLT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "XsltConverter",IkasanComponentDependency.NONE),

    MESSAGE_FILTER(IkasanComponentCategory.FILTER, false, "FilterInvokerConfiguration",IkasanComponentDependency.NONE),

    CHANNEL(IkasanComponentCategory.ENDPOINT, false, "messageChannel",IkasanComponentDependency.NONE),
    FTP_LOCATION(IkasanComponentCategory.ENDPOINT, false, "ftpLocation",IkasanComponentDependency.NONE),
    SFTP_LOCATION(IkasanComponentCategory.ENDPOINT, false, "sftpLocation",IkasanComponentDependency.NONE),
    DB(IkasanComponentCategory.ENDPOINT, false, "message-store",IkasanComponentDependency.NONE),

    LIST_SPLITTER(IkasanComponentCategory.SPLITTER, false, "listSplitter",IkasanComponentDependency.NONE),
    SPLITTER(IkasanComponentCategory.SPLITTER, false, "splitter",IkasanComponentDependency.NONE),

    TRANSLATOR(IkasanComponentCategory.TRANSLATER, false, "Translator",IkasanComponentDependency.NONE),

    DB_PRODUCER(IkasanComponentCategory.PRODUCER, false, "DBProducer",IkasanComponentDependency.NONE),
    DEV_NULL_PRODUCER(IkasanComponentCategory.PRODUCER, false, "devNullProducer",IkasanComponentDependency.NONE),
    EMAIL_PRODUCER(IkasanComponentCategory.PRODUCER, false, "emailProducer",IkasanComponentDependency.NONE),
    FTP_PRODUCER(IkasanComponentCategory.PRODUCER, false, "ftpProducer",IkasanComponentDependency.NONE),
    SFTP_PRODUCER(IkasanComponentCategory.PRODUCER, false, "sftpProducer",IkasanComponentDependency.NONE),
    JMS_PRODUCER(IkasanComponentCategory.PRODUCER, false, "jmsProducer", IkasanComponentDependency.JMS),
    JMS_PRODUCER_BASIC_AMQ(IkasanComponentCategory.PRODUCER, false, "jmsProducer", IkasanComponentDependency.JMS),
    LOG_PRODUCER(IkasanComponentCategory.PRODUCER, false, "logProducer",IkasanComponentDependency.NONE),

    SINGLE_RECIPIENT_ROUTER(IkasanComponentCategory.ROUTER, false, "SingleRecipientRouter",IkasanComponentDependency.NONE),
    MULTI_RECIPIENT_ROUTER(IkasanComponentCategory.ROUTER, false, "MultiRecipientRouter",IkasanComponentDependency.NONE),

    BESPOKE(IkasanComponentCategory.UNKNOWN, true, "bespoke",IkasanComponentDependency.NONE),
    UNKNOWN(IkasanComponentCategory.UNKNOWN, false, "unknown",IkasanComponentDependency.NONE);

    private static final Logger LOG = Logger.getLogger(IkasanComponentType.class);
    public final String associatedMethodName;
    public final boolean bespokeClass;
    public final IkasanComponentCategory elementCategory;
    @JsonIgnore
    public final IkasanComponentDependency componentDependency;
    @JsonIgnore
    Map<String, IkasanComponentPropertyMeta> metadataMap;

    /**
     * Represents a flow element e.g. JMS Consumer, DB Consumer et
     * @param elementCategory e.g. CONSUMER, PRODUCER
     * @param bespokeClass name
     * @param associatedMethodName in the source code that can be used to identify this element, typically used by ComponentBuilder.
     * @param componentDependency the pom jars associated with this component.
     */
    IkasanComponentType(IkasanComponentCategory elementCategory, boolean bespokeClass, String associatedMethodName, IkasanComponentDependency componentDependency) {
        this.bespokeClass = bespokeClass;
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
    public Map<String, IkasanComponentProperty> getMandatoryProperties() {
        Map<String, IkasanComponentProperty> mandatoryProperties = new TreeMap<>();
        for (Map.Entry<String, IkasanComponentPropertyMeta> entry : metadataMap.entrySet()) {
            if (entry.getValue().isMandatory()) {
                mandatoryProperties.put(entry.getKey(), new IkasanComponentProperty(entry.getValue()));
            }
        }
        return mandatoryProperties;
    }

    public Map<String, IkasanComponentPropertyMeta> getMetadataMap() {
        return metadataMap;
    }
    public IkasanComponentPropertyMeta getMetadata(String propertyName) {
        return metadataMap.get(propertyName);
    }

    public IkasanComponentPropertyMeta getMetaDataForPropertyName(final String propertyName) {
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

    public static IkasanComponentType parseCategoryType(String methodName) {
        if (methodName != null) {
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

    public String getAssociatedMethodName() {
        return associatedMethodName;
    }

    public IkasanComponentDependency getComponentDependency() {
        return componentDependency;
    }

    public boolean isJms() {
        boolean isJms = false;
        if (this != null) {
            switch (this) {
                case JMS_PRODUCER:
                case JMS_PRODUCER_BASIC_AMQ:
                case SPRING_JMS_CONSUMER:
                case SPRING_JMS_CONSUMER_BASIC_AMQ:
                    isJms = true;
                    break;
            }
        }
        return isJms;
    }
}
