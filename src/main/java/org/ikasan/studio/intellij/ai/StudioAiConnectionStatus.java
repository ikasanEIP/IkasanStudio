package org.ikasan.studio.intellij.ai;

/** Chooses guidance from observed state; opening settings never proves a client connected. */
final class StudioAiConnectionStatus {
    private StudioAiConnectionStatus() { }

    static String nextStep(boolean running, String readinessIssue, String lastReadTransport,
                           boolean nativeRoute, boolean promptCopied) {
        if (!running) return "ai.NextStopped";
        if (readinessIssue != null) return "ai.NextModel";
        if (lastReadTransport != null) return "ai.NextVerified";
        if (promptCopied) return nativeRoute ? "ai.NextNewChat" : "ai.NextPasteManual";
        return nativeRoute ? null : "ai.NextConfigureManual";
    }
}
