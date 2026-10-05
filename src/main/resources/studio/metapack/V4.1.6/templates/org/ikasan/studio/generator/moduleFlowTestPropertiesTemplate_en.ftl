# Maximum wait for each expected delivery or component-path assertion (positive whole seconds).
# Increase on slow CI agents; successful delivery returns immediately. Default: 10.
test.delivery.timeout-seconds=10

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
# This developer-owned file is normally preserved. Selecting the local FTP option enables it with a backup.
# After model changes, consult generated application.properties for new property keys.
#
# Available configured component properties (keys only; supply your test values):
<#list modulePropertyKeys as key>
# ${key?j_string}=
</#list>

# JMS tests: ModuleJmsTestConfig uses ActiveMQ. Replace that class for other providers.
# Set the SAME isolated broker URL in the flow's connection.factory.jndi.provider.url override.
# Choose dedicated test queues in the flow's destination overrides; never reuse production queues.
# A common embedded JMS broker is selected through generated test wiring when available.
# Override test.jms.broker-url explicitly for an external or alternative test broker.
# test.jms.broker-url=vm://studio-flow-tests?broker.persistent=false&broker.useJmx=false
# test.jms.username=
# test.jms.password=
# Generated wiring supplies the broker default; explicit values here are preserved.

# New Generic Consumer sample implementations support per-class polling delays in milliseconds.
# Replace example.MyConsumer with the fully qualified implementation class printed in its @Value keys.
# Default initial and repeat delays are both 60000 (one minute). Example test values:
# studio.sample-consumer.example.MyConsumer.initial-delay-ms=500
# studio.sample-consumer.example.MyConsumer.poll-delay-ms=2000
# This changes timing only: tests must still supply suitable expected payloads and delivery assertions.
# Keep the repeat delay longer than the standard test's one-second idle check, or use a custom scenario.
# Existing developer-owned consumers must be updated manually before these settings take effect.

# Deterministic input for sample Generic Consumers: submitNow supplies each test batch directly.
# Remove a setting if not needed, or set false to test automatic polling (and adapt supplyInput).
# Generated wiring supplies current defaults; an explicit false here takes precedence.
# Generated wiring enables fixture input for current sample consumers by default.
# To test automatic polling, override the current class explicitly:
# studio.sample-consumer.example.MyConsumer.fixture-input-enabled=false

<#if ftpEndpoints?has_content>
# Optional real, isolated FTP server (plain FTP only, not SFTP/FTPS).
# Enable to override ALL metadata-declared FTP endpoints in this module for this test context.
# A fresh loopback port and temporary home are allocated per test and removed on close.
# All FTP source/output directories become / within that home; other endpoint settings remain unchanged.
# test.ftp.enabled=true
# test.ftp.username=ikasan
# test.ftp.password=ikasan
# An absent password uses the username; blank credentials are rejected. These credentials are for this disposable server only.
# Covered endpoints:
<#list ftpEndpoints as endpoint>
# ${endpoint.name?replace("\n", " ")?replace("\r", " ")}
</#list>
# Seed files / assert delivery via context.getBean(LocalFtpTestServer.class).root().
# For an existing external test server leave test.ftp.enabled absent or false and set endpoint keys above.
</#if>

# FTP consumer input filenames come from the resource paths in the test class.
# Their basenames must match the consumer filenamePattern.

# Local test SMTP server: loopback only, dynamically allocated port, fresh inbox per test.
# Overrides mail endpoint connections in this test application; no email is forwarded.
# Enable through Generate Flow Test or set true here. Uses test.delivery.timeout-seconds.
test.smtp.enabled=false

# Local test FTP consumer: accept complete fixture files immediately (production default is 120 seconds).
# Non-negative seconds; use a positive value only when deliberately testing file-age filtering.
test.ftp.consumer.min-age-seconds=0

# Local test SFTP server: loopback, allocated port, trusted temporary host key; no installed server needed.
# Generate Flow Test enables this when the local test SFTP option is selected.
test.sftp.enabled=false
# Test-only credentials; no personal SSH keys are used.
test.sftp.username=ikasan
test.sftp.password=ikasan
# Accept complete fixture files immediately; set a non-negative age only to test age filtering.
test.sftp.consumer.min-age-seconds=0
