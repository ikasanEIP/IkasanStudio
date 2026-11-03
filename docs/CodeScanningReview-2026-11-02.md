# Code-scanning review — 2 November 2026

Reviewed the seven open CodeQL alerts using the authenticated GitHub API and the SARIF
source-to-sink traces from analysis `2080641886`. Changes below are local; GitHub must
analyse the pushed revision before alert resolution can be confirmed. No alert dismissals,
query exclusions or CodeQL suppressions were added.

## Remediated code paths

| Alerts | Rule | Change |
| --- | --- | --- |
| [14](https://github.com/ikasanEIP/IkasanStudio/security/code-scanning/14), [15](https://github.com/ikasanEIP/IkasanStudio/security/code-scanning/15) | `java/polynomial-redos` | Replace whitespace/comma regular-expression splitting with a linear parser. Preserve the legacy ASCII-whitespace rules, empty entries, order, duplicate values and bracketed-list fallback. JSON collection parsing is unchanged. |
| [13](https://github.com/ikasanEIP/IkasanStudio/security/code-scanning/13) | `java/polynomial-redos` | Use literal substring checks for sensitive property names, preserving case-insensitive redaction and handling long repeated keys without regex backtracking. |
| [9](https://github.com/ikasanEIP/IkasanStudio/security/code-scanning/9) | `java/path-injection` | Constrain metadata/template/icon resource lookups to canonical relative names beneath `studio/metapack`. Reject traversal, absolute/drive paths, URI schemes, backslashes, empty or dot segments, controls, and Windows trailing-dot/space aliases. Template-loader pack IDs are validated before lookup. |
| [10](https://github.com/ikasanEIP/IkasanStudio/security/code-scanning/10) | `java/zipslip` | Strengthen portable archive-entry validation and add an explicit `normalize`/`startsWith` installation-root check at the filesystem move, alongside the existing direct-parent requirement. |

The archive loader indexes resource-only JARs; it does not extract their entries to disk.
The reported Zip Slip trace also reaches classpath directory discovery and installation of
the validated JAR itself. Canonical-name checks now cover directory discovery, and the move
has an explicit containment guard. Existing executable/service rejection, resource limits,
encoded resource URLs, collision protection and session-scoped pack isolation remain intact.

Legacy `ComponentIO.deserializeModuleInstance` can read bundled model resources outside the
meta-pack namespace. It now uses a separate canonical relative classpath lookup, preserving
that API without reopening meta-pack metadata lookup to arbitrary classpath locations.
The duplicate-key fixture was moved inside the meta-pack test namespace, and its assertion
now requires the actual duplicate-field parser diagnostic rather than merely matching the
word “duplicate” in a missing-file name.

## Findings retained for review

[11](https://github.com/ikasanEIP/IkasanStudio/security/code-scanning/11) and
[12](https://github.com/ikasanEIP/IkasanStudio/security/code-scanning/12), both
`java/regex-injection`, concern `ComponentPropertyMeta.getValidationPattern` and
`MetaPackDataValidator` compiling the `validation` expression supplied by a meta-pack.

That field intentionally contains a complete regex, not literal text to interpolate into
another regex. Quoting it would disable existing validation. Shipped packs use lookaheads,
including exclusion of Java keywords from package names, and the public metadata API returns
`java.util.regex.Pattern`. This patch retains that contract and its existing validation tests.

These two alerts remain open and are not asserted to be resolved. Java's backtracking engine
can still make pathological custom pack patterns expensive. Restricting the supported regex
dialect, changing engines or adding a bounded matching policy needs a separate compatibility
design; moving the compiler call or suppressing the query would not fix that concern.

## Verification

- Full plugin suite: **1,655 tests**, zero failures, errors or skips.
- Full headless suites: **665 tests**, zero failures, errors or skips.
- `buildPlugin` and `verifyReleaseArchive` passed; the archive contains the updated core.

Regression coverage includes 200,000-character whitespace runs, long repeated sensitive
keys, legacy list whitespace/empty-entry behavior, malicious portable archive names,
resource namespace escapes, invalid template IDs, normal encoded resource names,
legacy model resources, duplicate JSON keys and external-pack installation/lifecycle tests.

CodeQL was not rerun locally; its CLI is not installed in this workspace. The existing manual
Java compilation workflow remains enabled and unchanged. GitHub reanalysis is required to
confirm which of the five addressed findings clear under the server's query models.

The separate ActiveMQ/JUnit dependency fixes already in the working tree were preserved.
H2 remains unchanged under the owner's earlier instruction.
