package org.ikasan.studio.model.Ikasan;

import org.apache.commons.lang3.StringUtils;

/**
 * The different elements (components) that might be in a flow
 */
public enum IkasanFlowElementType {
    BROKER(IkasanFlowElementCategory.BROKER, "broker"),
    DB_BROKER(IkasanFlowElementCategory.BROKER, "DbBroker"),
    DELAY_GENERATION_BROKER(IkasanFlowElementCategory.BROKER, "DelayGenerationBroker"),
    EXCEPTION_GENERATING_BROKER(IkasanFlowElementCategory.BROKER, "ExceptionGenerationgBroker"),
    SCHEDULE_RULE_CHECK_BROKER(IkasanFlowElementCategory.BROKER, "ScheduledRuleCheckBroker"),

    DB_CONSUMER(IkasanFlowElementCategory.CONSUMER, "DBConsumer"),
    EVENT_DRIVEN_CONSUMER(IkasanFlowElementCategory.CONSUMER, "eventDrivenConsumer"),
    EVENT_GENERATING_CONSUMER(IkasanFlowElementCategory.CONSUMER, "eventGeneratingConsumer"),
    SCHEDULED_CONSUMER(IkasanFlowElementCategory.CONSUMER, "scheduledConsumer"),
    FTP_CONSUMER(IkasanFlowElementCategory.CONSUMER, "ftpConsumer"),
    JMS_CONSUMER(IkasanFlowElementCategory.CONSUMER, "jmsConsumer"),
    SPRING_JMS_CONSUMER(IkasanFlowElementCategory.CONSUMER, "jmsConsumer"),
    SFTP_CONSUMER(IkasanFlowElementCategory.CONSUMER, "sftpConsumer"),
    LOCAL_FILE_CONSUMER(IkasanFlowElementCategory.CONSUMER, "fileConsumer"),
    MONGO_CONSUMER(IkasanFlowElementCategory.CONSUMER, "mongoConsumer"),

    JSON_XML_CONVERTER(IkasanFlowElementCategory.CONVERTER, "JsonXmlConverter"),
    MAP_MESSAGE_TO_OBJECT_CONVERTER(IkasanFlowElementCategory.CONVERTER, "MapMessageToObjectConverter"),
    MAP_MESSAGE_TO_PAYLOAD_CONVERTER(IkasanFlowElementCategory.CONVERTER, "MapMessageToPayloadConverter"),
    OBJECT_MESSAGE_TO_OBJECT_CONVERTER(IkasanFlowElementCategory.CONVERTER, "ObjectMessageToObjectConverter"),
    OBJECT_MESSAGE_TO_XML_STRING_CONVERTER(IkasanFlowElementCategory.CONVERTER, "ObjectToXMLStringConverter"),
    OBJECT_TO_XML_CONVERTER(IkasanFlowElementCategory.CONVERTER, "ObjectToXmlStringConverter"),
    PAYLOAD_TO_MAP_CONVERTER(IkasanFlowElementCategory.CONVERTER, "PayloadToMapConverter"),
    TEXT_MESSAGE_TO_STRING_CONVERTER(IkasanFlowElementCategory.CONVERTER, "TextMessageToStringConverter"),
    THREAD_SAFE_XSLT_CONVERTER(IkasanFlowElementCategory.CONVERTER, "ThreadSafeXsltConverter"),
    XML_BYTE_ARRAY_TO_OBJECT_CONVERTER(IkasanFlowElementCategory.CONVERTER, "XmlByteArrayToObjectConverter"),
    XML_STRING_TO_OBJECT_CONVERTER(IkasanFlowElementCategory.CONVERTER, "XmlStringToObjectConverter"),
    XML_TO_JSON_CONVERTER(IkasanFlowElementCategory.CONVERTER, "XmlJsonConverter"),
    XSLT_CONFIGURATION_PARAMETER_CONVERTER(IkasanFlowElementCategory.CONVERTER, "XsltConfigurationParameterConverter"),
    XSLT_CONVERTER(IkasanFlowElementCategory.CONVERTER, "XsltConverter"),

    MESSAGE_FILTER(IkasanFlowElementCategory.FILTER, "FilterInvokerConfiguration"),

    CHANNEL(IkasanFlowElementCategory.ENDPOINT, "messageChannel"),
    FTP_LOCATION(IkasanFlowElementCategory.ENDPOINT, "ftpLocation"),
    SFTP_LOCATION(IkasanFlowElementCategory.ENDPOINT, "sftpLocation"),
    DB(IkasanFlowElementCategory.ENDPOINT, "message-store"),

    LIST_SPLITTER(IkasanFlowElementCategory.SPLITTER, "listSplitter"),
    SPLITTER(IkasanFlowElementCategory.SPLITTER, "splitter"),

    TRANSLATOR(IkasanFlowElementCategory.TRANSLATER, "Translator"),

    DB_PRODUCER(IkasanFlowElementCategory.PRODUCER, "DBProducer"),
    DEV_NULL_PRODUCER(IkasanFlowElementCategory.PRODUCER, "devNullProducer"),
    EMAIL_PRODUCER(IkasanFlowElementCategory.PRODUCER, "emailProducer"),
    FTP_PRODUCER(IkasanFlowElementCategory.PRODUCER, "ftpProducer"),
    SFTP_PRODUCER(IkasanFlowElementCategory.PRODUCER, "sftpProducer"),
    JMS_PRODUCER(IkasanFlowElementCategory.PRODUCER, "JmsProducer"),
    LOG_PRODUCER(IkasanFlowElementCategory.PRODUCER, "logProducer"),

    SINGLE_RECIPIENT_ROUTER(IkasanFlowElementCategory.ROUTER, "SingleRecipientRouter"),
    MULTI_RECIPIENT_ROUTER(IkasanFlowElementCategory.ROUTER, "MultiRecipientRouter"),

    BESPOKE(IkasanFlowElementCategory.UNKNOWN, "bespoke"),
    UNKNOWN(IkasanFlowElementCategory.UNKNOWN, "unknown");

    public final String associatedMethodName;
    public final IkasanFlowElementCategory elementCategory;

    /**
     * Represents a flow element e.g. JMS Consumer, DB Consumer et
     * @param elementCategory e.g. CONSUMER, PRODUCER
     * @param associatedMethodName in the source code that can be used to identify this element, typically used by ComponentBuilder.
     */
    IkasanFlowElementType(IkasanFlowElementCategory elementCategory, String associatedMethodName) {
        this.associatedMethodName = associatedMethodName;
        this.elementCategory = elementCategory;
    }

    public String getAssociatedMethodName() {
        return associatedMethodName;
    }

    public IkasanFlowElementCategory getElementCategory() {
        return elementCategory;
    }

    public static IkasanFlowElementType parseMethodName(String methodName) {
        if (methodName != null) {
            for (IkasanFlowElementType name : IkasanFlowElementType.values()) {
                if (StringUtils.containsIgnoreCase(methodName, name.associatedMethodName)) {
                    return name;
                }
            }
        }
        return UNKNOWN;
    }

    public static IkasanFlowElement getEndpointForFlowElement(IkasanFlowElement ikasanFlowElement) {
        IkasanFlowElement endpointFlowElement = null ;
        if (ikasanFlowElement.getType() != null) {
            Object description;
            switch (ikasanFlowElement.getType()) {
                case SFTP_CONSUMER :
                    endpointFlowElement = new IkasanFlowElement();
                    endpointFlowElement.setTypeAndViewHandler(SFTP_LOCATION);
                    description = ikasanFlowElement.getProperties().get("SftpRemoteHost");
                    endpointFlowElement.setDescription(description != null ? description.toString() : "");
                    break;
                case FTP_PRODUCER :
                case FTP_CONSUMER :
                    endpointFlowElement = new IkasanFlowElement();
                    endpointFlowElement.setTypeAndViewHandler(FTP_LOCATION);
                    description = ikasanFlowElement.getProperties().get("RemoteHost");
                    endpointFlowElement.setDescription(description != null ? description.toString() : "");
                    break;
                case JMS_PRODUCER :
                case JMS_CONSUMER :
                    endpointFlowElement = new IkasanFlowElement();
                    endpointFlowElement.setTypeAndViewHandler(CHANNEL);
                    description = ikasanFlowElement.getProperties().get("DestinationJndiName");
                    endpointFlowElement.setDescription(description != null ? description.toString() : "");
                    break;
                case DB_PRODUCER :
                case DB_CONSUMER :
                    endpointFlowElement = new IkasanFlowElement();
                    endpointFlowElement.setTypeAndViewHandler(DB);
                    description = ikasanFlowElement.getProperties().get("setConfiguredResourceId");
                    endpointFlowElement.setDescription(description != null ? description.toString() : "");
                    break;
            }
        }
        return endpointFlowElement;
    }
}
