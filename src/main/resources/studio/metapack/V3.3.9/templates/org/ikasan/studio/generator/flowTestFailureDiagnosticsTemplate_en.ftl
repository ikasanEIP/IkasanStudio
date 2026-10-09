package org.ikasan.studio.flowtests.support.utils;

import org.junit.ComparisonFailure;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Adds scenario context while retaining JUnit comparisons and the developer's assertion location. */
public final class FlowTestFailureDiagnostics {
    private FlowTestFailureDiagnostics() { }

    public static AssertionError contextualise(String context, Throwable failure, Class<?> testClass) {
        Throwable assertion = null;
        int assertionFrame = -1;
        ComparisonFailure comparison = null;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof ComparisonFailure) comparison = (ComparisonFailure) cause;
            if (!(cause instanceof AssertionError)) continue;
            StackTraceElement[] trace = cause.getStackTrace();
            for (int i = 0; i < trace.length; i++) {
                if (("assertExpectedOutput".equals(trace[i].getMethodName())
                        || "verifyReceivedOutput".equals(trace[i].getMethodName()))
                        && isDeveloperClass(trace[i].getClassName(), testClass)) {
                    assertion = cause;
                    assertionFrame = i;
                    break;
                }
            }
        }
        AssertionError result;
        if (comparison != null) {
            // ComparisonFailure appends its own expected/actual rendering; avoid duplicating it.
            String message = comparison.getMessage();
            if (message != null && !message.isEmpty() && context.endsWith(message))
                context = context.substring(0, context.length() - message.length()).stripTrailing();
            result = new ComparisonFailure(context, comparison.getExpected(), comparison.getActual());
            result.initCause(failure);
        } else {
            result = new AssertionError(context, failure);
        }
        if (assertion != null) {
            StackTraceElement[] trace = assertion.getStackTrace();
            result.setStackTrace(Arrays.copyOfRange(trace, assertionFrame, trace.length));
        } else {
            // A timeout/setup/processing failure must retain its real location, not a test invocation frame.
            result.setStackTrace(failure.getStackTrace());
        }
        return result;
    }

    private static boolean isDeveloperClass(String name, Class<?> testClass) {
        for (Class<?> type = testClass; type != null && type != Object.class; type = type.getSuperclass()) {
            if (type.getName().equals("org.ikasan.studio.flowtests.support.ModuleFlowTestSupport")) break;
            if (type.getName().equals(name)) return true;
        }
        return false;
    }
}
