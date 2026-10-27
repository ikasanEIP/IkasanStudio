# Internal acceptance record

For each component record the source revision, runtime library coordinates/hash, pack revision,
Ikasan/JDK version, generated example and test report paths. Replace "pending" with evidence, not
with an assumption that the public starter's results apply to the private component.

- Inventory covers inherited APIs, constructor/factory/wiring and real configuration types.
- Metadata passes schema/semantic validation with no invented or unused fields.
- Palette identity, help and property grouping make the supported operation understandable.
- Generated POM resolves the actual internal library and converges on the intended Ikasan runtime.
- Generated code compiles and constructs the shared subclass, not merely the standard base class.
- Configured strings, booleans, numeric zero, lists/maps and empty/unset values bind as intended.
- Secret values are absent from checked-in fixtures, pack resources, reports and external handovers.
- Model save/reload is stable and developer-owned implementation code is preserved.
- REST imports select the right runtime class and apply only verified configuration bindings.
- First delivery, idle readiness and later delivery pass without application/flow restart or bean reset.
- Configured failure policy/rollback and explicit stop release resources as appropriate.
- Actual JAR passes installer validation and contains only the complete pack's allowed resources.
- Install, IDE restart, selection, generation and revision update/rollback work in the target Studio build.

Contract/generation checks and actual application-runtime evidence are separate. Record any unavailable
external systems as incomplete prerequisites. Do not publish a pack as certified from JSON validation alone.
