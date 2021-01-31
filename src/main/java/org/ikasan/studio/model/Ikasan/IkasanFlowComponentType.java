package org.ikasan.studio.model.Ikasan;

import org.apache.commons.lang3.StringUtils;
import org.ikasan.studio.StudioUtils;

import java.util.Map;
import java.util.TreeMap;

/**
 * Focuses on the Ikasan technical details of a component i.e. type, properties etc
 *
 * No UI specific elements should be present in this class (@see org.ikasan.studio.ui.model.IkasanFlowUIComponent for that)
 */
public enum IkasanFlowComponentType {

    BROKER(IkasanFlowComponentCategory.BROKER, "broker"),
    DB_BROKER(IkasanFlowComponentCategory.BROKER, "DbBroker"),
    DELAY_GENERATION_BROKER(IkasanFlowComponentCategory.BROKER, "DelayGenerationBroker"),
    EXCEPTION_GENERATING_BROKER(IkasanFlowComponentCategory.BROKER, "ExceptionGenerationgBroker"),
    SCHEDULE_RULE_CHECK_BROKER(IkasanFlowComponentCategory.BROKER, "ScheduledRuleCheckBroker"),

    DB_CONSUMER(IkasanFlowComponentCategory.CONSUMER, "DBConsumer"),
    EVENT_DRIVEN_CONSUMER(IkasanFlowComponentCategory.CONSUMER, "eventDrivenConsumer"),
    EVENT_GENERATING_CONSUMER(IkasanFlowComponentCategory.CONSUMER, "eventGeneratingConsumer"),
    SCHEDULED_CONSUMER(IkasanFlowComponentCategory.CONSUMER, "scheduledConsumer"),
    FTP_CONSUMER(IkasanFlowComponentCategory.CONSUMER, "ftpConsumer"),
    JMS_CONSUMER(IkasanFlowComponentCategory.CONSUMER, "jmsConsumer"),
    SPRING_JMS_CONSUMER(IkasanFlowComponentCategory.CONSUMER, "jmsConsumer"),
    SFTP_CONSUMER(IkasanFlowComponentCategory.CONSUMER, "sftpConsumer"),
    LOCAL_FILE_CONSUMER(IkasanFlowComponentCategory.CONSUMER, "fileConsumer"),
    MONGO_CONSUMER(IkasanFlowComponentCategory.CONSUMER, "mongoConsumer"),

    JSON_XML_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "JsonXmlConverter"),
    MAP_MESSAGE_TO_OBJECT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "MapMessageToObjectConverter"),
    MAP_MESSAGE_TO_PAYLOAD_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "MapMessageToPayloadConverter"),
    OBJECT_MESSAGE_TO_OBJECT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "ObjectMessageToObjectConverter"),
    OBJECT_MESSAGE_TO_XML_STRING_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "ObjectToXMLStringConverter"),
    OBJECT_TO_XML_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "ObjectToXmlStringConverter"),
    PAYLOAD_TO_MAP_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "PayloadToMapConverter"),
    TEXT_MESSAGE_TO_STRING_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "TextMessageToStringConverter"),
    THREAD_SAFE_XSLT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "ThreadSafeXsltConverter"),
    XML_BYTE_ARRAY_TO_OBJECT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "XmlByteArrayToObjectConverter"),
    XML_STRING_TO_OBJECT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "XmlStringToObjectConverter"),
    XML_TO_JSON_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "XmlJsonConverter"),
    XSLT_CONFIGURATION_PARAMETER_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "XsltConfigurationParameterConverter"),
    XSLT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, "XsltConverter"),

    MESSAGE_FILTER(IkasanFlowComponentCategory.FILTER, "FilterInvokerConfiguration"),

    CHANNEL(IkasanFlowComponentCategory.ENDPOINT, "messageChannel"),
    FTP_LOCATION(IkasanFlowComponentCategory.ENDPOINT, "ftpLocation"),
    SFTP_LOCATION(IkasanFlowComponentCategory.ENDPOINT, "sftpLocation"),
    DB(IkasanFlowComponentCategory.ENDPOINT, "message-store"),

    LIST_SPLITTER(IkasanFlowComponentCategory.SPLITTER, "listSplitter"),
    SPLITTER(IkasanFlowComponentCategory.SPLITTER, "splitter"),

    TRANSLATOR(IkasanFlowComponentCategory.TRANSLATER, "Translator"),

    DB_PRODUCER(IkasanFlowComponentCategory.PRODUCER, "DBProducer"),
    DEV_NULL_PRODUCER(IkasanFlowComponentCategory.PRODUCER, "devNullProducer"),
    EMAIL_PRODUCER(IkasanFlowComponentCategory.PRODUCER, "emailProducer"),
    FTP_PRODUCER(IkasanFlowComponentCategory.PRODUCER, "ftpProducer"),
    SFTP_PRODUCER(IkasanFlowComponentCategory.PRODUCER, "sftpProducer"),
    JMS_PRODUCER(IkasanFlowComponentCategory.PRODUCER, "JmsProducer"),
    LOG_PRODUCER(IkasanFlowComponentCategory.PRODUCER, "logProducer"),

    SINGLE_RECIPIENT_ROUTER(IkasanFlowComponentCategory.ROUTER, "SingleRecipientRouter"),
    MULTI_RECIPIENT_ROUTER(IkasanFlowComponentCategory.ROUTER, "MultiRecipientRouter"),

    BESPOKE(IkasanFlowComponentCategory.UNKNOWN, "bespoke"),
    UNKNOWN(IkasanFlowComponentCategory.UNKNOWN, "unknown");

    public final String associatedMethodName;
    public final IkasanFlowComponentCategory elementCategory;
    Map<String, IkasanComponentPropertyMeta>  properties;

    /**
     * Represents a flow element e.g. JMS Consumer, DB Consumer et
     * @param elementCategory e.g. CONSUMER, PRODUCER
     * @param associatedMethodName in the source code that can be used to identify this element, typically used by ComponentBuilder.
     */
    IkasanFlowComponentType(IkasanFlowComponentCategory elementCategory, String associatedMethodName) {
        this.associatedMethodName = associatedMethodName;
        this.elementCategory = elementCategory;

        // @todo having this in the constructor is too risky, and exception even if caught will prevent the plugin from working.
        // @todo may be more efficient to split into mandatory and optional properties
        properties = StudioUtils.readIkasanComponentProperties(this.toString());
    }

    /**
     * Get a list of the mandatory properties for this component.
     * @return
     */
    public Map<String, IkasanComponentProperty> getMandatoryProperties() {
        Map<String, IkasanComponentProperty> mandatoryProperties = new TreeMap<>();
        for (Map.Entry<String, IkasanComponentPropertyMeta> entry : properties.entrySet()) {
            if (entry.getValue().isMandatory()) {
                mandatoryProperties.put(entry.getKey(), new IkasanComponentProperty(entry.getValue()));
            }
        }
        return mandatoryProperties;
    }

    public IkasanComponentPropertyMeta getMetaDataForPropertyName(final String propertyName) {
        return properties.get(propertyName);
    }

    public Map<String, IkasanComponentPropertyMeta> getProperties() {
        return properties;
    }

    public static IkasanFlowComponentType parseMethodName(String methodName) {
        if (methodName != null) {
            for (IkasanFlowComponentType name : IkasanFlowComponentType.values()) {
                if (StringUtils.containsIgnoreCase(methodName, name.associatedMethodName)) {
                    return name;
                }
            }
        }
        return UNKNOWN;
    }

    public static IkasanFlowComponentType parseCategoryType(String methodName) {
        if (methodName != null) {
            for (IkasanFlowComponentType name : IkasanFlowComponentType.values()) {
                if (StringUtils.containsIgnoreCase(methodName, name.elementCategory.associatedMethodName)) {
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

    public IkasanFlowComponentCategory getElementCategory() {
        return elementCategory;
    }

    public String getAssociatedMethodName() {
        return associatedMethodName;
    }
}
