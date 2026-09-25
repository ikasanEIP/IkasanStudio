# Shared connection settings for this module's flow tests (UTF-8).
# Review using LOCAL_TEST_ENVIRONMENT.md. Uncomment only the overrides you need.
# Commented/absent keys retain the application's existing configuration, NOT isolated defaults.
# The whole Spring context loads; MANUAL flows do not prevent startup bean connections.
# Use test endpoints only. No credentials or live connection values are copied from the model.
# Java .properties syntax applies: prefer / for paths or double each backslash.
# Spring resolves environment placeholders in values, for example:
# your.connection.password=${r"${"}TEST_PASSWORD}
# (Replace the example key with your actual property key.)
#
# Precedence: application configuration < this file < flow-specific Java overrides
# < enforced random server port, unique in-memory H2 and MANUAL startup settings.
# Keep temporary directories in the individual flow tests, not here.
# This developer-owned file is created once and never overwritten by Studio.
# After model changes, consult generated application.properties for new property keys.
#
# Available configured component properties (keys only; supply your test values):
<#list modulePropertyKeys as key>
# ${key?j_string}=
</#list>

# JMS tests: ModuleJmsTestConfig uses ActiveMQ. Replace that class for other providers.
# Set the SAME isolated broker URL in the flow's connection.factory.jndi.provider.url override.
# Choose dedicated test queues in the flow's destination overrides; never reuse production queues.
# test.jms.broker-url=vm://studio-flow-tests?broker.persistent=false&broker.useJmx=false
# test.jms.username=
# test.jms.password=
# Existing files are preserved: add these keys manually if this file predates JMS support.
