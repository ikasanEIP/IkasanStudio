package org.ikasan.studio.core.project;

import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves the application reactor without changing the layout of existing projects. */
public final class StudioProjectLayout {
    public static final String APPLICATION_MARKER = ".ikasan-studio-application";
    private StudioProjectLayout() { }

    public static Path applicationRoot(Path workspace) {
        Path candidate = workspace.resolve("jar");
        if (Files.isRegularFile(workspace.resolve("generated/pom.xml"))
                && Files.isRegularFile(workspace.resolve("user/pom.xml"))) return workspace;
        return Files.isRegularFile(candidate.resolve(APPLICATION_MARKER))
                || (Files.isRegularFile(candidate.resolve("generated/pom.xml"))
                && Files.isRegularFile(candidate.resolve("user/pom.xml"))) ? candidate : workspace;
    }

    public static String applicationPath(Path workspace, String relative) {
        return (isNested(workspace) ? "jar/" : "") + relative;
    }

    public static boolean isNested(Path workspace) {
        return !applicationRoot(workspace).equals(workspace);
    }
}
