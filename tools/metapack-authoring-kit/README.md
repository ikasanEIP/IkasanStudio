# VDI meta-pack authoring kit — maintained source

This is the **tracked** source of a transferable public authoring kit. Commit this directory,
the maintained Maven example and the Studio changes it depends on. Generated ZIPs, artifact
repositories, logs and reports live under ignored `build/metapack-authoring-kit/`.
Do not add private shared-component sources, JARs, descriptors, configuration or credentials here.

For GitHub-only transfer to a restricted VDI, use the two prebuilt ZIPs in
[`downloads/metapack-authoring-kit/`](../../downloads/metapack-authoring-kit/README.md).
Download both and extract into the same directory. These are intentional versioned distribution
copies; normal kit builds still write only to `build/`. The single combined kit exceeds GitHub's
100 MiB regular Git file limit.

The kit starts from `ikasan-studio-ancillary/example-metapack`, targets Ikasan **4.1.6 / JDK 17**,
and assembles a complete `SharedIkasan4` resource pack from a pinned V4 baseline. The examples
are public teaching components, **not** implementations of any organisation's shared library.
It ships public engine/test-kit sources, schema/reference documents, Copilot prompts, an isolated
Maven dependency repository, inventory and build receipt. By default it also ships the matching
local **development** plugin ZIP. Its existing plugin version is not a new Marketplace release.

## Build here, then transfer the generated ZIP

Prerequisites: Python 3.10+, an approved JDK 17, Maven 3.8.6 or newer Maven 3.x and the Studio checkout's populated
Gradle cache. Set JAVA_HOME/PATH normally. No corporate Maven settings or credentials are copied.

```sh
python3 tools/metapack-authoring-kit/build_kit.py --maven /path/to/mvn
python3 -m unittest discover -s tools/metapack-authoring-kit -p 'test_*.py'
```

The builder runs headless Gradle publication-artifact tasks, seeds **only** generator, test-kit and
V4 pack artifacts, and builds the maintained Maven example against an initially isolated Maven
repository. A loopback read-through mirror supplies only requested Maven files from the specified
artifact cache (`~/.m2/repository` by default), falling back to public Maven Central. It never copies
the entire cache or reads Maven settings/server credentials. It pins default lifecycle plugins so
Maven versions cannot silently select different plugin versions. Artifact/repository licenses remain
applicable; included JAR notices are retained. Check organisational approval for redistribution.

The builder then runs the consumer verifier **offline**, including generated Java compilation and
packaged-JAR installer validation. Failure leaves diagnostics under `build/metapack-authoring-kit/`;
it does not produce a successful final archive. Existing output directories cause a refusal rather
than being overwritten. Preserve needed output, then remove the builder-owned output directory
before rebuilding. `--without-plugin` explicitly omits the plugin, requiring a compatible Studio
installation on the VDI. Internal/private component dependencies are intentionally not in this kit.

Outputs:

```text
build/metapack-authoring-kit/
  ikasan-metapack-authoring-kit.zip
  ikasan-metapack-authoring-kit.zip.sha256
  ikasan-metapack-authoring-kit/   # unpacked public bundle
  *.log
```

Transfer the ZIP and SHA-256 by your approved route. The VDI needs Python, JDK 17 and Maven; these
executables are not bundled. Git, the Studio checkout, Ikasan source and Dashboard source are not
needed on the VDI. The generated starter and this directory can be downloaded in GitHub's repository
source ZIP: a Git clone is optional. The ready-to-use binary/dependency ZIP must be built or attached
to an approved release separately; it is not silently committed to Git.

Read the bundled README on the VDI. Keep all private adaptations there. Treat the adapted pack as
internal too: class names, property names and templates can disclose internal implementation details.

## Evidence and limits

`build-receipt.json` records the Studio commit, dirty-worktree flag, baseline/runtime/API choices,
requested dependency paths and build outcome. `inventory.json` hashes every shipped file except
itself. SHA-256 detects accidental changes; it is not a signature or proof of publisher identity.
The offline test proves the public starter's dependencies are available, **not** arbitrary future
private dependencies or runtime certification of the shared library. Test the extracted ZIP on the
VDI OS as well; current maintainer evidence is recorded in the build receipt and local log files.

To change runtime release, revise the maintained baseline/starter and rerun the entire build rather
than globally replacing version strings. There is no runtime overlay/base-pack inheritance support.
