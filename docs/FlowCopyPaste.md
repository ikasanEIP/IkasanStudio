# Copying flows between projects

Open Ikasan Studio in both projects and configure the destination module with the same Ikasan version as the source.

1. Right-click the source flow background and choose **Copy Flow**. Alternatively, select the flow, give the canvas keyboard focus, and use IntelliJ's **Edit → Copy** or your configured Copy shortcut.
2. Switch to the destination Studio editor. Right-click the canvas or a flow and choose **Paste Flow**, or focus the canvas and use **Edit → Paste** or your configured Paste shortcut.
3. If the name conflicts with an existing flow or its generated Java names, enter a different name. Studio suggests a name ending in “Copy”. Cancelling leaves the destination unchanged.

The pasted flow is appended to the module. Its components, configured properties, route branches, decorators and exception rules are copied. The source and pasted flow are independent. Copy/Paste also works within one project to duplicate a flow. IntelliJ's normal Undo/Redo reverses or restores the insertion.

This copies the flow design. Custom Java implementations, resource files and additional Maven dependencies must be copied separately. Normal generation creates the destination's generated code and any missing implementation stubs; it does not transfer the source project's implementation bodies. Review paths, queue names and other project-specific values before running the pasted flow.

Different Ikasan versions are currently rejected. Studio also rejects clipboard data if the destination cannot preserve its contents. Configure matching versions before copying; this feature does not migrate flows.

## Manual verification

- Open two configured projects with the same version. Copy a flow with a consumer, router branches, decorators and exception rules; paste into the other project. Check the design and generated files.
- Edit a pasted component and confirm the source is unchanged. Repeat Paste to check conflict naming; cancel once, then accept a new name.
- Undo and Redo from the focused canvas; check the module model and flow registration.
- Try a destination using another Ikasan version; verify the message and unchanged destination.
- Verify the context menus and configured keyboard shortcuts in light and dark themes. Copy/Paste inside property text fields should retain ordinary text-editing behavior.
- Start a paste and close the destination editor or reload its model before completion; verify that the obsolete operation does not modify the new editor/model.
