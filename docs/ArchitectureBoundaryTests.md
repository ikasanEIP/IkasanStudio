# Architecture Boundary Tests

## What ArchUnit is

[ArchUnit](https://www.archunit.org/) is a Java testing library that inspects compiled bytecode and lets a project express architectural decisions as executable test rules. It can identify dependencies through constructors, method calls, fields, inheritance and generic types, rather than relying only on source-text searches or package naming conventions.

Ikasan Studio uses ArchUnit as a test-only dependency. It has no effect on the shipped plugin or generated Ikasan modules.

## Why Ikasan Studio uses it

Ikasan Studio combines several kinds of code with very different responsibilities:

- version-neutral domain models and generation logic;
- Ikasan runtime integration clients;
- Swing presentation code;
- IntelliJ Platform adapters using PSI, VFS, execution and project APIs.

Java's package system communicates these intended boundaries but does not enforce them. A convenient import can therefore introduce a dependency in the wrong direction without producing a compiler error. Such dependencies make core logic harder to test, can introduce hidden IntelliJ lifecycle or threading requirements, and can leak state between simultaneously open projects.

The boundary tests turn the important dependency rules into build failures. Their intention is to:

- keep the core usable and testable without an IntelliJ runtime;
- keep the persisted domain model independent of storage and presentation adapters;
- keep integration clients independent of UI and IntelliJ concerns;
- concentrate platform-heavy operations in dedicated IntelliJ adapters;
- prevent global fields from retaining project-specific state;
- stop new architectural debt while existing transitional dependencies are removed incrementally.

## Current rules

The executable rules live in [`ArchUnitBoundaryTest`](../src/test/java/org/ikasan/studio/architecture/ArchUnitBoundaryTest.java).

### Core independence

Classes under `org.ikasan.studio.core..` must not depend on:

- `org.ikasan.studio.ui..`;
- `org.ikasan.studio.intellij..`;
- `com.intellij..`.

Dependencies should point from IntelliJ and UI adapters towards core abstractions, never from core towards the adapters.

### Domain-model independence

Classes under `org.ikasan.studio.core.model..` must not depend on persistence or view adapters. The model is the version-neutral source of truth and should describe Ikasan concepts, not how those concepts are stored or displayed.

Serialization configuration belongs under `core.persistence..`. View handlers and other presentation state belong under `ui..`.

### Integration independence

Classes under `org.ikasan.studio.integration..` must not depend on UI or IntelliJ packages. An Ikasan HTTP or runtime client should be usable in an ordinary unit test without creating an IDE project or Swing component.

### IntelliJ API containment

Direct dependencies on PSI, VFS and execution APIs belong under `org.ikasan.studio.intellij..`.

A finite migration list currently permits named UI classes that already use those APIs. This is a ratchet, not a general exception:

- new classes must not be added merely to make a failing test pass;
- prefer introducing a focused adapter under `intellij..`;
- remove an entry as soon as its platform work has moved behind an adapter;
- nested classes are covered by the same entry as their owning class.

The source code of `ArchUnitBoundaryTest` is the authoritative list. Keeping it there makes every exception visible during review and prevents a package-wide exemption from hiding new dependencies.

### Global project-state protection

Static fields must not retain IntelliJ projects, IntelliJ modules, Studio modules, virtual files, filesystem paths, files or classloaders. The rule examines generic type arguments as well as raw field types, so constructs such as `static Map<Project, ...>` are also rejected.

Project state should normally live in a project-level IntelliJ service and be disposed with that project. Static constants and genuinely immutable, project-neutral infrastructure remain acceptable.

## Why production bytecode is imported explicitly

The test imports `build/classes/java/main` rather than scanning every matching package on the test runtime classpath. IntelliJ's test instrumentation produces additional copies of test classes whose locations are not consistently recognised by generic test-exclusion filters. Importing the production output explicitly prevents test fixtures from being mistaken for production dependencies.

This also means the test must run after `compileJava`, which Gradle's normal `test` lifecycle already guarantees.

## Running the tests

Run only the ArchUnit boundaries with:

```shell
./gradlew test --tests org.ikasan.studio.architecture.ArchUnitBoundaryTest
```

Run them as part of the complete verification suite with:

```shell
./gradlew cleanTest test
```

The existing source-oriented tests in `src/test/java/org/ikasan/studio/architecture/` remain useful for conventions that are clearer to check directly in source. ArchUnit complements those tests by checking the dependency graph produced by the compiler.

## Responding to a violation

When a rule fails:

1. Read the first dependency reported by ArchUnit and identify which class introduced it.
2. Confirm whether the dependency direction is genuinely required or is merely convenient.
3. Prefer moving platform work into a focused adapter or introducing a small core-facing interface.
4. Keep data passed across the boundary platform-neutral where practical.
5. Add or update behavioural tests for any extracted adapter.
6. Remove obsolete migration-list entries as part of the same change.

Do not weaken a package rule or add a broad exclusion to silence a failure. If an incremental exception is unavoidable, it should name the exact class, explain the migration need in code, and create no permission for unrelated classes.

## Evolving the architecture

These rules protect the current intended direction; they are not a claim that the package structure is finished. Further rules should be introduced in small, compiling steps when the desired boundary is understood and the production code can satisfy it.

Good candidates include narrower ownership rules for generation, settings, runtime monitoring and harness processes. Package-cycle rules should be added only after the corresponding existing cycles have been deliberately removed; recording a large permanent cycle exemption would provide little protection.

The guiding principle is that each rule should be strict enough to prevent regression, focused enough to produce an actionable failure, and honest about any remaining migration work.
