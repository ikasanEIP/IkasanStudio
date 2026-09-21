package org.ikasan.studio.testing.packs;

import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cron expression is free text handed to Quartz when the generated application starts. Unix's five-field cron,
 * a sentence, or day-of-month and day-of-week both set are all rejected by Quartz, and the flow then cannot start.
 * The expected verdicts below come from org.quartz.CronExpression#isValidExpression (Quartz 2.5.2); the pattern is
 * deliberately no stricter than Quartz, so nothing Quartz accepts is refused.
 */
@ExtendWith(SharedResourceExtension.class)
@org.junit.jupiter.api.Tag("packs")
class CronExpressionValidationTest {
    private static final String[] CRON_COMPONENTS = {"Scheduled Consumer", "FTP Consumer", "SFTP Consumer", "Local File Consumer"};

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void acceptsEveryExpressionQuartzAccepts(String pack) throws Exception {
        for (String component : CRON_COMPONENTS) {
            var meta = ComponentLibrary.getIkasanComponents(pack).get(component).getMetadata("cronExpression");
            assertNotNull(meta.getValidationPattern(), pack + " " + component);
            assertNotNull(meta.getValidationMessage());
            for (String valid : new String[]{"0 * * * * ? *", "0 0/5 * * * ?", "0 0 12 * * ?", "0 15 10 ? * MON-FRI",
                    "0 15 10 ? * 6#3", "0 0 0 L * ?", "0 0 0 1W * ?", "0 0 12 * * ? 2030", "0/30 * * * * ?", "0 0 12 1,15 * ?",
                    "0 0 12 ? JAN-MAR MON,FRI", "  0 0 12 * * ?  ", "0 0 0 L-2 * ?", "0 0 12 ? * 2L", "${cron.schedule}"}) {
                assertTrue(meta.getValidationPattern().matcher(valid).matches(), pack + " " + component + " should accept [" + valid + "]");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void rejectsTheCommonMistakesQuartzRejects(String pack) throws Exception {
        for (String component : CRON_COMPONENTS) {
            var pattern = ComponentLibrary.getIkasanComponents(pack).get(component).getMetadata("cronExpression").getValidationPattern();
            for (String invalid : new String[]{"*/5 * * * *", "every 5 minutes", "0 * * * * *", "0 0 12 * * ? * *", "",
                    "0 0 12 ? * ?", "0 0 12 * * ? ? 2030", "daily", "0 0 12"}) {
                assertFalse(pattern.matcher(invalid).matches(), pack + " " + component + " should reject [" + invalid + "]");
            }
        }
    }
}
