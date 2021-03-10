package org.ikasan.studio.ui.model;

import org.apache.log4j.Logger;
import org.ikasan.studio.model.ikasan.IkasanComponentType;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class IkasanFlowUIComponentFactory {
    private static final Logger log = Logger.getLogger(IkasanFlowUIComponentFactory.class);

    private static List<IkasanFlowUIComponent> ikasanFlowUIComponents = new ArrayList<>();
    private IkasanFlowUIComponent UNKNOWN ;
    private static String FLOW_HELP_TEXT = "The flow is the container for components and generally represents an atomic action";
    private static String BROKER_HELP_TEXT = "Brokers enrich the contents of the existing message with additional data or structure in a number of different ways. Request Response Brokers can make calls to other systems such as a database or HTTP(s) RESTful services. Aggregating Brokers consume all incoming messages until a condition is met ie aggregate every 10 messages. Re-Sequencing Brokers consume all incoming messages until a condition is met and then release them messages as a list of newly ordered events. This can provide a powerful function when combined with a Splitter as the next component.";
    private static String SPLITTER_HELP_TEXT = "Splitters break up the incoming event in to many outgoing events. ikasan will operate on the returned list of events and pass each event in the list independently to the next component for processing. Read more about EIP Sequencer In order to create your own splitter you need to implement Splitter Interface.";
    private static String CHANNEL_HELP_TEXT = "";
    private static String CONSUMER_HELP_TEXT = "Consumers provide the \"glue\" between the entry into the flow and the underlying technology generating the event. In order to create your own consumer you need to implement Consumer Interface.";
    private static String SCHEDULED_CONSUMER_HELP_TEXT = "This is a \"time event\" based consumer configured to be either an absolute or relative time schedule.";
    private static String FTP_CONSUMER_HELP_TEXT = "This is a \"time event\" based consumer configured to be either an absolute or relative time schedule, backed by FTP MessageProvider.";
    private static String SFTP_CONSUMER_HELP_TEXT = "This consumer is variation of Scheduled Consumer which is a \"time event\" based consumer configured to be either an absolute or relative time schedule, backed by (S)FTP Message provider.";
    private static String LOCAL_FILE_CONSUMER_HELP_TEXT = "This consumer is a variation of Scheduled Consumer which is a \"time event\" based consumer configured to be either an absolute or relative time schedule, backed by Local File MessageProvider.";
    private static String MONGO_CONSUMER_HELP_TEXT = "This consumer is variation of Scheduled Consumer which is a \"time event\" based consumer configured to be either an absolute or relative time schedule, backed by Mongo Message provider.";
    private static String GENERIC_MESSAGE_CONSUMER_HELP_TEXT = "The JMS consumer is a event driven consumer, used to connect to Legacy JBoss 4.3 and JBoss 5.1 Jboss Messaging.";
    private static String SPRING_MESSAGE_CONSUMER_HELP_TEXT = "The JMS consumer is Event Driven Consumer, used to connect to any Vendor specific JMS Broker(ActiveMQ, HornetQ, IBM MQ etc). However one need to include the related vendor specific libraries in the IM.";
    private static String CONVERTER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another.";
    private static String JSON_XML_CONVERTER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another. A converter acts as an adapter between components requiring different input types. The purpose of this converter is to take a JSON payload and convert it to an XML payload. It does this by delegating to a Marshaller. There are various Marshaller implementations available in ikasan. See Marshallers for more details of the various implementations.";
    private static String MAP_MESSAGE_OBJECT_CONVERTER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another. The Map Message to Object Converter is an implementation of the Converter Interface. It provides a mechanism that translates a JMS MapMessge into an Object, by retrieving the Object from the MapMessage using the attribute name provided on the configuration.";
    private static String MAP_MESSAGE_PAYLOAD_CONVERTER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another. The Map Message to Payload Converter is an implementation of the Converter Interface. It provides a mechanism that translates a JMS MapMessge into an Payload, by retrieving the content Object from the MapMessage using the content attribute name provided on the configuration and converting it to a Payload.";
    private static String PAYLOAD_TO_MAP_CONVERTER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another. The Map Message to Payload Converter is an implementation of the Converter Interface. It provides a mechanism that translates a Payload into a Map of <String, String>.";
    private static String OBJECT_MESSAGE_OBJECT_CONVERTER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another. The Map Message to Payload Converter is an implementation of the Converter Interface. The Object Message to Objcet Converter is an implementation of the Converter Interface. It provides a mechanism that translates a JMS ObjectMessge into an Object, by retrieving the content Object from the ObjectMessge and returning this Object.";
    private static String OBJECT_MESSAGE_XML_STRING_CONVERTER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another. The Object Message to XML String Converter is an implementation of the Converter Interface. It provides a mechanism that translates a JAXB Object into an XML representation of that Object as a String.";
    private static String XML_BYTE_ARRAY_OBJECT_CONVERTER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another. The XML Byte Array to Object Converter is an implementation of the Converter Interface. It provides a mechanism that translates a byte[] representation of an XML String and converts it to a JAXB Object which is a materialised Java POJO that has been initialised with the contents of the XML.";
    private static String XML_STRING_OBJECT_CONVERTER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another. The XML String to Object Converter is an implementation of the Converter Interface. It provides a mechanism that translates String representation of an XML Document and converts it to a JAXB Object which is a materialised Java POJO that has been initialised with the contents of the XML.";
    private static String XML_JSON_CONVERTER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another. The purpose of this converter is to take an XML payload and convert it to a JSON payload. It does this by delegating to a Marshaller. There are various Marshaller implementations available in ikasan. See Marshallers for more details of the various implementations.";
    private static String XSLT_CONFIGURATION_PARAMETER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another. The purpose of the XsltConverter is to convert configuration parameters into xslt parameters.";
    private static String XSLT_CONVERTER_HELP_TEXT = "The main responsibility of a converter is to convert from one POJO type to another. The XSLT Converter is an implementation of the Converter Interface. It provides a mechanism that applies an XSLT against an XML payload, subsequently transforming it to another XML format, or other formats that are supported by XSLT.";
    private static String FILTER_HELP_TEXT = "Filter will allow given event to be past to next component or it will end the flow. You can think of filter as of an 'IF' statment in a programming language. In order to create your own filter you need to implement Filter Interface.";
    private static String ROUTER_HELP_TEXT = "Decouple individual processing steps so that messages can be passed to different filters depending on a set of conditions.";
    private static String DEV_NULL_PRODUCER_HELP_TEXT = "Ending component of the flow, or a route in the flow. This type of producer discards all data passed to it and does not perform any processing.";
    private static String LOGGING_PRODUCER_HELP_TEXT = "Ending component of the flow, or a route in the flow. This type of producer logs all data passed to it.";
    private static String EMAIL_PRODUCER_HELP_TEXT = "Ending component of the flow, or a route in the flow. This type of producer sends an email. This requires an incoming EmailPayload event instance.";
    private static String JMS_PRODUCER_HELP_TEXT = "Ending component of the flow, or a route in the flow. The JMS producer is based on Spring template and is used to connect to any Vendor specific JMS Broker(ActiveMQ, HornetQ, IBM MQ etc). However one need to include the related vendor specific libraries in the IM.";
    private static String FTP_PRODUCER_HELP_TEXT = "Ending component of the flow, or a route in the flow. This producer allows delivery of a file to remote (S)FTP server. The producer is under pined with persistent store which saves meta information about the deliver files.";
    private static String TRANSLATOR_HELP_TEXT = "Translators receive and return the same object, but allow for the constituent parts of that object to be changed. For instance, if you pass object A to a Translator the Translator may change the content of Object A, but cannot return a new object. In order to create your own translator you need to implement Translator Interface.";
    private static String ENDPOINT_HELP_TEXT = "The endpoint holds details of the origin or destination feeding into or out of a flow.";
    private static String BESPOKE_HELP_TEXT = "A user created custom component.";
    private static final String WEB_URL_BASE = "https://github.com/ikasanEIP/ikasan/blob/3.1.x/ikasaneip/component/";

    private IkasanFlowUIComponentFactory() {
        UNKNOWN = createIkasanFlowUIComponent("Bespoke Generator", BESPOKE_HELP_TEXT, "Readme.md", IkasanComponentType.UNKNOWN, "unknown");
        //@todo add in link to online ikasan help
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Flow",FLOW_HELP_TEXT, "Readme.md", IkasanComponentType.FLOW, "flow"));

        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Aggregator","Aggregator is an ipsum", "Readme.md", IkasanComponentType.UNKNOWN, "aggregator"));

        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Splitter", SPLITTER_HELP_TEXT, "splitter/ConcurrentSplitter.md", IkasanComponentType.SPLITTER, "splitter"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("List Splitter", SPLITTER_HELP_TEXT, "splitter/DefaultSplitter.md", IkasanComponentType.LIST_SPLITTER, "splitter"));

        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Broker", BROKER_HELP_TEXT, "Readme.md", IkasanComponentType.BROKER, "broker"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Exception Generating Broker", BROKER_HELP_TEXT, "Readme.md", IkasanComponentType.EXCEPTION_GENERATING_BROKER, "broker"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Delay Generation Broker", BROKER_HELP_TEXT, "Readme.md", IkasanComponentType.DELAY_GENERATION_BROKER, "broker"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("DB Broker", BROKER_HELP_TEXT, "Readme.md", IkasanComponentType.DB_BROKER, "broker"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Schedule Rule Check Broker", BROKER_HELP_TEXT, "Readme.md", IkasanComponentType.SCHEDULE_RULE_CHECK_BROKER, "broker"));

        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Channel", CHANNEL_HELP_TEXT, "Readme.md", IkasanComponentType.CHANNEL, "channel"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Channel Adapter", CHANNEL_HELP_TEXT, "Readme.md", IkasanComponentType.CHANNEL, "channel-adapter"));

//        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Event Driven Consumer", CONSUMER_HELP_TEXT, "Readme.md", IkasanComponentType.EVENT_DRIVEN_CONSUMER, "event-driven-consumer"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Event Generating Consumer", CONSUMER_HELP_TEXT, "Readme.md", IkasanComponentType.EVENT_GENERATING_CONSUMER, "event-driven-consumer"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Scheduled Consumer", SCHEDULED_CONSUMER_HELP_TEXT, "endpoint/quartz-schedule/Readme.md", IkasanComponentType.SCHEDULED_CONSUMER, "scheduled-consumer"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("FTP Consumer", FTP_CONSUMER_HELP_TEXT, "endpoint/filetransfer/ftp/consumer.md", IkasanComponentType.FTP_CONSUMER, "ftp-consumer"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("SFTP Consumer", SFTP_CONSUMER_HELP_TEXT, "endpoint/filetransfer/sftp/consumer.md", IkasanComponentType.SFTP_CONSUMER, "ftp-consumer"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Local File Consumer", LOCAL_FILE_CONSUMER_HELP_TEXT, "endpoint/quartz-schedule/localFileConsumer.md", IkasanComponentType.LOCAL_FILE_CONSUMER, "local-file-consumer"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Mongo Consumer", MONGO_CONSUMER_HELP_TEXT, "endpoint/mongo-endpoint/Readme.md", IkasanComponentType.MONGO_CONSUMER, "mongo-consumer"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Generic Message Consumer", GENERIC_MESSAGE_CONSUMER_HELP_TEXT, "endpoint/jms-client/consumer.md", IkasanComponentType.JMS_CONSUMER, "message-consumer"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Spring Message Consumer", SPRING_MESSAGE_CONSUMER_HELP_TEXT, "endpoint/jms-spring-arjuna/consumer.md", IkasanComponentType.SPRING_JMS_CONSUMER, "message-consumer"));

        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Custom Converter", XML_JSON_CONVERTER_HELP_TEXT, "component/converter", IkasanComponentType.CUSTOM_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("JSON XML Converter", JSON_XML_CONVERTER_HELP_TEXT, "converter/JsonToXmlConverter.md", IkasanComponentType.JSON_XML_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Map Message to Object Converter", MAP_MESSAGE_OBJECT_CONVERTER_HELP_TEXT, "converter/MapMessageToObjectConverter.md", IkasanComponentType.MAP_MESSAGE_TO_OBJECT_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Map Message to Payload Converter", MAP_MESSAGE_PAYLOAD_CONVERTER_HELP_TEXT, "converter/MapMessageToPayloadConverter.md", IkasanComponentType.MAP_MESSAGE_TO_PAYLOAD_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Object Message to Object Converter", OBJECT_MESSAGE_OBJECT_CONVERTER_HELP_TEXT, "converter/ObjectMessageToObjectConverter.md", IkasanComponentType.OBJECT_MESSAGE_TO_OBJECT_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Object Message to XML String Converter", OBJECT_MESSAGE_XML_STRING_CONVERTER_HELP_TEXT, "converter/ObjectToXmlStringConverter.md", IkasanComponentType.OBJECT_MESSAGE_TO_XML_STRING_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Text Message to String Converter", CONVERTER_HELP_TEXT, "converter/TextMessageToStringConverter.md", IkasanComponentType.TEXT_MESSAGE_TO_STRING_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Threadsafe to XSLT Converter", CONVERTER_HELP_TEXT, "component/converter/ThreadSafeXsltConverter.md", IkasanComponentType.THREAD_SAFE_XSLT_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("XML Byte Array to Object Converter", XML_BYTE_ARRAY_OBJECT_CONVERTER_HELP_TEXT, "converter/XmlByteArrayToObjectConverter.md", IkasanComponentType.XML_BYTE_ARRAY_TO_OBJECT_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("XML String to Object Converter", XML_STRING_OBJECT_CONVERTER_HELP_TEXT, "component/converter/XmlStringToObjectConverter.md", IkasanComponentType.XML_STRING_TO_OBJECT_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("XML to JSON Converter", XML_JSON_CONVERTER_HELP_TEXT, "component/converter/XmlToJsonConverter.md", IkasanComponentType.XML_TO_JSON_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("XSLT Configuration Converter", XSLT_CONFIGURATION_PARAMETER_HELP_TEXT, "converter/XsltConfigurationParameterConverter.md", IkasanComponentType.XSLT_CONFIGURATION_PARAMETER_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("XSLT Converter", XSLT_CONVERTER_HELP_TEXT, "converter/XsltConverter.md", IkasanComponentType.XSLT_CONVERTER, "message-translator"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Payload to Map Converter", PAYLOAD_TO_MAP_CONVERTER_HELP_TEXT, "Readme.md", IkasanComponentType.PAYLOAD_TO_MAP_CONVERTER, "message-translator"));

        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Single Recipient Router", ROUTER_HELP_TEXT, "Readme.md", IkasanComponentType.SINGLE_RECIPIENT_ROUTER, "message-router"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Multi Recipient Router", ROUTER_HELP_TEXT, "Readme.md", IkasanComponentType.MULTI_RECIPIENT_ROUTER, "message-router"));

        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Dev Null Producer", DEV_NULL_PRODUCER_HELP_TEXT, "Readme.md", IkasanComponentType.DEV_NULL_PRODUCER, "message-endpoint"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Logging Producer", LOGGING_PRODUCER_HELP_TEXT, "Readme.md", IkasanComponentType.LOG_PRODUCER, "message-endpoint"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Email Producer", EMAIL_PRODUCER_HELP_TEXT, "Readme.md", IkasanComponentType.EMAIL_PRODUCER, "message-endpoint"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("JMS Producer", JMS_PRODUCER_HELP_TEXT, "Readme.md", IkasanComponentType.JMS_PRODUCER, "jms-producer"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("FTP Producer", FTP_PRODUCER_HELP_TEXT, "Readme.md", IkasanComponentType.FTP_PRODUCER, "message-endpoint"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("SFTP Producer", FTP_PRODUCER_HELP_TEXT, "Readme.md", IkasanComponentType.SFTP_PRODUCER, "message-endpoint"));

        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Filter", FILTER_HELP_TEXT, "Readme.md", IkasanComponentType.MESSAGE_FILTER, "message-filter"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Message Translator", TRANSLATOR_HELP_TEXT, "Readme.md", IkasanComponentType.TRANSLATOR, "message-translator"));

        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Ftp Location", ENDPOINT_HELP_TEXT, "Readme.md", IkasanComponentType.FTP_LOCATION, "sftp-location"));
        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Sftp Location", ENDPOINT_HELP_TEXT, "Readme.md", IkasanComponentType.SFTP_LOCATION, "sftp-location"));

        ikasanFlowUIComponents.add(createIkasanFlowUIComponent("Bespoke Generator", BESPOKE_HELP_TEXT, "Readme.md", IkasanComponentType.UNKNOWN, "unknown"));

    }

    private IkasanFlowUIComponent createIkasanFlowUIComponent(String text, String helpText, String webHelpURL, IkasanComponentType ikasanComponentType, String iconName) {
        String smallIconLocation = "/studio/icons/paletteSmall/" + iconName + ".png";
        String standardIconLocation = "/studio/icons/palette/" + iconName + ".png";
        return new IkasanFlowUIComponent(text, helpText, WEB_URL_BASE + webHelpURL, ikasanComponentType, getImageIcon(smallIconLocation), getImageIcon(standardIconLocation));
    }

    private ImageIcon getImageIcon(String iconLocation) {
        ImageIcon imageIcon = null;
        try {
            imageIcon = new ImageIcon(getClass().getResource(iconLocation));
        } catch (NullPointerException npe) {
            log.error("Could not create Icon for " + iconLocation, npe);
        }
        return imageIcon;
    }

    //*todo make into Enum ?
    private static class BillPughSingleton  // Credit where it is due.
    {
        private static final IkasanFlowUIComponentFactory INSTANCE = new IkasanFlowUIComponentFactory();
    }

    public static IkasanFlowUIComponentFactory getInstance()
    {
        return BillPughSingleton.INSTANCE;
    }

    public IkasanFlowUIComponent getIkasanFlowUIComponentFromType(IkasanComponentType ikasanComponentType) {
        return ikasanComponentType == null ? UNKNOWN : ikasanFlowUIComponents.stream().filter(x -> x.getIkasanComponentType().equals(ikasanComponentType)).findFirst().orElse(UNKNOWN);
    }

    public IkasanFlowUIComponent getUNKNOWN() {
        return UNKNOWN;
    }

    public List<IkasanFlowUIComponent> getIkasanFlowComponents() {
        return ikasanFlowUIComponents;
    }
}
