package org.ikasan.studio.flowtests.support.utils;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumer;
import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.TriggerBuilder;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;

/**
 * Deterministic timer events for the default QuartzMessageProvider.
 * The consumer still emits a JobExecutionContext; optional fixture text is stored in its job data map.
 * These events exercise flow processing, not cron timing, misfires or scheduler recovery.
 * Custom providers needing a live scheduler should use the harness's fireScheduledConsumer().
 */
public final class ScheduledEventFixture {
    /** Job data key available to downstream components in these test events. */
    public static final String TEXT_KEY = "studio.test.event.text";
    private ScheduledEventFixture() { }

    /**
     * Fires a timer trigger without business input or fixture text. Use this when a downstream
     * broker obtains its own data; prepare that data source before firing the trigger.
     * Call after startFlow() and register scheduledConsumer(name) to suppress normal cron firing.
     * The flow remains available for later triggers without restarting.
     * @param harness the started flow harness
     * @param consumerName the scheduled consumer's model name
     */
    public static void fire(IkasanFlowTestRule harness, String consumerName) {
        ScheduledConsumer consumer = (ScheduledConsumer) harness.getComponent(consumerName);
        harness.fireScheduledConsumerSynchronously(create(consumer));
    }

    /**
     * Fires one event synchronously through the real consumer registered with the harness.
     * Call after startFlow(); registering scheduledConsumer(name) suppresses normal cron firing.
     * The same running flow can receive later events without restarting.
     * @param harness the started flow harness
     * @param consumerName the scheduled consumer's model name
     * @param text deterministic fixture content for this event
     */
    public static void fire(IkasanFlowTestRule harness, String consumerName, String text) {
        ScheduledConsumer consumer = (ScheduledConsumer) harness.getComponent(consumerName);
        harness.fireScheduledConsumerSynchronously(create(consumer, text));
    }

    /**
     * Creates a trigger-only context with the same fixed time and job identity as the text variant.
     * No fixture text is attached; text(context) therefore rejects this event.
     * The trigger is not registered with Quartz and getScheduler() returns null.
     * @param consumer the started scheduled consumer
     * @return an independent timer event without business input
     */
    public static JobExecutionContext create(ScheduledConsumer consumer) {
        return createEvent(consumer, null);
    }

    /**
     * Creates an independent context using the started consumer's job identity and a fixed UTC time.
     * The trigger is not registered with Quartz and getScheduler() returns null.
     * Job data is copied so fixture changes cannot modify the consumer's real job configuration.
     * @param consumer the started scheduled consumer
     * @param text fixture content, accessible through text(context) or TEXT_KEY
     * @return a new timer event for synchronous harness execution
     */
    public static JobExecutionContext create(ScheduledConsumer consumer, String text) {
        return createEvent(consumer, Objects.requireNonNull(text, "Fixture text"));
    }

    private static JobExecutionContext createEvent(ScheduledConsumer consumer, String text) {
        JobDetail job = (JobDetail) consumer.getJobDetail().clone();
        job.getJobDataMap().remove(TEXT_KEY);
        Date time = Date.from(Instant.parse("2000-01-01T00:00:00Z"));
        OperableTrigger trigger = (OperableTrigger) TriggerBuilder.newTrigger()
                .withIdentity("studio-test-event", job.getKey().getGroup()).forJob(job)
                .startAt(time).build();
        if (text != null) trigger.getJobDataMap().put(TEXT_KEY, text);
        trigger.getJobDataMap().put(ScheduledConsumer.CRON_EXPRESSION,
                consumer.getConfiguration().getConsolidatedCronExpressions().get(0));
        TriggerFiredBundle fired = new TriggerFiredBundle(job, trigger, null, false,
                time, time, null, null);
        return new JobExecutionContextImpl(null, fired, consumer);
    }

    /**
     * Reads fixture text for a meaningful output assertion instead of the context's identity string.
     * @param event the actual context received downstream
     * @return the original fixture text
     * @throws IllegalArgumentException if the event did not carry fixture text
     */
    public static String text(JobExecutionContext event) {
        Object text = event.getMergedJobDataMap().get(TEXT_KEY);
        if (!(text instanceof String)) {
            throw new IllegalArgumentException("Scheduled event has no fixture text: " + TEXT_KEY);
        }
        return (String) text;
    }
}
