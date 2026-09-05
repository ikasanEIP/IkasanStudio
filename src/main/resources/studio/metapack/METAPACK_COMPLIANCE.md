# Ikasan Studio Meta-Pack Compliance

This document defines the compatibility contract every Ikasan Studio meta-pack must satisfy. A meta-pack is
one tested Ikasan dependency platform; it is not a collection of independently selected library versions.

## Required manifest

Every immediate child of `studio/metapack/` must contain `metapack.json` alongside its `library/` and
`templates/` directories:

```json
{
  "schemaVersion": 1,
  "id": "V4.0.x",
  "ikasanVersion": "4.1.6",
  "javaVersion": "17",
  "dependencyManagement": [
    {
      "groupId": "org.ikasan",
      "artifactId": "ikasan-eip-standalone-bom",
      "version": "4.1.6"
    }
  ],
  "compatibilityOverrides": []
}
```

- `id` must exactly match the meta-pack directory name.
- `ikasanVersion` is the exact tested release, never a range or an `x` version.
- `javaVersion` is the Java release used by generated modules.
- `dependencyManagement` must contain the published BOM used by that Ikasan release.
- An Ikasan BOM version must equal `ikasanVersion`.
- Snapshot coordinates are allowed only in a deliberately snapshot-labelled development pack and must not be
  shipped in a Marketplace release.

Studio imports each declared BOM into generated Maven projects and writes `version.ikasan` from the manifest.
Direct `org.ikasan` dependencies are emitted without versions so Maven obtains one coherent set from the BOM.

## Component dependencies

Component and component-type metadata declare only dependencies needed when that component is used.

- All `org.ikasan` dependency versions in source metadata, when present for readability, must equal the
  manifest's `ikasanVersion`. Studio removes those versions from generated POMs.
- Do not choose a newer transitive dependency merely because it is newer. The BOM is authoritative.
- Prefer omitting versions that the BOM manages.
- A direct third-party version is permitted only when the component genuinely requires an override. Add the
  same coordinate to `compatibilityOverrides`, with the exact version and a concrete reason.
- Never add transitive repair pins merely to hide a missing or unpublished BOM. Publish/fix the matching Ikasan
  BOM, or keep the meta-pack out of release.

## Compatibility overrides

Overrides are temporary, reviewable exceptions:

```json
{
  "groupId": "example.group",
  "artifactId": "example-artifact",
  "version": "1.2.3",
  "reason": "Required because ...; remove when ..."
}
```

An override must not use a range, `LATEST`, or `RELEASE`. Remove it as soon as the corresponding BOM manages
the required version.

## Required verification

Before a meta-pack is released:

1. Run `./gradlew verifyMetaPackBoms` to confirm every BOM coordinate resolves from repositories available to
   ordinary generated projects.
2. Generate a maximal fixture module exercising every component in the pack.
3. Run Maven dependency resolution and compile/test the generated project on `javaVersion`.
4. Run Maven Enforcer dependency convergence (or inspect an equivalent dependency tree).
5. Confirm no `org.ikasan` dependency resolves to a version other than `ikasanVersion`.
6. Test a minimal module as well as the maximal fixture so optional components do not hide missing basics.
7. Run the Ikasan Studio Gradle test suite; manifest and metadata validation failures are release blockers.

The manifest is loaded and validated before Studio exposes a packaged component library. Missing manifests,
mixed Ikasan versions, conflicting explicit versions, blank override reasons, and mismatched BOM versions make
the pack unavailable rather than allowing it to generate an unpredictable project.

