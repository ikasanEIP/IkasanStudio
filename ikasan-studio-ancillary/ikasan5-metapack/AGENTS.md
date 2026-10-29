# External Ikasan 5 pack authoring

This directory is an independently buildable Maven project. Keep its POM free of parent
or relative-path dependencies on the Studio checkout. Resources under src/main/resources
are the complete distributable pack; tests must not leak into its JAR. Test with mvn verify.
Use Studio's published/local test kit only in test scope. The resource-only authoring build
uses Java 17; generated applications target Java 25 and the exact released BOM in metapack.json.
Keep POM and manifest pack revisions aligned. Preserve the pack ID for existing models;
use a distinct ID when adapting this project for another runtime/library. Advance the pack
revision when changing published content. Never change business expectations or suppress
rollback diagnostics to claim runtime readiness. See README.md for the installation demo.
