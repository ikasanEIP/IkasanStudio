# Internal meta-pack authoring contract

This project authors a resource-only Ikasan Studio pack. Read the kit README, WORKFLOW.md,
GENERATION_RULES.md, schemas and public examples before editing. Their paths may be one level
above the starter workspace; attach them explicitly in Chat when needed. No Studio/Dashboard
checkout is required. Follow the organisation's approved Copilot, source-access and network policies;
a local IDE and prompt instructions are not data-isolation controls.

- The public starter targets Ikasan 4.1.6/JDK 17 and generator API 1. Preserve that baseline unless
  the user explicitly chooses a different runtime and its complete pack is supplied.
- Keep private library source, internal configuration, coordinates, generated private packs and
  reports inside the approved work environment. Never publish or upload them for external review.
- Never invent metadata keys, methods, constructors, default values, payload types or dependency versions.
  Record evidence in the inventory, including inherited APIs and configuration classes. Mark uncertainties.
- Build the smallest working component first. A base Ikasan builder may instantiate the base type,
  not the shared subclass. Prove the actual constructed class and property bindings in tests.
- Keep implementations in the existing runtime library. Pack JARs contain descriptors, templates,
  schemas/artwork and no business implementation classes. A small adapter is a separate runtime artifact.
- Add runtime dependencies to generated applications using verified descriptor dependencies. Test-only
  Maven dependencies of this authoring project do not add them to generated application POMs.
- Distinguish direct component setters, configuration-bean setters, constructor/factory arguments,
  Spring references, property-driven expressions and secrets. Do not turn a bean reference into a string.
- REST import uses exact implementation/configuration classes and verified runtime fields. Use explicit
  REST-to-Studio mappings only for real discrepancies, and verified runtime defaults only with evidence.
  Preserve unknown/unmapped configuration for review. Missing value and JSON null are unset, not zero/false.
- Use a complete baseline pack under a distinct ID. Build-time assembly is available; runtime inheritance,
  overlays and automatic internal dependency installation are not supported.
- Preserve standard components and upstream fixes. Do not alter the public API or weaken tests to make
  a descriptor pass. Expose unsupported shapes as gaps needing an adapter or coordinated engine change.
- Fixtures use synthetic data and placeholder credentials. Do not embed production JNDI maps/secrets.
- Keep `requiresStub=false` for imported existing implementations where that contract applies. Do not
  generate replacements for developer-owned implementations or change business assertions to pass tests.
- Run contract, save/reload, generation, compile and installer tests. Runtime acceptance requires first
  delivery, idle readiness and later delivery in the same running application without restart/reset.
- Keep the public kit inventory unchanged as a provenance record. After deliberate starter edits use
  verify.py --build-only; provision new private dependencies only through approved internal mechanisms.
