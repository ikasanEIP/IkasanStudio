package org.ikasan.studio.flowtests.support.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.flow.FlowElement;
import org.ikasan.spec.flow.FlowElementInvoker;
import org.ikasan.spec.flow.FlowEvent;

/** Test-only observation of component failures. Original exceptions still reach Ikasan's error policy. */
public final class FlowTestFailureCapture implements AutoCloseable {
    private final Map<FlowElement, FlowElementInvoker> originals = new IdentityHashMap<>();
    private final AtomicReference<AssertionError> failure = new AtomicReference<>();

    /** Attach while stopped; close only after the test flow has stopped. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public FlowTestFailureCapture(Flow flow, String flowName) {
        try {
            for (FlowElement element : (java.util.List<FlowElement<?>>) flow.getFlowElements()) {
                FlowElementInvoker original = element.getFlowElementInvoker();
                if (original == null) continue;
                originals.put(element, original);
                element.setFlowElementInvoker((FlowElementInvoker) Proxy.newProxyInstance(
                        original.getClass().getClassLoader(), contracts(original.getClass()),
                        (proxy, method, arguments) -> {
                            // Retain the entering payload type: components may mutate the event before throwing.
                            Object payload = "invoke".equals(method.getName()) ? ((FlowEvent) arguments[4]).getPayload() : null;
                            String payloadType = payload == null ? "null" : payload.getClass().getName();
                            try { return method.invoke(original, arguments); }
                            catch (InvocationTargetException wrapped) {
                                Throwable cause = wrapped.getCause();
                                if ("invoke".equals(method.getName())) {
                                    Throwable root = cause;
                                    java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
                                    while (root.getCause() != null && seen.add(root)) root = root.getCause();
                                    String hint = "";
                                    if (element.getFlowComponent() != null && element.getFlowComponent().getClass().getName()
                                            .equals("org.ikasan.component.converter.jms.ObjectMessageToObjectConverter")) {
                                        hint = " This converter requires a JMS ObjectMessage containing a serializable business object. "
                                                + "Review supplyInput(): use sendObject(queue, object), not sendText(queue, text). "
                                                + "For an ObjectMessage, also check the business class and trusted-package configuration.";
                                    }
                                    failure.compareAndSet(null, new AssertionError("Flow '" + flowName + "' failed at component '"
                                            + element.getComponentName() + "' with incoming payload type " + payloadType
                                            + ". Cause: " + root.getClass().getName() + ": " + root.getMessage() + hint, cause));
                                }
                                throw cause;
                            }
                        }));
            }
        } catch (RuntimeException | Error setupFailure) {
            close();
            throw setupFailure;
        }
    }

    private static Class<?>[] contracts(Class<?> implementation) {
        java.util.Set<Class<?>> contracts = new java.util.LinkedHashSet<>();
        contracts.add(FlowElementInvoker.class);
        // Preserve ConfiguredResource and other contracts used during flow startup/configuration.
        for (Class<?> type = implementation; type != null; type = type.getSuperclass()) {
            for (Class<?> contract : type.getInterfaces()) {
                if (java.lang.reflect.Modifier.isPublic(contract.getModifiers())) contracts.add(contract);
            }
        }
        return contracts.toArray(new Class<?>[0]);
    }

    public void check() {
        AssertionError observed = failure.get();
        if (observed != null) throw observed;
    }

    /** Poll in short intervals so an asynchronous component failure does not become an output timeout. */
    public <T> T poll(BlockingQueue<T> outputs, int seconds) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        do {
            check();
            T output = outputs.poll(25, TimeUnit.MILLISECONDS);
            check();
            if (output != null) return output;
        } while (System.nanoTime() < deadline);
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override public void close() {
        originals.forEach(FlowElement::setFlowElementInvoker);
        originals.clear();
    }
}
