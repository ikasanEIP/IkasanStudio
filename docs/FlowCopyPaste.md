# Copying flows between projects

Open Ikasan Studio in both projects and configure the destination module with the same Ikasan version as the source.

1. Right-click the source flow background and choose **Copy Flow**. Alternatively, select the flow, give the canvas keyboard focus, and use IntelliJ's **Edit → Copy** or your configured Copy shortcut.
2. Switch to the destination Studio editor. Right-click the canvas or a flow and choose **Paste Flow**, or focus the canvas and use **Edit → Paste** or your configured Paste shortcut.
3. If the name conflicts with an existing flow or its generated Java names, enter a different name. Studio suggests a name ending in “Copy”. Cancelling leaves the destination unchanged.

The pasted flow is appended to the module. Its components, configured properties, route branches, decorators and exception rules are copied. The source and pasted flow are independent. Copy/Paste also works within one project to duplicate a flow. IntelliJ's normal Undo/Redo reverses or restores the insertion.

Copy includes Java files in the flow's package beneath `user/src/main/java`, including helper classes, custom property implementations and subpackages. Unsaved Java editor changes are included. On paste, Studio adjusts package declarations, imports, qualified Java references and package-based Spring bean names for the destination module and chosen flow name. Normal generation preserves the copied implementation bodies. The source project is unchanged, and the clipboard contains the source text, so the source project can be closed after copying.

An existing destination user package stops the paste before any flow or source files are added; Studio never replaces existing Java files. Resolve the package conflict or use a different flow name and copy again. If writing the copied files fails, files created by that paste are removed.

Shared Java code outside the flow's package, resources and additional Maven dependencies must still be copied separately. Arbitrary string literals (for example, reflection class names) are preserved; review these alongside paths, queue names and other project-specific values before running. This feature transfers Java source, not Kotlin files or test sources. Linked source paths are rejected, and the clipboard payload is limited to 8 MB.

Older design-only clipboard data is still accepted. A clipboard containing Java sources requires a Studio version that supports source copying; older versions will not silently paste just the design.

Different Ikasan versions are currently rejected. Studio also rejects clipboard data if the destination cannot preserve its contents. Configure matching versions before copying; this feature does not migrate flows.

## Updating shared domain references

After moving a shared class with IntelliJ refactoring, use **Tools → Ikasan Studio → Find and Replace in Model…**, or right-click the Studio module background and choose **Find and Replace in Model…**. For example, find `org.example.cat.domain` and replace it with `org.example.debug.domain`.

Choose **Preview replacements** to see each affected flow, component, property and before/after value. Clear any rows you do not want to change, then choose **Apply**. This can update both class properties and JMS trusted-package lists. Matching is literal and case-sensitive, respects Java name boundaries, and includes subpackages; `org.example.cat.domainExtra` will not match `org.example.cat.domain`.

To review remaining references in project files, select **Open Replace in Files after applying**. After successful generation, Studio opens IntelliJ's **Replace in Files** window for the current project with the same Find and Replace values. Review its matches and choose which to replace; opening the window does not change any files. IntelliJ uses ordinary literal text matching here, so review similarly named packages as well.

Studio updates the live model, saves `model.json` and regenerates the affected code. There is no reload step. IntelliJ Undo/Redo reverses or restores the selected property changes; finish pending property edits and generation first. User Java files, module application packages and flow/component names are outside this operation. Move Java classes and update their code references with IntelliJ refactoring separately.

## Manual verification

- Open two configured projects with the same version. Copy a flow with a consumer, router branches, decorators and exception rules; paste into the other project. Check the design and generated files.
- Copy a flow with a hand-written converter and helpers, including an unsaved edit. Paste into a project with a different application package, compile, regenerate twice and check the bodies and Jump to Code. Confirm a destination package conflict leaves existing files and the model unchanged.
- Edit a pasted component and confirm the source is unchanged. Repeat Paste to check conflict naming; cancel once, then accept a new name.
- Undo and Redo from the focused canvas; check the module model and flow registration.
- Try a destination using another Ikasan version; verify the message and unchanged destination.
- Verify the context menus and configured keyboard shortcuts in light and dark themes. Copy/Paste inside property text fields should retain ordinary text-editing behavior.
- Start a paste and close the destination editor or reload its model before completion; verify that the obsolete operation does not modify the new editor/model.
