package org.ikasan.studio.ui.component.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Backs the CronPopupDialogue's live plain-English summary (CronPanel#updateSummary): as each field is edited,
 * CronExpression#describeCronExpression is re-run over the whole expression to produce one sentence.
 */
public class CronExpressionTest {

    @Test
    public void testDescribeCronExpression_allDefaults() {
        String description = CronExpression.describeCronExpression("* * * ? * ? *");
        assertEquals("Every seconds, every minutes, every hours, no specific day of month, every month, no specific day of week, every year.", description);
    }

    @Test
    public void testDescribeCronExpression_specificFieldsCombine() {
        String description = CronExpression.describeCronExpression("0 0 12 ? * MON,WED,FRI *");
        assertEquals("At seconds 0, at minutes 0, at hours 12, no specific day of month, every month, on Monday, Wednesday, Friday, every year.", description);
    }

    @Test
    public void testDescribeCronExpression_stepAndRange() {
        String description = CronExpression.describeCronExpression("0 0/5 14 * * ? *");
        assertEquals("At seconds 0, every 5 minutes starting at 0, at hours 14, every day of month, every month, no specific day of week, every year.", description);
    }

    @Test
    public void testDescribeCronExpression_blankIsEmpty() {
        assertEquals("", CronExpression.describeCronExpression(""));
        assertEquals("", CronExpression.describeCronExpression(null));
    }

    @Test
    public void testDescribeCronExpression_shorterExpressionOnlyDescribesSuppliedFields() {
        String description = CronExpression.describeCronExpression("0 30 14");
        assertEquals("At seconds 0, at minutes 30, at hours 14.", description);
    }

    @Test
    public void testDescribeCronExpression_undescribableFieldIsOmittedNotBlank() {
        // Bare weekday abbreviations (e.g. "MON") aren't resolved by describeField today - the day-of-week
        // field should simply be dropped from the sentence rather than producing a blank/garbled entry.
        String description = CronExpression.describeCronExpression("0 30 14 ? * MON *");
        assertEquals("At seconds 0, at minutes 30, at hours 14, no specific day of month, every month, every year.", description);
    }

    @Test
    public void testDescribeCronExpression_startsWithCapitalAndEndsWithFullStop() {
        String description = CronExpression.describeCronExpression("* * * ? * ? *");
        assertTrue(Character.isUpperCase(description.charAt(0)));
        assertTrue(description.endsWith("."));
    }
}
