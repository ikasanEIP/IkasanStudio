Add meaningful tests for the new component: contract validation, model save/reload, generated POM and
factory, compilation against the actual internal library and a runtime assertion that the factory
constructs the intended shared class. Exercise non-default values, false, zero, empty/unset and supported
collections. Prove actual property binding; do not only check source substrings. Keep expected business
values independent and preserve developer-owned code. Fix the implementation if a check fails; do not
edit assertions merely to obtain a pass. Record dependencies that require approved internal provisioning.
