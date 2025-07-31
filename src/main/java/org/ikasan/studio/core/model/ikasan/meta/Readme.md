### Anatomy of a Component

Taking the example of a JMS Consumer.

**ComponentTypeMeta** - 

Reflects the state of a generic component type, in this case a '**Consumer**'. This typically includes

* "componentType": e.g. "org.ikasan.spec.component.endpoint.Consumer",
* "displayOrder": in the palette
* "componentShortType": "Consumer",
* "helpText": 
* "allowableProperties": a list of the properties this component type can have e.g. description.

**ComponentMeta** - 

Reflects the state for the specific component, in this case '**JMS Consumer**', that is the state that is common to all JMS Consumers, regardless of the specific implementation.

Some of the attributes include:

* "canvasIcon" - the icon to display in the palette and on the canvas
* "componentTypeMeta" - A ComponentMeta has a ComponentTypeMeta, this is a composition relationship, not an inheritance relationship, e.g. the help text for Consumer is different to the help text for JMS Consumer.
* "implementingClass": "org.ikasan.component.endpoint.jms.spring.consumer.JmsContainerConsumer"
* "endpointKey": This indicates hwo to present and endpoint e.g. a DB, and SFTP location, a JMS destination, etc.
* "allowableProperties": a list of the properties this component can have e.g. description.

**ComponentPropertyMeta** - 

This is the meta-data for each of the allowableProperties.

AllowableProperties reflect the **state of a component instance** i.e. in a specific flow route, so a JMS Consumer in one flow route will have different properties to a JMS Consumer in another flow route.

### How is the metapack structured?

The Meta-Pack holds a complete set of information for all the components for a given version of Ikasan e.g.
* Properties,
* screen icon,
* template generation file,
* validation etc)

The idea is that meta-packs can be added / removed / updated from the plugin so that the end user can support multiple versions of Ikasan,
or even their own custom version and custom components and custom free-marker templates to auto generate boilerplate and custom code (this last point is **why meta resides in the core package grouping and not ui**).

The installed metapacks can be found in resources/studio/metapack/*

Within that folder are the roots of each metapack version e.g. V3.3.3 is the metapack for Ikasan version 3.3.3, V4.0.x is the metapack for Ikasan version 4.0.x, etc.

Within each metapack version is a 
* template folder containing the free-marker templates for the code generation for that metapack.
* library folder containing the meta-data for all the components supported by that metapack. 
  * Each component type (Broker, Consumer etc.) has its own top level folder and **component-type-meta*.json* (derserialised into '**ComponentTypeMeta.java**'). 
  * Each specialisation of the component types has its own subfolder and **component-meta.json** (deserailised into '**ComponentMeta.java**').

### How is the metapack used?

IkasanComponentLibrary is a singleton that is used to expose metapacks for use within the IDE e.g. by the:
* deserialiser to convert the model.json into an in-memory representation of the Ikasan module
* code generator to generate the java code from the in-memory representation of the Ikasan module
* validation to validate the in-memory representation of the Ikasan module
* ui to display the in-memory representation of the Ikasan module
* ui to display the palette of components available for the user to drag and drop into the module
* ui to display / validate / provide help for the properties for each component.
