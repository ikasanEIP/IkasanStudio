WELCOME TO IKASAN STUDIO
========================

This project was created using the Maven archetype for studio with the following configurations:

* artifactId: $rootArtifactId
* groupId: $groupId
* package: $package
* version: $version

'generated' Submodule
=====================

The code in the generated submodule will be automatically updated by the Ikasan Studio Designer, it is unlikely you
will need to edit these files directly.

'user' Submodule
================

The 'user' submodule holds the users's code, and can be edited directly. There wre times when Studio can auto generate
code stubs into the 'user' submodule but it will only do so if permitted by the user.

Getting Started
===============

To get started, select the Ikasan Studio panel on the far right of the IDE. Choose the metapack you wish to base this
module on then click the 'click here' button.

If you can't see the Ikasan Studio designer window, or require further explination, please refer to the online
documentation for Ikasan Studio.

Logging from your components
============================

New custom Consumers, Converters, Brokers and Producers include an SLF4J logger and
examples inside their methods. The Generic Producer logs arrivals at DEBUG; other
examples are commented out and can be uncommented to try them. SLF4J is
a logging facade; these examples use the application's existing logging backend
and do not require you to add another binding or select a particular backend.
Existing developer-owned classes are preserved when Studio generates code.

Use parameterised messages, for example LOG.debug("Dispatching event {}", identifier).
Prefer a safe event reference or a count to the complete payload, credentials or
personal data. Use DEBUG for development diagnostics, INFO for useful operational
milestones, WARN for unexpected recoverable conditions and ERROR for failures.
When handling an exception, pass it as the last argument to retain the stack trace.
Let failures that Ikasan should handle propagate; avoid logging and rethrowing the
same failure at every component, which duplicates the framework's error reporting.

To see debug messages with the generated Spring Boot application:
1. Open Run > Edit Configurations and select the module's Application configuration.
2. Add this program argument, replacing com.example with your component package:
   --logging.level.com.example=DEBUG
3. Run or Debug the module and inspect its IntelliJ Run or Debug console.
   Remove the argument when you no longer need debug output.

This avoids editing Studio-owned generated application.properties. If you supply
custom logging configuration, its appenders determine where output is written.
The Logging Producer logs as a step in a flow; it is not required for logging from
your own component code.
