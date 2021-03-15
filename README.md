# IkasanStudio

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

![Build](https://github.com/ikasanEIP/IkasanStudio/workflows/Build/badge.svg)
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
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "IkasanStudio"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/ikasanEIP/IkasanStudio/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

## Known Issues
- Sometimes the port for Ikasan is not free - Use  netstat -ano | findstr 8090 / taskkill /F /PID  pid
- Sometimes we get the error "PSI and index do not match", workaround - File menu > Invalidate caches and restart
- Error:java: error: release version 5 not supported - 
Intellij has to be told what version of Java is required.
- java.lang.NoClassDefFoundError: javax/xml/bind/JAXBException
```
ERROR 27612 --- [ost-startStop-1] o.s.b.web.embedded.tomcat.TomcatStarter  : Error starting Tomcat context. Exception: org.springframework.beans.factory.UnsatisfiedDependencyException. Message: Error creating bean with name 'org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration': Unsatisfied dependency expressed through method 'setFilterChainProxySecurityConfigurer' parameter 1; nested exception is org.springframework.beans.factory.BeanExpressionException: Expression parsing failed; nested exception is org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'org.ikasan.web.WebSecurityConfig': Unsatisfied dependency expressed through method 'setContentNegotationStrategy' parameter 0; nested exception is org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration$EnableWebMvcConfiguration': Unsatisfied dependency expressed through method 'setConfigurers' parameter 0; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'org.ikasan.web.IkasanWebAutoConfiguration': Injection of resource dependencies failed; nested exception is org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'moduleService' defined in class path resource [org/ikasan/module/IkasanModuleAutoConfiguration.class]: Unsatisfied dependency expressed through method 'moduleService' parameter 1; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'systemEventService' defined in class path resource [systemevent-service-conf.xml]: Cannot resolve reference to bean 'systemEventDao' while setting constructor argument; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'systemEventDao' defined in class path resource [systemevent-service-conf.xml]: Cannot resolve reference to bean 'systemEventHibernateSessionFactory' while setting bean property 'sessionFactory'; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'systemEventHibernateSessionFactory' defined in class path resource [systemevent-service-conf.xml]: Invocation of init method failed; nested exception is java.lang.NoClassDefFoundError: javax/xml/bind/JAXBException
```
This is a result of the JAXB APIS being removed from the JDB after version 8. You need to add them explicitly into your pom:
```
        <dependency>
            <groupId>javax.xml.bind</groupId>
            <artifactId>jaxb-api</artifactId>
            <version>2.3.1</version>
        </dependency>
        <dependency>
            <groupId>com.sun.xml.bind</groupId>
            <artifactId>jaxb-core</artifactId>
            <version>2.3.0</version>
        </dependency>
        <dependency>
            <groupId>com.sun.xml.bind</groupId>
            <artifactId>jaxb-impl</artifactId>
            <version>2.3.1</version>
        </dependency>
```
See also - https://stackoverflow.com/questions/43574426/java-how-to-resolve-java-lang-noclassdeffounderror-javax-xml-bind-jaxbexceptio

## Help with components
EventDrivenConsumer - cant see this in anu of the standard module
## Help with incomplete components
EventGeneratingConsumer    EndpointEventProvider, ManagedEventIdentifierService
## Mick / Andrzej
* For some properties of a component, I can see we want to make environment specific (thus supported by a sprint injected property)
and others will be fixed within the code. I may have guessed incorrectly for some items but thats easy to fix / alter. 
* Properties, should they be strict dash format, camel case or dash and camel - newflow1.testftpconsumer.ftp.consumer.filename-pattern=*Test.txt
* As soon as a component declares that it needs bespoke configuration (https://github.com/ikasanEIP/ikasan/blob/3.2.x/ikasaneip/developer/docs/StandaloneDeveloperGuide.md#configuring-components), that component will need to be treated as a 'custom component' e.g. the custom converter
* Some of the property configurations seem quite advanced e.g. ManagedEventIdentifierService, ManagedResourceRecoveryManager etc - Should we 'put' these in an 'advanced' options section in order to reduce screen clutter / scare factor?