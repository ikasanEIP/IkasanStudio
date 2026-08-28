# Component drag/move implementation handoff

Date: 2026-08-28

## User request

Allow existing Ikasan flow components to be dragged to a new position in the same flow/route or into another flow in the module without deleting/recreating the component. Preserve the exact `FlowElement` instance and all configuration, especially user-generated class associations. Once a move succeeds, the old left/right neighbours reconnect automatically. If the drop is invalid, restore the component to its exact original position. Show a grey/translucent ghost while dragging and retain valid/invalid flow highlighting.

## Repository state at handoff

- No tracked files were modified for this feature.
- `git status --short` showed only the pre-existing untracked `rename.sh`; do not modify it.
- Editing was blocked by an environment failure from the patch sandbox: `bwrap: loopback: Failed RTM_NEWADDR: Operation not permitted`.
- Read-only investigation completed successfully.

## Existing implementation findings

- Main canvas: `src/main/java/org/ikasan/studio/ui/component/canvas/DesignerCanvas.java`.
- Palette import: `CanvasImportTransferHandler.java` uses `IkasanFlowUIComponentTransferable` and calls `DesignerCanvas.requestToAddComponent(...)`. It creates a new component from `ComponentMeta`; it must remain the palette COPY path.
- Existing in-canvas dragging is in `DesignerCanvas.mouseDragAction(...)`. It only changes the selected component view handler's temporary x/y coordinates. It does not mutate the flow model, and `mouseReleaseAction()` merely repaints, so layout snaps back.
- `mouseClickAction(...)` already resolves a `FlowElement` beneath the press via `getComponentAtXY(...)`.
- `componentDraggedToFlowAction(...)` already performs flow/route target highlighting and calls `Flow.issueCausedByAdding(...)` and `FlowRoute.issueCausedByAdding(...)`.
- New-item positioning is implemented by `insertNewComponentBetweenSurroundingPair(...)` and `getSurroundingComponents(...)`.
- Model ownership:
  - Consumer is stored at `Flow.consumer`, not in a route list.
  - Exception resolver is stored at `Flow.exceptionResolver`.
  - Ordinary elements are in `FlowRoute.flowElements` and hold `containingFlow` / `containingFlowRoute`.
  - `Flow.removeFlowElement(...)` returns `FlowElementRemoval`, whose `undo()` restores the exact element and any router child routes.
  - Removing routers cascades their associated child routes, so router moves require special care.
- Existing undo infrastructure includes `DeleteComponentUndoableAction`; a component move should ideally be registered as one IntelliJ global undoable action using the same pattern.
- Component painting is owned by `IkasanFlowComponentViewHandler`; `paintComponent` uses its canvas icon and decorators.
- Existing focused canvas tests: `src/test/java/org/ikasan/studio/ui/component/canvas/DesignerCanvasTest.java`.

## Recommended implementation

1. Replace the cosmetic coordinate mutation in `DesignerCanvas.mouseDragAction(...)` with explicit drag state:
   - source `FlowElement`
   - press point and current point
   - small movement threshold (about 5 px)
   - original `Flow`, `FlowRoute`, list index, and consumer/exception-resolver status
2. Keep the model unchanged throughout dragging. Paint the normal layout plus a translucent ghost at the pointer. This makes invalid-drop rollback automatic and avoids temporarily generating an invalid model.
3. Resolve a destination on release from the component/route/flow under the pointer. Compute insertion before/after an element from its centre x-coordinate, while respecting:
   - consumer remains the head and is stored on `Flow`
   - internal router endpoint remains first in a child route
   - producer remains terminal
   - router remains terminal in its owning route
4. Validate the proposed result with the source logically excluded. Same-route reorder must not fail because it sees itself as an existing producer/router/consumer. Prefer a testable model-level move transaction/helper over embedding all mutation rules in Swing callbacks.
5. Commit by relocating the same instance and updating `containingFlow` / `containingFlowRoute`. For cross-flow router moves, relocate its associated child routes as a unit and recursively update containing-flow references. If this cannot be made valid, reject and leave the source untouched.
6. After a successful commit:
   - reset dimensions and repaint
   - persist/regenerate using `StudioPsiUtils.refreshCodeFromModel(...)`
   - use a flow-scoped `GenerationRequest` for same-flow reorder; use a module-structure/full appropriate request for cross-flow moves so both flows update
   - preserve generated/user ownership; do not recreate the `FlowElement`
   - register one global undoable move action following `DeleteComponentUndoableAction`
7. On invalid release, clear drag state/highlighting and repaint. Since the model was never mutated, the component returns exactly to its origin.

## Tests to add

- Ordinary component moves earlier and later within the same `FlowRoute`.
- Same-route no-op drop.
- Cross-flow move preserves object identity and all component properties/user class name.
- Invalid cross-flow drop (for example into a route already containing a producer) leaves both source and target byte-for-byte/order-equivalent.
- Consumer move only succeeds into a flow without a consumer.
- Producer remains terminal.
- Internal endpoint remains first in child routes.
- Router move preserves its child-route subtree or is rejected without mutation.
- Undo/redo restores both route ordering and containment references.
- Ghost state begins only after the drag threshold and clears on release.

## Verification

Run focused tests first, then:

```bash
./gradlew test
```

For manual verification, use `./gradlew runIde` and test light/dark themes, same-route reorder, cross-flow move, invalid rollback, router branches, and persistence after closing/reopening the Studio editor.
