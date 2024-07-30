<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# IkasanStudio Changelog

## [1.0.0]

#### July
General
- Fix property list
- Improve email producer
- Expand HelpText
- Add support for terminal interaction
- Add support for launching H2 directly from Intellij via use of platform neutral maven commands and Intellij Terminal integratiom
- Expose application.properties for use inside plugin

Simplify property entry:
- Add UI helper for any cron based data entry
- Add dialoge for cron configuration entry

#### June
General
- Tune JSM config
- Simplify prperty substitution into compenent properties

UX
- Fix bug in startup meta pack
- For new Maven generated projects, the app name and package name are pre-populated from the maven values.
- Swap back to palette panel when property update is completed.
- Enhance debug component look and feel and integration

Fixes
- Threading issue caused by new release of Intellij

#### May
General
- Add further support for user supplied generic components (and their serlaisation).
- Add jump to code from components.

Increase developer self help:
- Isolate the component description/help as a reusable widget.
- Add component description/help to properties panel.
- Add new debug component, ensure it is transient and read only

MetaPack
- Fix issues with DefautListSplitter, Local file consumer, JMS consumer, Message Filter
- Support specific help at component group level (expose in palette)

Simplify property entry:
- Dialogues have optional properties minimised by default.
- Provide button to reset to defaults.
- Provide support to expose and subsequently ignore optional properties.

QA
- Extend unit tests with Codium AI generated tests.
- 
#### April - Mature the configuration module and application split

General
- Add jump to code from components.
- Create maven archetype for new projects.
- Introduce further Ikasan branding.
- Tune lombok to reduce potential for error and support free clone and builders.
- Extend support for generating internal model from model.json

MetaPack
- Add support for inheritance in configuration between components and their category.
- Add default splitter, generic splitter and custome splitter.
- Add support for generic consumer.
- Add single recipient router and multi-recipient router. Refactor model and rendering to accommodate 'route' concepts.
- Fix issues with JMS consumer/producer, email consumer/producer.
- Fix 4th+ tier of router nesting.
- Introduce hidden properties with defaults.
- Fix endpoint rendering.
- Add support for generic / unknown components with basic interface support and user supplied implementations

Increase developer self help:
- Add quick start documentation to Readme.md.
- Add 'self-healing/best guess' to 'model.json' for simple developer mistakes.

Resilience
- Capture and process freemarker exceptions.
- Utiilise messaging system for notifications.

Simply property entry:
Add support for lists in meta and data entry.

UI
- Add scrolling support to main canvas.
- Fix issues with slow scrollpane scrollig.
- Move properties panel to right of main screen, tabbing with Palette panel.
- Split generated code into 'user' and 'auto-gen' roots, adjust poms, compile hierarchy and archetype approperiately.
- Extend mouse hover validation.

#### March - Extend component palette

General
- Tidy away generated components when flow is deleted.
- Improve project creation journey
- Improve change listeners on combo-boxes
- Create list of features and components for MVP, document design
- Support for pom management.
- Reduce coupling for IkasanType
- Start support for splitting UI and Metapack, into separate products / lifecycles.

MetaPack
- Add MetaPack versioning support.
- Add support for switching meta0packs mid-development.
- Add Broker component
- Add Exception Resolver component, refactor to work with new meta-pack paradigm, remove osbolete associated lookups.
- Add basic JMS & Spring JMS consumer / producer
- Add Message to XML converter
- Add support for confined choices / drop down choices for component configuration.
- Add sftp & ftp producer
- Add message filter.
- Update PK mechanism for components with non-unique identity.
- Extend scheduled consumer
- Add support for property validation, property sorting and bespoke classes.
- Add Logging producer

QA
Extend unit tests and reusable test fixtures.
Address Qonda issues
Address intellij thread issues, stack overflow and general error handling.
Address empty consumer issues.

UI
- Reduce coupling in ViewHandlers, introduce caching.
- Introduce endpoint rendering.
- Add generator unit tests, meta unit tests, reusable test fixtures.
- Improve icons for components.
- Sort components and improve display ordering in palette

#### Feb - Switch from code based persistence to model.json

MetaPack
- Add Event driven consumer
- Add support for jar dependencies

General
- Refactor Java Code based model onto json based model.
- Refactor to simplify component types.
- Add support for serialisation and deserialisation of component transitions / general transition support.
- Resolve FTL build issue.
- Introduce Lombok for data model, remove now redundant boilerplate code.
- Refactor previous code base to suport consistent naming conventions.

QA
- Resolve UI threading issue during code generation.
- Add test cover for serialisation.

UI
- Initialise the internal model using model.json.
- Refactor serialisation / deserialisation mapping from model.json into in memory module model. Coverage for module, flow and 1 flow element.

#### Jan
- Expansion of metapacks to suppport Ikasan standard release and end user bespoke code generation.
- Support documenations and resource links.
- Resilience and intelligence to best guess corrections or workarounds for typical developer mistakes / misconfigurations.

## [Unreleased]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
