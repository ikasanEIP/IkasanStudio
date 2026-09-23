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

1. Open the Ikasan Studio editor (Tools > Ikasan Studio > Open Ikasan Studio if it is not already open).
2. Select an Ikasan version and create your module.
3. Review and update LOCAL_TEST_ENVIRONMENT.md in the project root. Fill in the services
   you need as name=value pairs. Literal local-test passwords are accepted; environment references are optional.
   Leave unneeded fields blank; the AI uses applicable defaults and asks only for missing required details.
4. Connect AI if desired, then add flows and components. The project's AGENTS.md tells
   assistants to consult the local environment file each time test settings are needed.
   For flow tasks, the ikasan-integration-workflow skill guides implementation and verification.
   AGENTS.md links its shared file for clients without automatic skill discovery.
5. Start the required local services before Run module and verify actual delivery.

LOCAL_TEST_ENVIRONMENT.md is developer-owned. Add /LOCAL_TEST_ENVIRONMENT.md to the
root .gitignore before entering machine-specific settings. Local-test passwords may be stored here; use filenames for private keys, never their contents. Its commented examples
explain environment-variable references; it is not runtime configuration by itself.

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
