package org.ikasan.studio.ui.actions;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets the user provide the test message payload either by typing it directly, or by loading a file's
 * contents into the text area (via "Load from File..."). Either way, whatever is in the text area when
 * the dialog is confirmed becomes the payload, so a loaded file can still be tweaked before sending.
 * -
 * Optionally, the user can also pick a class from their own project (via "Choose Class..."). When set, the
 * payload text is expected to be a JSON body matching that class, rather than plain text - the generated
 * StudioInjectController deserializes it (via Jackson) into a real instance of that class before injecting it,
 * relying on the class's own fields to populate it, rather than delivering the raw JSON string. "Generate
 * Sample JSON" writes a skeleton JSON object (one entry per field, with a type-appropriate placeholder value)
 * into the text area, based on the chosen class's PSI - a starting point the user can then edit with real
 * values before sending.
 * -
 * Whatever payload text and payload class were last confirmed (OK, not Cancel) for a given component are
 * persisted via PropertiesComponent, keyed by componentKey, and restored the next time this dialog is opened
 * for that same component - so re-testing the same consumer repeatedly doesn't mean retyping the same JSON
 * every time. Scoped per-component (not shared globally) since different consumers typically expect
 * completely different payload shapes.
 */
public class SendTestMessagePayloadDialog extends DialogWrapper {
    private static final String PAYLOAD_PROPERTY_PREFIX = "ikasan.studio.sendTestMessage.payload.";
    private static final String PAYLOAD_CLASS_PROPERTY_PREFIX = "ikasan.studio.sendTestMessage.payloadClass.";

    private final Project project;
    private final String componentKey;
    private final JBTextArea payloadTextArea = new JBTextArea(10, 60);
    private final JBTextField payloadClassField = new JBTextField();
    private final JBLabel payloadLabel = new JBLabel();
    private final JButton generateJsonButton = new JButton(StudioBundle.message("button.GenerateSampleJson"));
    private String payloadClassName;

    /**
     * @param componentKey a string that uniquely identifies the target component (e.g. module + flow + component
     *                      name) - used only to namespace this dialog's persisted last-used payload/class, never
     *                      sent anywhere. Pass null to disable persistence (nothing is restored or saved).
     */
    public SendTestMessagePayloadDialog(Project project, String componentKey) {
        super(project, true);
        this.project = project;
        this.componentKey = componentKey;
        restorePersistedValues();
        init();
        setTitle(StudioBundle.message("dialog.SendTestMessage"));
    }

    private void restorePersistedValues() {
        if (componentKey == null) {
            return;
        }
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        String persistedPayload = properties.getValue(PAYLOAD_PROPERTY_PREFIX + componentKey);
        if (persistedPayload != null) {
            payloadTextArea.setText(persistedPayload);
        }
        String persistedClassName = properties.getValue(PAYLOAD_CLASS_PROPERTY_PREFIX + componentKey);
        if (persistedClassName != null && !persistedClassName.isBlank()) {
            // Sets payloadClassName and refreshes payloadClassField/payloadLabel - all already constructed via
            // field initialisers at this point, even though createCenterPanel() (called from init() below)
            // hasn't run yet.
            setPayloadClassName(persistedClassName);
        }
    }

    /**
     * Only reached on OK (not Cancel or closing the dialog) - persisting on Cancel would remember a payload the
     * user explicitly chose not to send.
     */
    @Override
    protected void doOKAction() {
        if (componentKey != null) {
            PropertiesComponent properties = PropertiesComponent.getInstance(project);
            properties.setValue(PAYLOAD_PROPERTY_PREFIX + componentKey, payloadTextArea.getText());
            if (payloadClassName != null) {
                properties.setValue(PAYLOAD_CLASS_PROPERTY_PREFIX + componentKey, payloadClassName);
            } else {
                // Explicitly cleared by the user (see setPayloadClassName(null) below) - forget it too, rather
                // than resurrecting a stale class next time.
                properties.unsetValue(PAYLOAD_CLASS_PROPERTY_PREFIX + componentKey);
            }
        }
        super.doOKAction();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));

        // BorderLayout (not BoxLayout) - BoxLayout(Y_AXIS) aligns siblings relative to EACH OTHER's
        // alignmentX, so setting it on just the label wasn't enough while classPanel's own alignmentX stayed
        // at its default. BorderLayout always stretches its NORTH/CENTER children to the container's full
        // width, and JLabel's text is left-aligned by default, so this reliably sits flush left regardless.
        JPanel topPanel = new JPanel(new BorderLayout(0, 4));
        topPanel.add(createClassPanel(), BorderLayout.NORTH);
        updatePayloadLabel();
        topPanel.add(payloadLabel, BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        payloadTextArea.setLineWrap(true);
        panel.add(new JBScrollPane(payloadTextArea), BorderLayout.CENTER);

        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton loadFromFileButton = new JButton(StudioBundle.message("button.LoadPayloadFromFile"));
        loadFromFileButton.addActionListener(e -> loadFromFile());
        filePanel.add(loadFromFileButton);
        panel.add(filePanel, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(560, 340));
        return panel;
    }

    /**
     * BorderLayout (not FlowLayout) so the buttons always stay anchored at the right edge and visible -
     * FlowLayout would wrap them onto a second row once the label + wide text field didn't fit the dialog's
     * width, and that wrapped row was being clipped rather than growing the dialog.
     */
    private JPanel createClassPanel() {
        JPanel classPanel = new JPanel(new BorderLayout(4, 0));
        payloadClassField.setEditable(false);
        // Fixed column width regardless of the actual class name's length - without this, a long fully-
        // qualified name (e.g. org.example.cat.dog.Animal) makes the field's preferred width (and so the
        // whole dialog's width) grow to fit the text once a class is chosen.
        payloadClassField.setColumns(30);
        classPanel.add(new JBLabel(StudioBundle.message("label.PayloadClass")), BorderLayout.WEST);
        classPanel.add(payloadClassField, BorderLayout.CENTER);

        JPanel classButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton chooseClassButton = new JButton(StudioBundle.message("button.ChooseClass"));
        chooseClassButton.addActionListener(e -> chooseClass());
        classButtonsPanel.add(chooseClassButton);
        JButton clearClassButton = new JButton(StudioBundle.message("button.Clear"));
        clearClassButton.addActionListener(e -> setPayloadClassName(null));
        classButtonsPanel.add(clearClassButton);
        generateJsonButton.setEnabled(false);
        generateJsonButton.addActionListener(e -> generateSampleJson());
        classButtonsPanel.add(generateJsonButton);
        classPanel.add(classButtonsPanel, BorderLayout.EAST);
        classPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return classPanel;
    }

    private void chooseClass() {
        // Project scope only (not libraries) - see class javadoc: this is for the user's own simple POJOs,
        // not arbitrary library/JDK classes.
        TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project)
                .createProjectScopeChooser(StudioBundle.message("dialog.ChoosePayloadClass"));
        chooser.showDialog();
        PsiClass selected = chooser.getSelected();
        if (selected != null) {
            setPayloadClassName(selected.getQualifiedName());
            warnIfNotSerializable(selected);
        }
    }

    /**
     * A real (or our synthetic test) JMS ObjectMessage's getObject() is declared to return
     * java.io.Serializable, so a non-Serializable payload class fails with a ClassCastException the moment a
     * JMS-backed consumer's downstream code actually calls getObject() (e.g. Object Message To Object
     * Converter) - this is a genuine JMS constraint, not a Studio limitation (a real broker enforces the same
     * rule when an ObjectMessage is created), so it's flagged as a warning rather than silently worked around.
     * Runs the inheritance check via ReadAction.nonBlocking for the same "no PSI/index access on the EDT"
     * reason as generateSampleJson.
     */
    private void warnIfNotSerializable(PsiClass psiClass) {
        ReadAction.nonBlocking(() -> InheritanceUtil.isInheritor(psiClass, "java.io.Serializable"))
                .expireWith(getDisposable())
                .finishOnUiThread(ModalityState.stateForComponent(payloadClassField), isSerializable -> {
                    if (!isSerializable) {
                        StudioUIUtils.displayIdeaWarnMessage(project,
                                StudioBundle.message("message.PayloadClassNotSerializable", payloadClassName));
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private void setPayloadClassName(String payloadClassName) {
        this.payloadClassName = payloadClassName;
        payloadClassField.setText(payloadClassName != null ? payloadClassName : "");
        // The field has a fixed column width (see createClassPanel), so a long fully-qualified name can be
        // truncated from view - the tooltip keeps it discoverable on hover.
        payloadClassField.setToolTipText(payloadClassName);
        generateJsonButton.setEnabled(payloadClassName != null);
        updatePayloadLabel();
    }

    private void updatePayloadLabel() {
        // A plain JBLabel never wraps - a single line this long (especially once it includes the class name)
        // was what was actually forcing the whole dialog wide, not the class field. Wrapping in HTML with an
        // explicit body width makes Swing's HTML renderer reflow it across multiple lines instead.
        String text = payloadClassName != null
                ? StudioBundle.message("message.EnterTheTestMessagePayloadAsJson", payloadClassName)
                : StudioBundle.message("message.EnterTheTestMessagePayload");
        payloadLabel.setText("<html><body style='width: 500px'>"
                + StringUtil.escapeXmlEntities(text) + "</body></html>");
    }

    /**
     * Writes a skeleton JSON object into the payload text area, one entry per non-static/non-transient field
     * of the chosen class, each with a type-appropriate placeholder value - a starting point to hand-edit with
     * real values, not a finished payload. Always overwrites the text area, same as "Load from File...".
     * -
     * PsiClass/field lookups go through the stub index, which the platform now hard-refuses to touch
     * synchronously on the EDT ("Slow operations are prohibited on EDT") - ReadAction.nonBlocking runs the PSI
     * work on a pooled thread under a read action, then hops back to the EDT (at this dialog's own modality,
     * since it's a modal DialogWrapper) to update the text area. expireWith(getDisposable()) cancels it if the
     * dialog is closed before it completes.
     */
    private void generateSampleJson() {
        if (payloadClassName == null) {
            return;
        }
        ReadAction.nonBlocking(() -> {
                    PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(payloadClassName, GlobalSearchScope.projectScope(project));
                    return psiClass != null ? buildSampleJson(psiClass) : null;
                })
                .expireWith(getDisposable())
                .finishOnUiThread(ModalityState.stateForComponent(payloadTextArea), json -> {
                    if (json == null) {
                        StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotFindClassForJson", payloadClassName));
                    } else {
                        payloadTextArea.setText(json);
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private String buildSampleJson(PsiClass psiClass) {
        List<String> entries = new ArrayList<>();
        for (PsiField field : psiClass.getAllFields()) {
            if (field.hasModifierProperty(PsiModifier.STATIC) || field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                continue;
            }
            entries.add("  \"" + field.getName() + "\": " + samplePsiJsonValue(field.getType()));
        }
        return entries.isEmpty() ? "{\n}" : "{\n" + String.join(",\n", entries) + "\n}";
    }

    /**
     * A deliberately shallow, type-name-based placeholder - not a full type-aware serializer. Nested
     * object/enum/collection fields get null/[]/{} rather than being recursively expanded, so the result is
     * always a flat starting point the user fills in themselves rather than a risk of infinite recursion on
     * self-referential types.
     */
    private String samplePsiJsonValue(PsiType type) {
        String typeText = type.getCanonicalText();
        return switch (typeText) {
            case "java.lang.String", "char", "java.lang.Character" -> "\"\"";
            case "int", "java.lang.Integer", "long", "java.lang.Long",
                    "short", "java.lang.Short", "byte", "java.lang.Byte" -> "0";
            case "double", "java.lang.Double", "float", "java.lang.Float" -> "0.0";
            case "boolean", "java.lang.Boolean" -> "false";
            default -> {
                if (type instanceof PsiArrayType
                        || typeText.startsWith("java.util.List") || typeText.startsWith("java.util.Set")
                        || typeText.startsWith("java.util.Collection")) {
                    yield "[]";
                } else if (typeText.startsWith("java.util.Map")) {
                    yield "{}";
                } else {
                    yield "null";
                }
            }
        };
    }

    private void loadFromFile() {
        VirtualFile file = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFileDescriptor(), project, null);
        if (file == null) {
            return;
        }
        try {
            // Read via the VirtualFile's own API (not java.nio.file) so this also works when the file
            // lives on a remote/WSL/Docker dev environment (IntelliJ's Eel abstraction), not just locally.
            payloadTextArea.setText(new String(file.contentsToByteArray(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.CouldNotReadPayloadFile", ex.getMessage()));
        }
    }

    public String getPayload() {
        return payloadTextArea.getText();
    }

    /**
     * @return the fully-qualified name of the project class the user chose to deserialize the payload JSON
     * into, or null if none was chosen (plain-text payload, the default).
     */
    public String getPayloadClassName() {
        return payloadClassName;
    }
}
