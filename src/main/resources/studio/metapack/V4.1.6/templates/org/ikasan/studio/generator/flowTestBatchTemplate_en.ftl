package org.ikasan.studio.flowtests.support.utils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** One sequential batch: inputs and ordered expectations per producer, with no global output ordering. */
public final class FlowTestBatch<I> {
    /** Compatibility for older generated scenarios. */
    @Deprecated
    @FunctionalInterface public interface OutputCheck {
        void verify(Object actualAfterProducer) throws Exception;
        default String expectedText() { throw new IllegalStateException("Custom output assertion has no expected text; customise receiver verification"); }
    }
    @FunctionalInterface public interface OutputComparison {
        void verify(Object expected, Object actual) throws Exception;
    }
    private final List<I> inputs;
    private final Map<String, List<?>> outputs;

    public FlowTestBatch(List<I> inputs, Map<String, ? extends List<?>> outputs) {
        this.inputs = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(inputs)));
        Map<String, List<?>> copy = new LinkedHashMap<>();
        Objects.requireNonNull(outputs).forEach((producer, checks) -> {
            if (producer == null || producer.isBlank()) throw new IllegalArgumentException("Name each observed producer");
            copy.put(producer, List.copyOf(checks));
        });
        this.outputs = Collections.unmodifiableMap(copy);
    }
    public List<I> inputs() { return inputs; }
    public List<?> expectedOutputs(String producer) { return outputs.getOrDefault(producer, List.of()); }
    public String expectedText(String producer, int outputIndex) {
        Object expected = outputs.get(producer).get(outputIndex);
        if (expected instanceof String) return (String) expected;
        if (expected instanceof OutputCheck) return ((OutputCheck) expected).expectedText();
        throw new IllegalStateException("Expected payload is not text; customise receiver verification");
    }
    public int expectedCount(String producer) { return outputs.getOrDefault(producer, List.of()).size(); }
    public Set<String> producers() { return outputs.keySet(); }

    /** Bounded observation, including empty expectations. Omitted producers are intentionally ignored. */
    public Observation observe() {
        return observe((expected, actual) -> {
            if (!Objects.equals(expected, actual))
                throw new AssertionError("Expected <" + expected + "> but was <" + actual
                        + ">; business objects must implement equals");
        });
    }
    public Observation observe(OutputComparison comparison) { return new Observation(Objects.requireNonNull(comparison)); }
    public final class Observation {
        private final OutputComparison comparison;
        private Observation(OutputComparison comparison) { this.comparison = comparison; }
        private final BlockingQueue<Delivery> deliveries = new LinkedBlockingQueue<>();
        private final Map<String, Integer> received = new HashMap<>();
        private volatile AssertionError overflow;
        private final int capacity = outputs.values().stream().mapToInt(List::size).sum() + 1;
        public void accept(String producer, Object payload) {
            if (!outputs.containsKey(producer)) return;
            // Bound retained payloads even when a broken flow floods an observed producer.
            synchronized (deliveries) {
                if (deliveries.size() >= capacity) overflow = new AssertionError("Too many outputs at producer '" + producer + "'");
                else deliveries.add(new Delivery(producer, payload));
            }
        }
        private void check(Runnable ready) {
            ready.run();
            if (overflow != null) throw overflow;
        }
        public void verify(Duration timeout, Duration quietPeriod, Runnable ready) throws Exception {
            if (timeout.isZero() || timeout.isNegative() || quietPeriod.isZero() || quietPeriod.isNegative())
                throw new IllegalArgumentException("Delivery and quiet periods must be positive");
            int remaining = outputs.values().stream().mapToInt(List::size).sum();
            long deadline = System.nanoTime() + timeout.toNanos();
            while (remaining > 0) {
                check(ready);
                Delivery delivery = deliveries.poll(20, TimeUnit.MILLISECONDS);
                if (delivery != null) { verifyDelivery(delivery); remaining--; }
                else if (System.nanoTime() >= deadline) throw new AssertionError("Missing producer outputs; expected counts "
                        + counts() + ", observed counts " + received);
            }
            // Also checks explicit zero-output expectations and rejects duplicates after the expected outputs.
            deadline = System.nanoTime() + quietPeriod.toNanos();
            do {
                check(ready);
                Delivery extra = deliveries.poll(20, TimeUnit.MILLISECONDS);
                if (extra != null) throw new AssertionError("Unexpected extra output at producer '" + extra.producer + "'");
            } while (System.nanoTime() < deadline);
            check(ready);
        }
        public void assertNoPending() {
            if (overflow != null) throw overflow;
            Delivery extra = deliveries.poll();
            if (extra != null) throw new AssertionError("Unexpected extra output at producer '" + extra.producer + "'");
        }
        private Map<String, Integer> counts() {
            Map<String, Integer> counts = new LinkedHashMap<>();
            outputs.forEach((name, checks) -> counts.put(name, checks.size()));
            return counts;
        }
        private void verifyDelivery(Delivery delivery) throws Exception {
            int index = received.getOrDefault(delivery.producer, 0);
            List<?> checks = outputs.get(delivery.producer);
            if (index >= checks.size()) throw new AssertionError("Unexpected output at producer '" + delivery.producer + "'");
            try {
                Object expected = checks.get(index);
                if (expected instanceof OutputCheck) ((OutputCheck) expected).verify(delivery.payload);
                else comparison.verify(expected, delivery.payload);
            }
            catch (AssertionError | Exception failure) {
                throw new AssertionError("Producer '" + delivery.producer + "', output " + (index + 1) + " failed", failure);
            }
            received.put(delivery.producer, index + 1);
        }
    }
    private static final class Delivery {
        final String producer; final Object payload;
        Delivery(String producer, Object payload) { this.producer = producer; this.payload = payload; }
    }
}
