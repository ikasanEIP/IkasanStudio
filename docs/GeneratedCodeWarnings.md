# Generated Java warning review

The V3.3.9 and V4.1.6 FTL templates are reviewed together. This review covers all bundled FTL files, including supporting generated classes, with regression coverage for generated component output and Javadoc tag hygiene.

Fixed in this review:

- Empty Filter, filter-rule and Producer parameter/exception descriptions. FilterException describes an evaluation failure, not normal message rejection.
- Missing Router and Splitter exception documentation and the Splitter method summary.
- Annotation examples mistakenly written as Javadoc block tags in the resource factory template; the example also previously evaluated a Spring placeholder as FreeMarker data.
- Generic interface/recipe names embedded as unescaped Javadoc HTML.
- Translator and Debug stdout calls, including Debug's null dereference. They now use parameterized DEBUG logging of payload types without dumping payload contents.
- Redundant imports in Debug and configuration stubs.
- Serialization streams in the debug copy helper now use try-with-resources. Its documentation explains that copying is best-effort and may retain shared state.

## Verification

From the repository root:

```sh
./gradlew test --tests '*GeneratedTemplateJavadocTest' --tests 'org.ikasan.studio.testing.packs.*'
./gradlew -p headless test
./gradlew buildPlugin
```

The Javadoc test checks recognized block tags and nonempty parameter, exception and return descriptions in every bundled template. It is not a full Java compiler or Javadoc/doclint run. Generated-output tests check representative component combinations; they do not cover every possible user-supplied type.

## Remaining limits

GitHub runs the checks configured by each repository; there is no universal set of check-in warnings. The consuming project's Checkstyle, PMD, Sonar, Qodana, compiler and Javadoc settings may enforce additional rules.

- User-owned stubs intentionally contain implementation tasks and may have unused parameters/loggers until completed. Generators cannot supply application-specific filtering, routing, splitting or broker logic.
- Some generated integration code uses raw types/unchecked casts to cross Ikasan/provider API boundaries. Eliminating these safely requires per-provider typed adapters and broader compatibility work; this review does not blanket-suppress them.
- Debug copying uses Java serialization/reflection and may trigger security or encapsulation rules. The copy policy needs separate design work; it must not be described as guaranteed isolation.
- Full public-API Javadoc coverage, project-specific naming/line-length rules and dependency/deprecation findings require the consuming project's actual rules and supported dependency versions.
- Resource-factory/generic-interface scaffolds depend on user-supplied interfaces and resources and still require implementations; arbitrary interface methods cannot be inferred by these templates.

Existing developer-owned Java files are not rewritten by this review. Regenerate or re-add components in Studio to inspect the changes, and run the consuming project's checks after implementing the stubs.
