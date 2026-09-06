package org.ikasan.studio.core.model.command;

import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowUserImplementedElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;

/**
 * Platform-neutral identity of a component's generated user-implemented class - package name plus simple class
 * name, deliberately holding neither a {@code PsiFile} nor a {@code VirtualFile} so a UI action can describe
 * "which class would be affected" without depending on IntelliJ's PSI/VFS APIs. An {@code intellij..} adapter
 * (see {@code StudioProjectFiles#resolveUserImplementedClassPath}/{@code #deleteUserImplementedClassFile})
 * resolves this to a real file.
 */
public record UserClassReference(String packageName, String className) {
    /**
     * @return the reference for {@code element}'s configured user-implemented class, or null if it isn't a
     * {@link FlowUserImplementedElement}, or has no class name configured yet.
     */
    public static UserClassReference forElement(Module module, Flow flow, FlowElement element) {
        if (!(element instanceof FlowUserImplementedElement) || module == null || flow == null) {
            return null;
        }
        Object rawClassName = element.getPropertyValue(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME);
        String className = rawClassName != null ? rawClassName.toString() : null;
        if (className == null || className.isBlank()) {
            return null;
        }
        return new UserClassReference(GeneratorUtils.getUserImplementedClassesPackageName(module, flow), className);
    }
}
