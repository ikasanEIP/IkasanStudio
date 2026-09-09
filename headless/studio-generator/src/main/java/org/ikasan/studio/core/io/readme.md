# Meta Data and Instance Data

## Ikasan MetaData Pack
The pack provides plugin style support for 

* A specific version of Ikasan.
* An alternative implementation e.g. raw component based, template based, Spring or non Spring based
* A cut down / simplified implementation

The pack consists of 
* Generic meta and icons for the pack as a whole
* Meta data for each component supported

For each component or meta component or user defined aggregate components, the pck will provide
* The icons for the palette and canvas
* Metadata describing the component
* Metadata (e.g. type, help, default value, optionality, name) for each property that the component supports

## Ikasan MetaData Model
* The metadata pack(s) are loaded on demand, as either the user creates a new module for a specific version or an existing module is loaded into the IDE (remember, Intellij shares its data will open Windows of the IDE i.e. if there are multiple projects open, they all share the same central memory space).
* The metadata library is indexes by the version of the metadata pack that has been chosen
* The metadata library supports the creation of all the instance i.e. when a new component is created, the metadata for that component will be used to create the form  i.e. what properties, their type, default, help text etc

## Ikasan Instance Data Model
* The instance data reflects an actual implementation of an Ikasan module including flows and fow elements
* The instance data model is created based on the properties maps provided by the MetaData model as a result, if we wish to serialise and deserialize JSON that resembles the structure used by the dashboard, we need customer serialisation to map the instance data into the flat and explicit name / values we see in the dashboard JSON




