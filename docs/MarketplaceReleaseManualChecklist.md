# Ikasan Studio Marketplace release checklist

## Threading and lifecycle (release blockers)

- Run `./gradlew runIdeInternal` and exercise Studio with IntelliJ internal mode enabled.
- Keep `idea.log` open. Treat every `SlowOperations`, EDT assertion, write-action, disposed-component,
  leaked-disposable, thread-leak, and message-bus warning attributed to Ikasan Studio as a release blocker.
- Open and close the Studio editor at least ten times. Confirm there is still one canvas refresh timer, one
  initialization subscription, and no increasing background activity after each reopen.
- Start and stop Run, Debug, FTP, and mail harness activity; close the Studio editor while each is starting,
  running, and stopping; then close the project. Confirm no callback updates a disposed canvas.
- With Studio both open and deliberately closed, restart IntelliJ and confirm the editor restoration rule is
  respected and no duplicate polling, listeners, notifications, or harness nodes appear.
- Open two projects using different meta-packs. Start a module and harnesses in each, close one project, and
  verify the other project continues independently.
- Trigger Maven import/indexing while Studio is opening. Resize and interact with the editor during indexing;
  the UI must remain responsive and initialization must resume or show a recoverable state.
- Repeat the common workflow with network endpoints unavailable and with startup tasks cancelled. No expected
  connection failure should become an uncaught IDE exception.

## External processes and test harnesses

- On Windows, macOS, and Linux, start and stop the FTP and mail harnesses twice. Confirm repeated actions are
  idempotent, the mail terminal output remains governed by IntelliJ terminal scrollback, and closing the project
  stops only harnesses launched by that project.
- Occupy the configured FTP, SMTP, and mail UI ports with external processes before clicking Start. Confirm the
  notification identifies the conflicting endpoint and Studio never terminates those processes.
- Start the mail harness, restart IntelliJ abnormally, and reopen the project. Confirm the listener is shown as
  externally owned and Stop Harnesses refuses to terminate it. Stop it using the original OS/terminal owner.
- Exercise an external SFTP server before and after an IDE restart. Studio must probe/use it without claiming
  ownership, logging its password, passphrase, or private-key path, or attempting to terminate sshd.
- Move or rename the project directory, reopen it, and confirm FTP test data is created beneath the new
  `test-data/ftp` directory. Confirm no notification, log, model, or generated file contains a developer-specific
  absolute path unless that path was explicitly configured as application data.
- Force an owned mail process to exit unexpectedly and confirm its canvas status clears; Start Harnesses must be
  able to launch it again.

## Automated gates

- Run `./gradlew cleanTest test`.
- Run `./gradlew buildPlugin verifyPlugin`.
