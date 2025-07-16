This package provide support for the Meta-Data and Meta-Pack.

The Meta-Pack holds a complete set of information for all the components for a given version of Ikasan e.g.
* Properties,
* screen icon,
* template generation file,
* validation etc)

The idea is that meta-packs can be added / removed / updated from the plugin so that the end user can support multiple versions of Ikasan,
or even their own custom version and custom components and custom free-marker templates to auto generate boilerplate and custom code (this last point is **why meta resides in the core package grouping and not ui**).

IkasanComponentLibrary is a singleton, it is a kind of factory, it holds the meta-data per Ikasan version. This metadata is used by the:
* deserialiser to convert the model.json into an in-memory representation of the Ikasan module
* code generator to generate the java code from the in-memory representation of the Ikasan module
* validation to validate the in-memory representation of the Ikasan module
* ui to display the in-memory representation of the Ikasan module
* ui to display the palette of components available for the user to drag and drop into the module
* ui to display / validate / provide help for the properties for each component.

IkasanComponentLibrary is populated from the meta-pack, found in resources/studio/metapack/*

Within that folder are the roots of each metapack version e.g. V3.3.3 is the metapack for Ikasan version 3.3.3, V4.0.x is the metapack for Ikasan version 4.0.x, etc.

Within each metapack version is a 
* template folder containing the free-marker templates for the code generation for that metapack.
* library folder containing the meta-data for all the components supported by that metapack. 
  * Each component type (Broker, Consumer etc.) has its own top level folder and **component-type-meta*.json* (derserialised into '**ComponentTypeMeta.java**'). 
  * Each specialisation of the component types has its own subfolder and **component-meta.json** (deserailised into '**ComponentMeta.java**').



