# Ikasan Studio sample projects

These are sample applications created with Ikasan Studio. Use them to explore visual flow design, inspect generated Java and configuration, and see how developer-owned components fit into an Ikasan application. They also provide examples for manual Studio regression checks and comparisons between supported Ikasan versions.

Each folder is a separate Maven project. These applications are examples to adapt to your environment; inclusion here does not certify that every flow or test is ready to run without configuration.

## Available samples

| Ikasan 3.3.9 | Ikasan 4.1.6 | Purpose |
| --- | --- | --- |
| [allConsumerAndProducers](allConsumerAndProducers/) | [allConsumerAndProducers4](allConsumerAndProducers4/) | Consumer and producer examples: event generation to a discard sink, generic input to FTP, FTP to email, local files to a custom producer, SFTP/JMS in both directions, and scheduled input to logging. |
| [convertersAndTranslators](convertersAndTranslators/) | [converterAndTranslators4](converterAndTranslators4/) | Create and enrich an order, then transform its representation using Java objects, XML and strings. Illustrates converters, translators and custom business types. |
| [routesAndBranches](routesAndBranches/) | [routesAndBranches4](routesAndBranches4/) | Split collections into individual messages, filter messages, and route messages through branches. Illustrates how flow structure affects delivery. |

Folders ending in `4` target Ikasan 4.1.6; the other folders target 3.3.9. The matching families demonstrate similar concepts, but their implementations and test scaffolds may differ. The version in each project's `pom.xml` and Studio model is authoritative. The V3 POMs target Java 11; V4 targets Java 17. Use a JDK compatible with the selected application and dependencies.

## Open and explore a sample

1. Copy the chosen sample to a working directory if you want to experiment without changing this repository.
2. Open that sample's root `pom.xml` as a Maven project in IntelliJ IDEA with Ikasan Studio installed. Allow Maven import and indexing to finish.
3. Open the Studio editor using the squid icon on the right stripe or **Tools → Ikasan Studio → Open Ikasan Studio**. Use the matching bundled meta-pack if prompted.
4. Inspect the flows, component properties and developer implementations. Review connection settings and local paths before running.
5. Start any required services, then select **Run module**. After startup completes, **Console** opens the module-local Blue Console for flow status and administration (local default login: `admin` / `admin`).

FTP, SFTP, email and JMS examples need suitable endpoints and credentials. Consult each sample's `LOCAL_TEST_ENVIRONMENT.md` and the [Studio harness guide](../docs/Harnesses.md). Studio provides explicitly started FTP and mail harnesses; SFTP application demos require an SSH/SFTP service. Check the actual configured JMS broker and destinations. Machine-specific paths may need adapting. Run samples individually to avoid conflicts between application ports and services.

Successful delivery includes the flow remaining running while idle and delivering later work in the same application. An unavailable endpoint or an intentionally MANUAL flow awaits setup; it is not evidence of successful delivery.

## Project layout and ownership

- `pom.xml`: Maven reactor and Ikasan dependency version.
- `generated/src/main/model/model.json`: Studio's version-neutral flow model.
- `generated/`: Studio-owned application wiring, configuration, model schema/catalogue and generated verification tests. Preserve it so the sample can be inspected and built; use Studio to regenerate it.
- `user/`: developer-owned components and business types. Make application implementation changes here.
- `ModuleDiagram-*.png`: exported diagrams for visual reference. The model is authoritative; exported filenames may reflect earlier module names.
- `AGENTS.md`, `.agents/` and `.claude/`: guidance supplied with the projects for AI-assisted work.
- `test-data/`, where present: retained sample data; inspect it before using it as input.

The original archetype `Readme.txt` files remain for provenance; use the editor-opening steps above for current Studio navigation.

## Build and test

Run Maven from the selected sample's root, rather than the Studio repository root:

```sh
mvn -DskipTests package
mvn -pl user-flow-tests -am test
```

The first command compiles/packages without executing tests. The second runs the flow-test module and its reactor dependencies, including applicable generated tests. Some supplied tests are unfinished scaffolds and deliberately fail until configured; review the sample's `user-flow-tests/README.md`, test sources and `src/test/resources/module-test.properties`. Test support differs between samples. Do not interpret a successful skipped-test build as runtime verification. See [flow testing](../docs/IkasanFlowTesting.md) for assertion and debugging guidance.

## Repository housekeeping

Compiled Maven `target/` directories, IntelliJ `.idea/` state, local H2 databases, transaction logs, scheduler recovery files, temporary runtime files, persistence stores and backup copies have been removed. Project ignore rules prevent this local state from being added again. Runtime stores are recreated when needed, so these samples start without the previous developer's persisted runtime state.

Source code, current Studio models, generated application sources, Maven files, test fixtures, diagrams and project guidance are retained. Keep local credentials out of commits; `LOCAL_TEST_ENVIRONMENT.md` is ignored for future additions and can hold your own local settings.
