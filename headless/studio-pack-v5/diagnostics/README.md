# V5 file rollback experiment — 14 October 2026

`file-rollback-reconnect.patch` is a candidate **Ikasan runtime** change, not a Studio template
change. It targets `DeliverFileCommand` in the reference checkout at commit
`811a14b1ee7c202868d4bfbcac505d5da6a5565e`. The patch applies cleanly there; that repository
has not been modified.

## Finding

The ordinary FTP and SFTP commit-failure probes reached rollback with a closed client. Their
completion callback reported ROLLED_BACK and no final output existed, but Narayana reported
`ARJUNA016031` / `FINISH_ERROR`. Directory access failed with `Connection is not open` or
`Pipe closed`. The existing commit path explicitly ensures a connection; rollback did not.

The candidate ensures a connection before rollback operations, restores the working directory
in a finally block (including when the temporary file is absent), and disconnects in a finally
block. It does not change the exception policy or suppress transaction diagnostics.

## Experiment and reproduction

1. Export the rollback fixtures using `:studio-pack-v5:generateRollbackFixtures` in the headless
   build. Copy the `ftp-output` and `sftp-output` applications to a disposable directory, excluding
   build output, persistence and transaction-store directories.
2. In a disposable copy of the reference runtime source, apply this patch. Copy the resulting
   `DeliverFileCommand.java` into each application's `src/main/java/org/ikasan/connector/basefiletransfer/outbound/command/`.
   This shadows that dependency class for the experiment only; do not install or publish it.
3. Run each copied application's Maven `test` under Java 26 against the same V5 snapshot dependencies.
   Keep the fixture's strict transaction-log assertion enabled.

Both scenarios passed in `/tmp/studio-v5-file-rollback-experiment`; the local execution log is
`/tmp/v5-file-rollback-experiment.log`. Each checked failed-transaction output absence, rollback
completion, clean transaction diagnostics, explicit retry, later delivery without restart,
idle RUNNING state, STOPPED teardown and transport cleanup. The unmodified snapshot fixtures
remain separate and failing, so the experiment cannot silently turn the release gate green.

## Review still needed

The snapshot dependency graph is mutable and differs from the reference source baseline in some
versions. Runtime maintainers should review connection ownership and add runtime-level tests for
missing temporary files, reconnect failures, directory restoration failures and concurrent use.
This experiment does not establish crash recovery, network-outage behavior or file-consumer rollback.
The separate JMS XA resource-end warning remains unresolved.

## JMS resource-end warning: reduced reproducer

The exported `JmsXaLifecycleTest` now reproduces `ARJUNA016129` without starting an Ikasan flow,
consumer or Spring application context. Two separate XA connections/sessions, each sending one
message in a Spring-managed Narayana transaction, are enough to reproduce the warning during
rollback. Two sends on one session pass. Receive/send variants fail whether the receiving session
is closed before rollback or afterward. These are controls against the same resolved snapshot
dependencies, not a proposed production workaround.

Run the export command, then execute the JMS Maven fixture with
`-Dtest=JmsXaLifecycleTest#twoSendSessionsRollback`. The corresponding passing comparison is
`-Dtest=JmsXaLifecycleTest#twoSendsOnOneSessionRollback`. The fixture supplies an embedded broker;
no external JMS setup is required. Both assert no rolled-back output is delivered, and the failing
case fails only when checking transaction diagnostics. The final control run is recorded locally
in `/tmp/v5-jms-controls-seven.log`: seven tests, four passed, three failed, none skipped.

The captured call chain is `TransactionContext.end` → Narayana `TransactionImple.doEnd` /
`endAssociation` → `XAResourceRecord.topLevelAbort`, with the ActiveMQ transaction ID already null.
This localizes the reproduction but does not assign fault or justify suppressing the warning.
No changes were made to the reference runtime, ActiveMQ or Narayana.
