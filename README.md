# IkasanStudio2

The easiest way to view the plugin just now is to load it up into the IDE, Intellij 2019.3.5 was the last working 
version (when I tried a refresh in September), subsequent versions seem to have 'known but not fixed' issues with
the template / gradle.

Its a Gradle/Kotlin build, JetBrains prefer it that way, I have learned the hard way over many months to just accept
however JetBrains want to do it, otherwise its just a huge bundle or late nights with spurious issues.

The run configuration is Gradle and :runIde

As part of the template package that JebBrains put together, there is a full pipeline including compatibility tests
with named versions of the Intellij IDE, I have only a few defined just now (didn't focus on that part for now).

Ignore anything with shed in the title, or in the package its just stuff I want to keep since it might come in handy soon.

Enjoy.

![Build](https://github.com/davidhilton68/IkasanStudio2/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml) and [sources package](/src/main/kotlin).
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [ ] [Publish a plugin manually](https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/publishing_plugin.html) for the first time.
- [ ] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
This Fancy IntelliJ Platform Plugin is going to be your implementation of the brilliant ideas that you have.

This specific section is a source for the [plugin.xml](/src/main/resources/META-INF/plugin.xml) file which will be extracted by the [Gradle](/build.gradle.kts) during the build process.

To keep everything working, do not remove `<!-- ... -->` sections. 
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "IkasanStudio2"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/davidhilton68/IkasanStudio2/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

## Known Issues
- [ ] Sometimes the port for Ikasan is not free - Use  netstat -ano | findstr 8090 / taskkill /F /PID  pid
- [ ] Sometimes we get the error "PSI and index do not match", workaround - File menu > Invalidate caches and restart

