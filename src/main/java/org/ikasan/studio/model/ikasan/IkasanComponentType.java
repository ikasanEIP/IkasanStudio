package org.ikasan.studio.model.ikasan;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ikasan.studio.StudioUtils;

import java.util.Map;
import java.util.TreeMap;

/**
 * Focuses on the ikasan technical details of a component i.e. type, properties etc
 *
 * No UI specific elements should be present in this class (@see org.ikasan.studio.ui.model.IkasanFlowUIComponent for that)
 */
public enum IkasanComponentType {
    MODULE(IkasanComponentCategory.MODULE, false, ""),
    FLOW(IkasanComponentCategory.FLOW, false, ""),
    BROKER(IkasanComponentCategory.BROKER, false, "broker"),
    DB_BROKER(IkasanComponentCategory.BROKER, false, "DbBroker"),
    DELAY_GENERATION_BROKER(IkasanComponentCategory.BROKER, false, "DelayGenerationBroker"),
    EXCEPTION_GENERATING_BROKER(IkasanComponentCategory.BROKER, false, "ExceptionGenerationgBroker"),
    SCHEDULE_RULE_CHECK_BROKER(IkasanComponentCategory.BROKER, false, "ScheduledRuleCheckBroker"),

    DB_CONSUMER(IkasanComponentCategory.CONSUMER, false, "DBConsumer"),
//    EVENT_DRIVEN_CONSUMER(IkasanComponentCategory.CONSUMER, false, "eventDrivenConsumer"),  cant seem to find this in ikasan code base.
    EVENT_GENERATING_CONSUMER(IkasanComponentCategory.CONSUMER, false, "eventGeneratingConsumer"),
    SCHEDULED_CONSUMER(IkasanComponentCategory.CONSUMER, false, "scheduledConsumer"),
    FTP_CONSUMER(IkasanComponentCategory.CONSUMER, false, "ftpConsumer"),
    JMS_CONSUMER(IkasanComponentCategory.CONSUMER, false, "jmsConsumer"),
    SPRING_JMS_CONSUMER(IkasanComponentCategory.CONSUMER, false, "jmsConsumer"),
    SFTP_CONSUMER(IkasanComponentCategory.CONSUMER, false, "sftpConsumer"),
    LOCAL_FILE_CONSUMER(IkasanComponentCategory.CONSUMER, false, "fileConsumer"),
    MONGO_CONSUMER(IkasanComponentCategory.CONSUMER, false, "mongoConsumer"),

    // @todo, maybe introduce the base class into this list, might conflict though when matching custom components.
    CUSTOM_CONVERTER(IkasanComponentCategory.CONVERTER, true, "Converter"),  // This will really be the implemented interface
    JSON_XML_CONVERTER(IkasanComponentCategory.CONVERTER, false, "JsonXmlConverter"),
    MAP_MESSAGE_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "MapMessageToObjectConverter"),
    MAP_MESSAGE_TO_PAYLOAD_CONVERTER(IkasanComponentCategory.CONVERTER, false, "MapMessageToPayloadConverter"),
    OBJECT_MESSAGE_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "ObjectMessageToObjectConverter"),
    OBJECT_MESSAGE_TO_XML_STRING_CONVERTER(IkasanComponentCategory.CONVERTER, false, "ObjectToXMLStringConverter"),
    OBJECT_TO_XML_CONVERTER(IkasanComponentCategory.CONVERTER, false, "ObjectToXmlStringConverter"),
    PAYLOAD_TO_MAP_CONVERTER(IkasanComponentCategory.CONVERTER, false, "PayloadToMapConverter"),
    TEXT_MESSAGE_TO_STRING_CONVERTER(IkasanComponentCategory.CONVERTER, false, "TextMessageToStringConverter"),
    THREAD_SAFE_XSLT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "ThreadSafeXsltConverter"),
    XML_BYTE_ARRAY_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "XmlByteArrayToObjectConverter"),
    XML_STRING_TO_OBJECT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "XmlStringToObjectConverter"),
    XML_TO_JSON_CONVERTER(IkasanComponentCategory.CONVERTER, false, "XmlJsonConverter"),
    XSLT_CONFIGURATION_PARAMETER_CONVERTER(IkasanComponentCategory.CONVERTER, false, "XsltConfigurationParameterConverter"),
    XSLT_CONVERTER(IkasanComponentCategory.CONVERTER, false, "XsltConverter"),

    MESSAGE_FILTER(IkasanComponentCategory.FILTER, false, "FilterInvokerConfiguration"),

    CHANNEL(IkasanComponentCategory.ENDPOINT, false, "messageChannel"),
    FTP_LOCATION(IkasanComponentCategory.ENDPOINT, false, "ftpLocation"),
    SFTP_LOCATION(IkasanComponentCategory.ENDPOINT, false, "sftpLocation"),
    DB(IkasanComponentCategory.ENDPOINT, false, "message-store"),

    LIST_SPLITTER(IkasanComponentCategory.SPLITTER, false, "listSplitter"),
    SPLITTER(IkasanComponentCategory.SPLITTER, false, "splitter"),

    TRANSLATOR(IkasanComponentCategory.TRANSLATER, false, "Translator"),

    DB_PRODUCER(IkasanComponentCategory.PRODUCER, false, "DBProducer"),
    DEV_NULL_PRODUCER(IkasanComponentCategory.PRODUCER, false, "devNullProducer"),
    EMAIL_PRODUCER(IkasanComponentCategory.PRODUCER, false, "emailProducer"),
    FTP_PRODUCER(IkasanComponentCategory.PRODUCER, false, "ftpProducer"),
    SFTP_PRODUCER(IkasanComponentCategory.PRODUCER, false, "sftpProducer"),
    JMS_PRODUCER(IkasanComponentCategory.PRODUCER, false, "JmsProducer"),
    LOG_PRODUCER(IkasanComponentCategory.PRODUCER, false, "logProducer"),

    SINGLE_RECIPIENT_ROUTER(IkasanComponentCategory.ROUTER, false, "SingleRecipientRouter"),
    MULTI_RECIPIENT_ROUTER(IkasanComponentCategory.ROUTER, false, "MultiRecipientRouter"),

    BESPOKE(IkasanComponentCategory.UNKNOWN, true, "bespoke"),
    UNKNOWN(IkasanComponentCategory.UNKNOWN, false, "unknown");

    private static final Logger log = Logger.getLogger(IkasanComponentType.class);
    public final String associatedMethodName;
    public final boolean bespokeClass;
    public final IkasanComponentCategory elementCategory;
    Map<String, IkasanComponentPropertyMeta> metadataMap;

    /**
     * Represents a flow element e.g. JMS Consumer, DB Consumer et
     * @param elementCategory e.g. CONSUMER, PRODUCER
     * @param bespokeClass name
     * @param associatedMethodName in the source code that can be used to identify this element, typically used by ComponentBuilder.
     */
    IkasanComponentType(IkasanComponentCategory elementCategory, boolean bespokeClass, String associatedMethodName) {
        this.bespokeClass = bespokeClass;
        this.associatedMethodName = associatedMethodName;
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
            if (entry.getValue().isMandatory()) {
                IkasanComponentProperty newIkasanComponentProperty = new IkasanComponentProperty(entry.getValue());
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
            Object description;
            switch (ikasanFlowComponent.getType()) {
                case SFTP_CONSUMER :
                    endpointFlowElement = new IkasanFlowComponent(SFTP_LOCATION, ikasanFlow);
//                    endpointFlowElement.setTypeAndViewHandler(SFTP_LOCATION);
                    description = ikasanFlowComponent.getProperties().get("SftpRemoteHost");
//                    endpointFlowElement.setDescription(description != null ? description.toString() : "");
                    break;
                case FTP_PRODUCER :
                case FTP_CONSUMER :
                    endpointFlowElement = new IkasanFlowComponent(FTP_LOCATION, ikasanFlow);
//                    endpointFlowElement.setTypeAndViewHandler(FTP_LOCATION);
                    description = ikasanFlowComponent.getProperties().get("RemoteHost");
//                    endpointFlowElement.setDescription(description != null ? description.toString() : "");
                    break;
                case JMS_PRODUCER :
                case JMS_CONSUMER :
                    endpointFlowElement = new IkasanFlowComponent(CHANNEL, ikasanFlow);
//                    endpointFlowElement.setTypeAndViewHandler(CHANNEL);
                    description = ikasanFlowComponent.getProperties().get("DestinationJndiName");
//                    endpointFlowElement.setDescription(description != null ? description.toString() : "");
                    break;
                case DB_PRODUCER :
                case DB_CONSUMER :
                    endpointFlowElement = new IkasanFlowComponent(DB, ikasanFlow);
//                    endpointFlowElement.setTypeAndViewHandler(DB);
                    description = ikasanFlowComponent.getProperties().get("setConfiguredResourceId");
//                    endpointFlowElement.setDescription(description != null ? description.toString() : "");
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
}
