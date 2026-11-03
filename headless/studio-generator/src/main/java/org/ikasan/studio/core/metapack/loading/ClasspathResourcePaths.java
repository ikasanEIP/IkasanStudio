package org.ikasan.studio.core.metapack.loading;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** Canonical, portable classpath names. URL schemes, drive paths and traversal are never resource names. */
public final class ClasspathResourcePaths {
    private ClasspathResourcePaths() { }

    /** Returns null for unsafe names; only archive directory entries may have a trailing slash. */
    public static String canonicalRelative(String name, boolean directory) {
        if (name == null || name.isEmpty() || name.indexOf('\\') >= 0 || name.indexOf(':') >= 0
                || name.chars().anyMatch(c -> c < 32 || c == 127)) return null;
        String canonical = directory && name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
        if (canonical.isEmpty() || canonical.startsWith("/")) return null;
        // Win32 strips trailing dots/spaces from path segments; reject aliases on every platform.
        for (String segment : canonical.split("/", -1))
            if (segment.endsWith(".") || segment.endsWith(" ")) return null;
        try {
            Path relative = Path.of(canonical);
            Path normalized = relative.normalize();
            if (relative.isAbsolute() || normalized.startsWith("..")
                    || !normalized.toString().replace('\\', '/').equals(canonical)) return null;
            return canonical;
        } catch (InvalidPathException invalid) { return null; }
    }

    static String metaPackResource(String name) {
        String canonical = canonicalRelative(name, false);
        if (canonical == null) return null;
        Path path = Path.of(canonical).normalize();
        Path root = Path.of("studio", "metapack");
        return path.startsWith(root) ? path.toString().replace('\\', '/') : null;
    }

    static boolean packId(String id) {
        return id != null && id.matches("[A-Za-z0-9][A-Za-z0-9._-]*") && canonicalRelative(id, false) != null;
    }
}
