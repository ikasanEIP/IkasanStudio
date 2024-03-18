# IkasanStudio2

![Build](https://github.com/davidhilton68/IkasanStudio2/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

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
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "IkasanStudio2"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/davidhilton68/IkasanStudio2/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation

Notes
Over time move some items from auto generation e,g. pom into the generation into build by IDE to speed it up.

Distinguish between the Ikansan Platform metadata (standard on per Ikasan version basis) and meta data for current model.

High level tasks
* Extract the 'core' part into standard maven project, build into jar and expose in repo accessible by project (CAUTION - resource restrictions in official Ikasan Github)
* Create builder for non-UI code regeneration, integrate into maven
* Add in all Ikasan standard components to meta pack with associated free marker templates
* Add in aggregate components with associated free marker templates
* Add 'advanced config' tag for use in UI
* Add metapacks for Ikasan V4.x

UI
* Code to support user implemented and user supplied classes - integration into model.json
* Validate current methods to run from UI
* Component deletion handling
* Debug mode, Message flow debug using wiretap
* Robustness tests
* Integrate 'advanced config' concept to reduce noise in UI
* Sort out icons with correct scalling and transparency to support standard Intellj themes
* Update UI look and feel for all modes of Intellij themes
* Videos and help wiki
* Eclipse, Vaaden, Javascript based IDE

CI
* Determine how / what output from the build process are integrated into standard M processing.