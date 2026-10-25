# FTP / JMS payload-map demo

This sample targets **Ikasan 4.1.6** with **Java 17**. Both demo flows are already present
in the Studio model and generated application; no AI proposal import is needed.

- **Payload To JMS Map Demo:** Read Demo Files (FTP) → Payload To Map → Send Map Message.
- **JMS Map To Payload Demo:** Receive Map Message → Require Map Message → Map Message To Payload → Log Restored Payload.

The FTP consumer supplies org.ikasan.filetransfer.Payload. PayloadToMapConverter copies its
content, ID and attributes to a Java Map. Spring JMS sends a real MapMessage; the receiver
keeps autoContentConversion=false, checks the message kind, and restores the Payload using
MapMessageToPayloadConverter with idAttributeName=id. Both flows retain AUTOMATIC startup.
The original order-processing flows are also retained.

## Run

1. Open this sample's pom.xml in IntelliJ with the current Studio plugin and matching meta-pack.
   For Ikasan 5, enable snapshot meta-packs and install/select the matching updated pack if needed.
   Allow Maven import and indexing to finish.
2. Right-click Read Demo Files or its endpoint and choose **Start Test FTP Server**, then
   **Show Test FTP Server Details**. Match the consumer settings to the actual server details.
   The sample uses the catalogue's local harness defaults. The original email flow also needs
   its configured SMTP service or Studio's mail harness.
3. Use **Run module**. Upload demo-input/payload-map-demo-1.txt into the FTP root and observe
   the restored Payload content in the Run log. Files are polled every five seconds with
   minAge=0 for this demo. Only payload-map-demo-*.txt files match.
4. Successful input is retained as .txt.done, preventing repeated processing. For gradual
   uploads, use .upload and rename to .txt when complete.
5. Upload demo-input/payload-map-demo-2.txt later, without restarting either flow.
   Both flows should remain running in **Console**, the module-local Blue Console, between inputs.

Run samples individually, or configure distinct FTP endpoints/roots and broker destinations.
Both stock converters use the runtime default charset for text. FTP assigns the Payload ID
from filename and file timestamp; it is not a fixed filename or a content-only hash.

## Tests

```sh
mvn -pl user-flow-tests -am -Dtest=PayloadToJMSMapDemoFlowTest,JMSMapToPayloadDemoFlowTest -Dsurefire.failIfNoSpecifiedTests=false test
```

PayloadToJMSMapDemoFlowTest compares the output Map without its variable id, using a comparison
copy so the observed payload is unchanged. JMSMapToPayloadDemoFlowTest sends real MapMessage
fixtures through sendMap and checks the restored content. Both scenarios include first and
later delivery without a flow restart. Keep the classpath fixtures under src/test/resources;
they are independent from the manual-upload examples in demo-input/.

The tests use isolated fixture servers and test-only startup overrides. The IDE's normal Run
module workflow still requires the FTP and SMTP prerequisites above. Broader test scenarios
may need their own setup; consult user-flow-tests/README.md and module-test.properties.

## Sample contents

Source, Maven POMs, the current Studio model, generated application/verification sources,
classpath fixtures and demo inputs are retained. Local build output, IDE state, runtime stores,
recovery data, cached readiness reports, backups and applied proposal history are omitted.
Regenerate reports through Studio when needed; no previous runtime state is required.
