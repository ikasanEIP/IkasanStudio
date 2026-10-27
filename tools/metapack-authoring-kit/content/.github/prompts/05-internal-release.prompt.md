Review this internal pack against ACCEPTANCE.md. Verify a stable distinct pack ID, exact baseline/runtime,
new packVersion, documented upstream differences, correct generated Maven dependencies and no secret values
in fixtures. Inspect and validate the actual JAR: complete resources only, no business classes or unrelated
packs. Record hashes and test evidence, including actual runtime delivery separately from contract checks.
Exercise install/restart/select/generate and managed revision updates/rollback in Studio. Prepare the private
handover only in the approved internal repository. Never publish or send the private pack outside work.
