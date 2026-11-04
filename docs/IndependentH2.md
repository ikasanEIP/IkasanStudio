# Independent development H2 server

**Launch independent H2** runs a separate Java Application using the generated
`org.ikasan.studio.boot.IndependentH2` entry point. It uses the generated module's
IntelliJ module, project JDK and resolved Maven runtime classpath. The Run tool window
provides the output and normal Stop/Rerun controls; the configuration is saved in the
local workspace as **Independent H2: <project>**. This server is separate from an
application's embedded H2 database.

Regenerate an older project and reload Maven before first use. Set distinct H2 TCP
and web-console ports in the module properties and disable embedded H2 when using the
independent server. Studio opens the local console only after both servers start.
The server binds to `127.0.0.1`; use a loopback address in the JDBC URL. This launcher
is for local development, not a remote database deployment. Configure the module's
actual datasource URL and credentials as usual.

The canvas tracks actual processes across menu recreation and Studio editor reopening.
A start that fails or a process stopped from the Run window resets its controls.
Studio sends a graceful shutdown command to its own process; the generated launcher
stops both H2 services. JVM shutdown also runs this cleanup. If shutdown takes more
than five seconds, Studio offers forced termination. Closing the project also stops
its owned child process. Studio does not connect to or stop unrelated H2 servers.

Missing/conflicting runtime H2 JARs fail with an actionable execution diagnostic.
Port conflicts appear in the Run console; if web startup fails after TCP starts, the
launcher stops TCP before exiting. No H2 classes are loaded inside the IDE process.
The generated source compiles against Java 11 APIs and uses reflection for the small
common H2 server API, so it requires no additional compile-time H2 dependency.

## Dependency and data compatibility

There is no generated `generated/h2/pom.xml`, Maven exec execution or separately pinned
server dependency. H2's version follows the project's selected Ikasan dependency
management. Studio does not override the core-managed version. The inspected V3 core
baseline still specifies 1.4.200; the inspected V4/V5 sources specify 2.4.240. A real
project's resolved classpath remains authoritative. Removing the obsolete helper POMs
removes those redundant dependency declarations, but does not resolve a vulnerability
in a remaining core-managed H2 dependency. GitHub must rescan the pushed changes before
alert closure can be confirmed.

The old helper forced 1.4.200 even for newer packs. If existing development databases
were created by that old server, preserve backups and follow H2's normal export/import
migration procedure before opening them with a newer major version. Studio neither
rewrites database files nor claims to migrate their format.

Checked-in sample helper POMs have been replaced with the Java launcher. In other
existing projects, old `generated/h2` copies are unused and no longer regenerated;
remove the old helper POM after confirming it contains no custom work. Other files
such as developer query scripts and database data are left alone.

## Pack compatibility and verification

Generator API 3 removes the legacy `h2StartStopPom_en.ftl` requirement; the launcher
is supplied by the engine. Current generators continue to accept API 1 and 2 packs.
New official packs declare API 3 so older generators reject them before attempting
to read the removed template. Released V5.1.0 and ExampleIkasan5 revisions advance to
1.0.3. Saved V5 model locks require the normal explicit **Adopt Installed Meta-Pack
Revision…** workflow with its backup; locks are not bypassed.

The reproducible runtime check takes existing H2 JARs and downloads nothing:

```sh
python3 tools/verify_independent_h2.py --h2-jar /path/to/h2.jar
```

It compiles the exact generated source with `--release 11`, starts a separate JVM,
checks web/TCP access, creates and queries a database, verifies idle readiness and a
later query, stops/restarts and checks persisted data, tests normal JVM termination,
checks port-conflict rollback and verifies missing-dependency diagnostics. Use more
than one `--h2-jar` to verify the supported versions independently.
