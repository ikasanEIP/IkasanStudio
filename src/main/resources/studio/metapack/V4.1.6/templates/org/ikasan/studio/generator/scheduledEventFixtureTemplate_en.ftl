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
 * The consumer still emits a JobExecutionContext; fixture text is stored in its job data map.
 * These events exercise flow processing, not cron timing, misfires or scheduler recovery.
 * Custom providers needing a live scheduler should use the harness's fireScheduledConsumer().
 */
public final class ScheduledEventFixture {
    /** Job data key available to downstream components in these test events. */
    public static final String TEXT_KEY = "studio.test.event.text";
    private ScheduledEventFixture() { }

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
     * Creates an independent context using the started consumer's job identity and a fixed UTC time.
     * The trigger is not registered with Quartz and getScheduler() returns null.
     * Job data is copied so fixture changes cannot modify the consumer's real job configuration.
     * @param consumer the started scheduled consumer
     * @param text fixture content, accessible through text(context) or TEXT_KEY
     * @return a new timer event for synchronous harness execution
     */
    public static JobExecutionContext create(ScheduledConsumer consumer, String text) {
        Objects.requireNonNull(text, "Fixture text");
        JobDetail job = (JobDetail) consumer.getJobDetail().clone();
        Date time = Date.from(Instant.parse("2000-01-01T00:00:00Z"));
        OperableTrigger trigger = (OperableTrigger) TriggerBuilder.newTrigger()
                .withIdentity("studio-test-event", job.getKey().getGroup()).forJob(job)
                .startAt(time).usingJobData(TEXT_KEY, text).build();
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
