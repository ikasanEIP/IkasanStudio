# Sending Java objects through JMS

## Fixing “Forbidden class … Order” in ActiveMQ

For the Spring JMS Consumer receiving an ActiveMQ ObjectMessage:

1. Open the consumer's properties.
2. Find **Trusted object packages (ActiveMQ)** in the **Popular** optional properties.
3. Enter the Java package containing the object, for example `org.example.cat.domain`.
   Enter the package, not `org.example.cat.domain.Order`. Separate more packages with commas.
4. Keep **autoContentConversion** off if the next component is **JMS Object Message To Object Converter**.
5. Apply the properties, regenerate the module and stop/start the module.

No JVM option or handwritten connection-factory class is required. This setting is available
on Spring JMS Producer too, when its connection factory needs the same allowance.
The consumer is the important side: it must be allowed to read the incoming serialized object.

Leave the setting empty for ordinary text, JSON, XML and byte messages. It does not change
an incoming message's type or turn JSON into an Order. The sending application must send
an actual JMS ObjectMessage containing a Serializable object. Compatible definitions of
that object's class must be on the sending and receiving application's classpath.

## Example test flow

```
Scheduled Consumer → Order Broker → Spring JMS Producer
                                          │
                                      orders queue
                                          │
Spring JMS Consumer → JMS Object Message To Object → Object To XML String → Logger
```

The Broker returns a sample `Order` implementing `java.io.Serializable`. If the XML converter
is used, Order also needs JAXB mappings (`javax.xml.bind` for the V3 pack, `jakarta.xml.bind`
for V4). The XML converter's objectClass is the full class name; the consumer's trusted
object packages setting is just its package.

## What Studio generates

The setting is stored in model.json as `trustedObjectPackages`. Both V3.3.9 and V4.1.6
provide the metadata and FreeMarker template. When populated, the component factory includes
a small ActiveMQ initial-context factory that adds those packages to the default trust list
before connections are created. It uses ActiveMQ's original factory creation, retaining
broker URL, JNDI names, credentials and XA selection. A directly supplied ActiveMQ factory
is configured in place, retaining Ikasan's existing credentials adapter.

Only explicitly listed packages are added; Studio does not infer trust from a converter's
objectClass and does not generate trustAllPackages or wildcard trust. Empty settings produce
the existing generation path. Package changes require regeneration and a module restart.
The setting is generated from the Studio model, rather than read from a JVM system property.

This convenience targets **ActiveMQ Classic** with its standard JNDI provider. For a custom
JNDI properties map/provider, another JMS vendor, or a wrapped connection-factory bean,
configure trust in your custom factory instead. Studio rejects custom JNDI maps combined
with this setting; the generated helper also rejects unsupported provider/factory types.

## Developer checks

```
./gradlew -p headless :studio-bundled-packs:test --tests '*Trusted*'
```

Template tests check both consumers and producers, empty settings, supplied factories and
invalid package lists. The standalone runtime tests compile the generated helper for Java 11
(V3) and Java 17 (V4), using each pack's BOM-managed ActiveMQ client in an isolated classloader.
They verify JNDI/XA selection, retained defaults, rejection without a package allowance and
successful deserialization of an Order once its package is allowed. They do not launch a broker.
