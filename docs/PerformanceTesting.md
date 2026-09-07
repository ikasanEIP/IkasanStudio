# Performance measurement

Run the opt-in platform benchmark with JDK 17:

```sh
./gradlew performanceTest --no-configuration-cache
./gradlew performanceTest --no-configuration-cache -PperformanceProcessors=2 -PperformancePollDelayMs=1000
```

The default deterministic fixture contains 40 flows and 480 components, including FTP, JMS, local-file, event and email connectors. Each run writes `build/reports/performance/measurements.json` (raw samples, median, nearest-rank p95, environment and lifecycle observations), plus a timestamped `project-40x12-*` directory containing the model and generated Maven project. Open its root `pom.xml` in an isolated IDEA project, then open Studio after import. Connector values are test data; this is not a configured runnable integration deployment. Dependencies may need resolving on first import. The export is regenerated, not checked in.

For larger workloads and more meaningful tail measurements:

```sh
./gradlew performanceTest --no-configuration-cache -PperformanceFlows=100 -PperformanceComponents=20 -PperformanceSamples=30 -PperformanceWarmups=5
```

Defaults are seven measured samples after two warmups, a 1440×900 canvas, and a 250 ms loopback server delay. Preserve each JSON before another run overwrites it. Compare runs on the same idle host with the same IDEA/JDK, theme, scaling, viewport, heap and fixture. The benchmark intentionally has no machine-dependent timing assertions and is excluded from normal `test` runs.

## Recorded local results

Recorded on 7 September 2026, Linux, JDK 17.0.12, IDEA 2024.3.7. Workstation JVM exposes 16 processors; the constrained JVM exposes two. Both use a 2 GiB maximum heap. Values below are median / p95 milliseconds. Full raw data is in [workstation](performance/workstation-40x12.json) and [constrained](performance/constrained-40x12.json).

| Operation | Workstation | Constrained JVM |
| --- | ---: | ---: |
| model.load | 6.50 / 9.50 | 7.66 / 9.72 |
| model.save.atomicWithBackup | 16.99 / 32.67 | 18.27 / 42.45 |
| generation.templates | 151.80 / 198.92 | 164.99 / 217.11 |
| editor.open.readyProject | 20.62 / 24.75 | 23.21 / 26.63 |
| canvas.layoutAndPaint | 67.72 / 76.56 | 48.32 / 90.56 |
| canvas.repaint.cachedLayout | 42.60 / 47.13 | 35.16 / 48.33 |
| properties.selectionChange | 7.37 / 13.70 | 8.53 / 17.20 |
| drag.hoverValidation.lastFlow | 0.23 / 0.38 | 0.22 / 0.51 |
| drag.moveGesture | 1.22 / 1.47 | 0.74 / 0.81 |
| generation.firstCommit | 9893.57 / 9893.57 | 10231.96 / 10231.96 |
| generation.regenerate.unchanged | 503.62 / 564.75 | 444.12 / 579.99 |
| polling.slowServer.wall | 297.06 / 302.21 | 1043.14 / 1044.80 |
| polling.edtHeartbeat | 1.68 / 14.12 | 1.10 / 1.11 |

The constrained profile uses `-XX:ActiveProcessorCount=2` and a one-second server delay. It limits JVM parallelism; it does not emulate physical CPU throttling, remote graphics or an actual VDI. Actual VDI results remain to be collected on that host.

## What the measurements cover

- Editor opening measures construction of a ready-project editor; disposal is outside the timing. Cold IDE startup, Maven import, indexing, tab restoration and first visible frame are excluded. Canvas layout/paint is measured separately with real Swing widgets rendered into an image.
- Model load includes file reading and deserialization. Save includes serialization, validation, atomic replacement and backup. Background timings include task scheduling and the test's EDT event pumping.
- Property selection exercises the real properties panel. Drag hover checks a component against the last flow; the move gesture dispatches real mouse events and releases at its original location. It measures a no-op drop, not a committed structural edit and its generation.
- Template rendering is separate from full generation through the synchronizer, transaction, filesystem and PSI formatting. First commit is one observation, so its p95 is not a statistical tail estimate. Unchanged regeneration still renders and validates output before deciding which writes can be skipped.
- Polling uses the real HTTP client against a delayed loopback server returning every fixture flow. The heartbeat measures EDT dispatch while a background request is outstanding; it does not measure remote desktop input latency or the complete production polling service scheduler.
- Memory reports whole-JVM used heap after explicit GC plus weak-reference reachability. Editor, canvas and properties references cleared in both recorded runs; the project model intentionally remains available for reopening. Heap differences are noisy and are not exact retained editor bytes. Use an IDE heap dump and dominator analysis for precise ownership and repeated-open leak investigation.

## Changes driven by measurement

Pending generation requests now merge their required scope. A burst of property requests stays property-only; unrelated flow changes still promote to a full generation when necessary. Existing supersession checks discard obsolete work. This does not introduce a timer-based debounce or interrupt an already-running commit.

Formatted artifacts remember hashes of both their template output and persisted content. Unchanged regeneration therefore avoids rewriting and reformatting an artifact merely because IDEA formatting differs from the template text. Changes to either template output or disk content invalidate reuse. The cache holds hashes on virtual files, not editor references. Tests verify reuse and invalidation.

Module painting reuses the dimensions established by the canvas dirty flag instead of laying out every flow again on every repaint. Flow painting remains active off screen because it positions endpoints used by cross-flow connectors. Preferred size triggers revalidation only when dimensions change; Swing continues to coalesce repaint requests. Pixel-comparison tests cover cached painting at two scroll positions.

The earlier [baseline](performance/baseline-40x12.json) recorded cached repaint median 42.61 ms; the current workstation run records 42.60 ms. These short runs are directional evidence, not a stable performance guarantee. The baseline editor timing included disposal and its polling response was smaller, so those values are not directly comparable.

## Remaining interactive measurements

Initial generation is still substantially slower than unchanged regeneration and includes synchronous PSI work. These results do not establish responsiveness during that initial commit.

On the actual VDI, run the same benchmark and preserve its JSON alongside host/IDE/scaling details. Open the exported project and use IDEA's CPU and memory profiling tools to capture cold editor opening, a committed drag-and-drop, rapid property edits, scrolling, and polling against the real slow module. Repeat editor open/close before capturing a heap dump. Record visible frame/input latency and EDT stalls separately from the headless benchmark. Repeat in light and dark themes and normal display scaling. These interactive and actual-VDI measurements are outstanding; the local profiles provide a repeatable starting point.

## Verification

Both recorded benchmark profiles completed successfully. The full Gradle correctness suite passed: 646 tests, zero failures, errors or skips. The exported workstation fixture contained 229 non-empty files and four parsable POMs. Maven dependency resolution and an interactive IDEA session were not part of this verification.
