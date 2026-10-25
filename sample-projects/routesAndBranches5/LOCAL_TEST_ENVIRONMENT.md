# Local test environment

Paste your local test settings into the block below, then ask the AI to use them.
Fill only the services and values you need. Local test passwords may be written directly here;
environment-variable references are optional. No separate configuration file is required.
Studio creates this file only if missing and preserves your edits.

## Before running local demos

Studio's FTP and SMTP harnesses are started explicitly in the IDE; a closed port may mean the
harness is not running yet. For FTP, right-click a supported local FTP component and choose
**Start Test FTP Server**, then **Show Test FTP Server Details**. For mail, right-click an Email
Producer or its endpoint and choose **Start Test Mail Server**, then **Show Test Mail Server Details**.
Check the actual addresses and credentials there. FTP paths are relative to the server's visible
root `/`; a custom directory must exist inside the harness root. Start required harnesses before
**Run module**. The FTP harness does not provide SFTP; SFTP needs your configured SSH server.
The AI should ask for these startup steps when it cannot operate the IDE, keep flows AUTOMATIC,
and report verification pending until actual delivery is tested. Do not disable flows to hide an
unstarted harness, invent credentials, or replace supplied endpoints just because a port is closed.

## My settings

Use `name=value`, one per line. Lines beginning with `#` are comments. Everything after the
first `=` is the value, including further `=` characters; do not add shell quotes or execute
this block as a script. Blank entries and commented examples mean unspecified, not empty
runtime values. Replace or remove the blank entries as convenient.

```text
# SFTP: host, account, existing remote directory, and ONE authentication method.
sftp.remoteHost=
sftp.remotePort=
sftp.username=
sftp.directory=
sftp.password=
# For key authentication, omit password and supply these existing local files instead:
# sftp.privateKeyFilename=/home/your-user/.ssh/id_rsa
# sftp.knownHostFilename=/home/your-user/.ssh/known_hosts

# FTP (optional)
ftp.remoteHost=
ftp.remotePort=
ftp.username=
ftp.password=
ftp.directory=

# Local SMTP capture server (optional)
smtp.mailSmtpHost=
smtp.mailSmtpPort=
smtp.from=
smtp.toRecipient=
# smtp.mailSmtpUser=
# smtp.mailPassword=

# JMS (optional; omit when the brief uses Studio's embedded broker)
# jms.connectionFactoryJndiPropertyProviderUrl=
# jms.connectionFactoryJndiPropertyFactoryInitial=
# jms.connectionFactoryName=
# jms.destinationJndiName=

# Optional service commands or extra component overrides:
# sftp.startCommand=
# sftp.consumer.cronExpression=0/5 * * * * ?
# sftp.producer.overwrite=false
```

## How the AI should use this

- Reread this file whenever test settings are needed. Filled-in values are the developer's
  supplied local-test configuration; no READY flag or completed checklist is required.
  An explicit NOT_CONFIGURED or NOT_USED status still excludes that service from testing.
- Apply these values as overrides to the selected component catalogue. Omitted optional
  properties retain existing values on existing components, or catalogue defaults on new
  components; otherwise leave them unset. Do not ask the developer to enumerate every property.
  Never invent credentials or treat a sample localhost endpoint as a verified service.
- `sftp.directory` and `ftp.directory` mean the producer's `outputDirectory` and consumer's
  `sourceDirectory`. Use the same server-visible directory for a pair. It must exist and the
  account must have the required permissions. Do not silently substitute `/` or invent a directory.
- Prefixes select the transport; `consumer.` and `producer.` select an optional role-specific
  override. Remaining names are catalogue property names, not automatically loaded runtime keys.
  Accept equivalent clearly labelled name/value pairs; clarify ambiguous or conflicting values.
- SFTP password authentication needs host, username and password. Key authentication instead
  needs an existing readable private key and known-hosts file. Resolve `~` to the actual user's
  home before submitting a path. A public key or TLS certificate is not an SSH private key.
  In the bundled connectors, password takes precedence over a supplied private key: do not
  accidentally select password authentication when key authentication was requested.
- Use the selected pack's numeric port default if omitted. Ask only for missing required
  connection/authentication details with no usable default. Check conditional requirements for
  the chosen mode, rather than requesting both password and key authentication.
- The developer permits using supplied local-test passwords in the configuration needed for
  this task, including Studio proposals/model properties when supported. Do not repeat them in
  chat or logs. Private-key contents are never needed; use the supplied filename.
- For a requested working demonstration, configure matching filenames, payload conversion,
  polling and consumption handling from the brief and catalogue. These are implementation
  choices, not extra environment questions. Check defaults together: a sender/receiver pair
  must not consume temporary uploads or repeatedly process its own archived files.
- A request to build and test against these local services authorises the necessary test
  connections and creating/consuming this demo's own files/messages. Preserve unrelated data.
  Additional restrictions can be written below. Do not widen scope or start supplied commands
  unrelated to the requested task.
- Apply model changes through Studio. Verify actual transfer, receipt, idle readiness and later
  delivery using normal Run module configuration. Keep ready ESB flows running. Report missing
  setup or failed checks honestly; filled-in settings alone do not prove a working service.

## Optional notes and references

Add restrictions, startup commands, network/container context or additional services here only
when useful. Add `/LOCAL_TEST_ENVIRONMENT.md` to the root `.gitignore` for machine-specific values.

<!-- Optional alternatives to literal local-test passwords:
ftp.password=env:IKASAN_TEST_FTP_PASSWORD
sftp.privateKeyFilename=env:IKASAN_TEST_SFTP_KEY_FILE
Set variables in IntelliJ Run > Edit Configurations > Environment variables.
An env: reference names a variable; it is not a literal password or filename. Verify the
application's supported binding. In loaded Spring configuration, a supported property can use
${IKASAN_TEST_FTP_PASSWORD}. Do not invent a property key and assume it is automatically bound.
-->
