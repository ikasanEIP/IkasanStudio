# IkasanStudio

![Build](https://github.com/davidhilton68/IkasanStudio2/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
## High livel design / motivations
Offering is split into 3 artefects with independant lifecycles

### Core (org.ikasan.studio.core)
* Contains the model.json (de)serialisation
* Framework for code generation (ftl supplied by meta pack), 
* Framework for unit test support for meta pack, abstract support for meta pack

### Metapack (resources/studio/metapack) - depdends on Core
* One per supported Ikasan version
* Reduces the number of updates to the core (UI) plugin, the metapack can be added to (almost) any version of the plugin
* Distribution / exposed via web / repo / something
* Component Library - Encapsulates the Components properties, meta information, helptext, icons and ftl for a version of Ikasan, or an 'approach to auto-generation implementation', or user created components / code practice. 
* Provides extensive unit testing to self certify
* End user encouraged to create their own, only official metapacks are 'supported'

### UI (org.ikasan.studio.ui) - Depends on Core and Metapackbs
* Business driver is cost saving resulting from standard coding of components, reduced build times, reduced migration between versions of Ikasan, reduced complexity in legacy code base
* The UI should be easy to use, intuitive, dumb down usage for junior / mid-tier devs
* Abstracted to be driven from the content of a meta pack
* Support for multiple simultaneous metapacks

## Epics / major stories

Core
* Extract the 'core' part into standard maven1 project, build into jar and expose in repo accessible by project (CAUTION - resource restrictions in official Ikasan Github)
* Create builder / Maven integration for non-UI code regeneration, integrate into maven
* Expand out model.json to accommodate 'code hooks' and any non-standard attributes e.g. meta-pack version (organic, maybe driven by needs of UI)
* Parameterize meta pack version in build
* Explore DB driven configuration

Meta pack(s)
* Migrate remaining part done 2023 components
* Remove old lookup mechanism
* Verify Exception resolver
* Add 'advanced config' tag for use in UI
* Add in all Ikasan standard components to meta pack with associated free marker templates
* Split out metapack with its unit tests into seperate module with dependent on code
* Add in aggregate components with associated free marker templates
* Add metapacks for Ikasan V4.x

UI
* Code to support user implemented and user supplied classes - integration into model.json
* Validate current methods to run from UI
* Component deletion handling
* Debug mode, Message flow debug using wiretap
* Robustness tests
* Integrate 'advanced config' concept to reduce noise in UI
* Support archetype to build the project, maybe partially implemented projects.
* Sort out icons with correct scalling and transparency to support standard Intellj themes
* Update UI look and feel for all modes of Intellij themes
* Eclipse, Vaaden, Javascript based IDE
* Data exchange / XML mmapping of payload
* Parameterise meta pack version in model startup
* Explore DB driven configuration

CI
* Determine how / what output from the build process are integrated into standard M processing.
* Solution for artefact storage issue

Other
* Videos
* Help wiki
* Demos
* Jira ?
* Public Forum for self-help (stack overflow sub-site)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Get familiar with the [template documentation][template].
- [ ] Adjust the [pluginGroup](./gradle.properties), [plugin ID](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [ ] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).BASIC
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the `PLUGIN_ID` in the above README badges.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
This Fancy IntelliJ Platform Plugin is going to be your implementation of the brilliant ideas that you have.

This specific section is a source for the [plugin.xml](/src/main/resources/META-INF/plugin.xml) file which will be extracted by the [Gradle](/build.gradle.kts) during the build process.

To keep everything working, do not remove `<!-- ... -->` sections.
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "IkasanStudio"</kbd> >
  <kbd>Install</kbd>

- Manually:

  Download the [latest release](https://github.com/davidhilton68/IkasanStudio2/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].