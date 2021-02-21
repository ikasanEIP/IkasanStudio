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

    BROKER(IkasanFlowComponentCategory.BROKER, false, "broker"),
    DB_BROKER(IkasanFlowComponentCategory.BROKER, false, "DbBroker"),
    DELAY_GENERATION_BROKER(IkasanFlowComponentCategory.BROKER, false, "DelayGenerationBroker"),
    EXCEPTION_GENERATING_BROKER(IkasanFlowComponentCategory.BROKER, false, "ExceptionGenerationgBroker"),
    SCHEDULE_RULE_CHECK_BROKER(IkasanFlowComponentCategory.BROKER, false, "ScheduledRuleCheckBroker"),

    DB_CONSUMER(IkasanFlowComponentCategory.CONSUMER, false, "DBConsumer"),
//    EVENT_DRIVEN_CONSUMER(IkasanFlowComponentCategory.CONSUMER, false, "eventDrivenConsumer"),  cant seem to find this in Ikasan code base.
    EVENT_GENERATING_CONSUMER(IkasanFlowComponentCategory.CONSUMER, false, "eventGeneratingConsumer"),
    SCHEDULED_CONSUMER(IkasanFlowComponentCategory.CONSUMER, false, "scheduledConsumer"),
    FTP_CONSUMER(IkasanFlowComponentCategory.CONSUMER, false, "ftpConsumer"),
    JMS_CONSUMER(IkasanFlowComponentCategory.CONSUMER, false, "jmsConsumer"),
    SPRING_JMS_CONSUMER(IkasanFlowComponentCategory.CONSUMER, false, "jmsConsumer"),
    SFTP_CONSUMER(IkasanFlowComponentCategory.CONSUMER, false, "sftpConsumer"),
    LOCAL_FILE_CONSUMER(IkasanFlowComponentCategory.CONSUMER, false, "fileConsumer"),
    MONGO_CONSUMER(IkasanFlowComponentCategory.CONSUMER, false, "mongoConsumer"),

    // @todo, maybe introduce the base class into this list, might conflict though when matching custom components.
    CUSTOM_CONVERTER(IkasanFlowComponentCategory.CONVERTER, true, "Converter"),  // This will really be the implemented interface
    JSON_XML_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "JsonXmlConverter"),
    MAP_MESSAGE_TO_OBJECT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "MapMessageToObjectConverter"),
    MAP_MESSAGE_TO_PAYLOAD_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "MapMessageToPayloadConverter"),
    OBJECT_MESSAGE_TO_OBJECT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "ObjectMessageToObjectConverter"),
    OBJECT_MESSAGE_TO_XML_STRING_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "ObjectToXMLStringConverter"),
    OBJECT_TO_XML_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "ObjectToXmlStringConverter"),
    PAYLOAD_TO_MAP_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "PayloadToMapConverter"),
    TEXT_MESSAGE_TO_STRING_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "TextMessageToStringConverter"),
    THREAD_SAFE_XSLT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "ThreadSafeXsltConverter"),
    XML_BYTE_ARRAY_TO_OBJECT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "XmlByteArrayToObjectConverter"),
    XML_STRING_TO_OBJECT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "XmlStringToObjectConverter"),
    XML_TO_JSON_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "XmlJsonConverter"),
    XSLT_CONFIGURATION_PARAMETER_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "XsltConfigurationParameterConverter"),
    XSLT_CONVERTER(IkasanFlowComponentCategory.CONVERTER, false, "XsltConverter"),

    MESSAGE_FILTER(IkasanFlowComponentCategory.FILTER, false, "FilterInvokerConfiguration"),

    CHANNEL(IkasanFlowComponentCategory.ENDPOINT, false, "messageChannel"),
    FTP_LOCATION(IkasanFlowComponentCategory.ENDPOINT, false, "ftpLocation"),
    SFTP_LOCATION(IkasanFlowComponentCategory.ENDPOINT, false, "sftpLocation"),
    DB(IkasanFlowComponentCategory.ENDPOINT, false, "message-store"),

    LIST_SPLITTER(IkasanFlowComponentCategory.SPLITTER, false, "listSplitter"),
    SPLITTER(IkasanFlowComponentCategory.SPLITTER, false, "splitter"),

    TRANSLATOR(IkasanFlowComponentCategory.TRANSLATER, false, "Translator"),

    DB_PRODUCER(IkasanFlowComponentCategory.PRODUCER, false, "DBProducer"),
    DEV_NULL_PRODUCER(IkasanFlowComponentCategory.PRODUCER, false, "devNullProducer"),
    EMAIL_PRODUCER(IkasanFlowComponentCategory.PRODUCER, false, "emailProducer"),
    FTP_PRODUCER(IkasanFlowComponentCategory.PRODUCER, false, "ftpProducer"),
    SFTP_PRODUCER(IkasanFlowComponentCategory.PRODUCER, false, "sftpProducer"),
    JMS_PRODUCER(IkasanFlowComponentCategory.PRODUCER, false, "JmsProducer"),
    LOG_PRODUCER(IkasanFlowComponentCategory.PRODUCER, false, "logProducer"),

    SINGLE_RECIPIENT_ROUTER(IkasanFlowComponentCategory.ROUTER, false, "SingleRecipientRouter"),
    MULTI_RECIPIENT_ROUTER(IkasanFlowComponentCategory.ROUTER, false, "MultiRecipientRouter"),

    BESPOKE(IkasanFlowComponentCategory.UNKNOWN, true, "bespoke"),
    UNKNOWN(IkasanFlowComponentCategory.UNKNOWN, false, "unknown");

    public final String associatedMethodName;
    public final boolean bespokeClass;
    public final IkasanFlowComponentCategory elementCategory;
    Map<String, IkasanComponentPropertyMeta>  properties;

    /**
     * Represents a flow element e.g. JMS Consumer, DB Consumer et
     * @param elementCategory e.g. CONSUMER, PRODUCER
     * @param bespokeClass
     * @param associatedMethodName in the source code that can be used to identify this element, typically used by ComponentBuilder.
     */
    IkasanFlowComponentType(IkasanFlowComponentCategory elementCategory, boolean bespokeClass, String associatedMethodName) {
        this.bespokeClass = bespokeClass;
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
                IkasanComponentProperty newIkasanComponentProperty = new IkasanComponentProperty(entry.getValue());
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
                if (name.associatedMethodName != null &&
                    name.associatedMethodName.length() > 0 &&
                    StringUtils.equalsIgnoreCase(methodName, name.associatedMethodName)) {
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

    public boolean isBespokeClass() {
        return bespokeClass;
    }

    public String getAssociatedMethodName() {
        return associatedMethodName;
    }
}
