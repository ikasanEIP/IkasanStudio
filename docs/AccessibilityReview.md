# Accessibility and IntelliJ UI review

Review date: 7 September 2026. Target: IDEA 2024.3.7, Java 17.

This is a source and automated component review, with fixes. It is not a screen-reader certification or a completed interactive theme/scaling sign-off. The outstanding checks below remain release work.

## Findings and changes

| Area | Evidence and changes | Remaining checks or gaps |
| --- | --- | --- |
| Keyboard navigation | Canvas now supports arrows through the module, flows, consumers and route components in model order. Selection updates properties, respects pending-edit confirmation, scrolls into view and announces the full name. Shift+F10 and the context-menu key open existing actions. Tab retains normal Swing traversal. | Palette insertion still depends on drag-and-drop. Exception resolvers, decorators, branch controls and painted endpoint/test-server controls need a complete keyboard workflow. Held-key behaviour and focus visibility need interactive checking. |
| Focus order | Settings retain the preferred first checkbox. Property labels are linked to their input, including boolean fields. Existing visual row order and platform traversal remain intact. | Traverse all conditional property groups, dialogs and split panes with Tab/Shift+Tab. Confirm there are no traps or hidden focus targets. |
| Accessible names | Canvas and palette have names and usage descriptions. Property editors use their complete label and help text; boolean choices include the property name and choice. Cron helper is named instead of being announced as “...”. Overwrite controls have contextual names; default/class buttons have contextual descriptions. Read-only choices are now actually disabled. | The custom-painted canvas does not expose each model element as a separate accessible child. Toolbar icon buttons already have names; descriptions and enabled-state announcements need screen-reader checking. |
| Toolbar tooltips | CanvasPanel routes toolbar buttons through a helper that assigns tooltips; Run and Debug also update their tooltips with runtime state. Module and harness icon buttons have explicit accessible names. | Test tooltips while disabled/running and at narrow widths. The toolbar layout was not changed during this review. |
| Shortcuts and IDEA actions | Open Studio, Migrate and Restore Migration are registered in the IDEA action system and Tools menu. Canvas bindings are local to its focus and leave IDE Undo/Redo and normal field editing intact. | Several toolbar/context commands remain Swing listeners rather than registered IDEA actions, so Find Action/keymap customization is incomplete. Provide a keyboard action for adding palette components. |
| Light/dark/high contrast | Palette now uses DefaultListCellRenderer to preserve look-and-feel selection colours and focus borders. Category headings use bold text rather than a fixed orange foreground. Tests supply light, dark and high-contrast-like colour combinations and verify theme colour use and full accessible text. | Synthetic colours do not validate actual IDEA themes. Canvas status, connector, warning and custom icon colours still need visual contrast review. ThemeAwareColors alone does not guarantee adequate contrast. |
| Scaling | Palette divider and settings insets use JBUI scaling. Automated settings checks exercise 125%, 150% and 200% user scale. | Verify native OS scaling, fonts, SVGs, click targets, drag coordinates, tooltips and the entire canvas at each scale. User-scale component tests do not emulate OS scaling or remote graphics. |
| Narrow widths | Settings now sit in a JBScrollPane so content remains reachable in a small viewport. Existing tests cover note widths; the new test covers scrolling in a 320×240 viewport at each requested scale. | Check the designer toolbar and property columns at 420/600 logical pixels; verify overflow is reachable without enlarging the IDE window. |
| Long names | Palette retains the complete name in the accessible label and tooltip. Keyboard selection announces the full model identity. Tests include a long flow identity and a long palette/category name. | Check actual canvas flow/component/endpoint labels, overlap, selection outlines, scrolling and tooltip placement with long names, spaces and non-Latin text. |
| Important notifications | Warning/error helpers retain a privacy-safe structured event in the IDE log using warning severity, without copying arbitrary notification text or producing fatal plugin-error reports. Model initialization already has editor loading/failure states. | Logs provide another copy but are not a substitute for persistent, actionable UI. Audit generation, migration and harness failures for a visible retry/details path after the balloon disappears. |

## Keyboard behaviour added

Focus the designer canvas, then use Right/Down for the next model element and Left/Up for the previous one. Navigation stops at either end. Selecting an element follows the existing property-selection workflow, including confirmation for pending changes. Use Shift+F10 or the context-menu key to reach existing edit, migration and other applicable menu commands. Tab leaves the canvas normally. These bindings do not introduce global shortcut conflicts.

## Repeatable automated checks

```sh
./gradlew test --tests '*AccessibilityTest' --tests '*CanvasKeyboardNavigationTest' --tests '*IkasanStudioSettingsConfigurableTest'
./gradlew test
```

Verification: the full Gradle suite passed with 651 tests, zero failures, errors or skips. The scale test uses IntelliJ’s `JBUIScale.setUserScaleFactorForTest`, asserts each requested factor and restores the original factor afterward.

Tests cover property-to-label associations, contextual boolean names, cron-helper naming, disabled choice controls, palette selection colours/focus borders, full palette names, model traversal order and settings scrolling/scaling. They do not exercise an actual screen reader or prove every menu action can be completed without a mouse.

## Interactive release matrix

Run `./gradlew runIde` and use both a newly created project and the large project from [PerformanceTesting.md](PerformanceTesting.md). Record IDEA version, OS, screen reader, theme, logical window dimensions and both OS/IDE scale settings with every result.

For each of IntelliJ Light, Darcula and High Contrast, repeat at 125%, 150% and 200% scaling, first at normal width and then at 420 and 600 logical pixels. Use component, flow and endpoint names of at least 120 characters, including spaces and non-Latin text.

1. Start with the keyboard: open Studio using Find Action; traverse toolbar, canvas, palette, properties and split panes. Check forward/backward traversal and visible focus. Try every flow/component action and note where a mouse is still required.
2. Enable the platform screen reader. Listen to icon-button names, descriptions, selection, boolean choices, validation errors and disabled states. Check editable combo boxes and popup focus return.
3. Inspect text and focus contrast, selection visibility, non-colour state cues, borders, SVGs and hit targets. Confirm long text can be read in full and does not cover adjacent controls.
4. Shrink both editor and settings. Reach every control using scrolling and the keyboard; check wrapped notes and buttons at the bottom of the page.
5. Trigger malformed-model, missing-meta-pack, generation and harness-start failures. Dismiss the notification and locate the details and recovery action again. Confirm safe error types and Studio call sites are available in diagnostics where no dedicated details panel exists; record missing actionable details as a recovery-UI gap.

No interactive matrix cells have been signed off by this automated review. Highest-priority follow-ups are keyboard insertion, accessible model structure, complete action registration and persistent recovery UI for failures currently explained only through notifications/logs.
