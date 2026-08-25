package org.ikasan.studio.ui.component.properties;

//import org.quartz.CronExpression;

import com.google.common.primitives.Ints;
import lombok.Getter;
import org.ikasan.studio.ui.StudioBundle;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNumeric;

@Getter
public enum CronExpression {

    SECONDS(     0, true, "*", "0-59",            "*  n  n1-n12   n1,n2,..  n1/n2"),
    MINUTES(     1, true, "*", "0-59",            "*  n  n1-n12   n1,n2,..  n1/n2"),
    HOURS(       2, true, "*", "0-23",            "*  n  n1-n12   n1,n2,..  n1/n2"),
    DAY_OF_MONTH(3, true, "?", "1-12 or JAN-DEC", "*  n  n1-n12   n1,n2,..  n1/n2  ?  L  W"),
    MONTH(       4, true, "*", "1-31",            "*  n  n1-n12   n1,n2,..  n1/n2"),
    DAY_OF_WEEK( 5, true, "?", "1-7 or SUN-SAT",  "*  n  n1-n12   n1,n2,..  n1/n2  ?  L  #"),
    YEARS(       6, true, "*", "1970-2099",       "*  n  n1-n12   n1,n2,..  n1/n2");

    final int index;
    final boolean mandatory;
    final String defaultValue;
    final String allowedValues;
    final String specialCharacters;

    CronExpression(int index, boolean mandatory, String defaultValue, String allowedValues, String specialCharacters) {
        this.index = index;
        this.defaultValue = defaultValue;
        this.mandatory = mandatory;
        this.allowedValues = allowedValues;
        this.specialCharacters = specialCharacters;
    }

    /**
     * The localised name for this field, e.g. shown on its reset button. Kept separate from {@link #name()} (the
     * stable Java identifier used for lookups) so that translating the display text can never affect identity
     * comparisons.
     * @return the localised field name
     */
    public String getFieldName() {
        return StudioBundle.message("cron.field." + name());
    }

    public static final Map<String, String> dayOfWeek;
        static {
            Map<String, String> tempMap = new HashMap<>();
            String[] abbreviations = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
            for (int day = 1; day <= 7; day++) {
                String localisedName = StudioBundle.message("cron.weekday." + day);
                tempMap.put(String.valueOf(day), localisedName);
                tempMap.put(abbreviations[day - 1], localisedName);
            }
            dayOfWeek = Map.copyOf(tempMap); // making it unmodifiable
        }
    public static final Map<String, String> monthOfYear;
        static {
            Map<String, String> tempMap = new HashMap<>();
            String[] abbreviations = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
            for (int month = 1; month <= 12; month++) {
                String localisedName = StudioBundle.message("cron.month." + month);
                tempMap.put(String.valueOf(month), localisedName);
                tempMap.put(abbreviations[month - 1], localisedName);
            }
            monthOfYear = Map.copyOf(tempMap); // making it unmodifiable
        }

    /**
     * Build a single plain English sentence describing every field of a cron expression, e.g.
     * "Every second, at 30 minutes, at 14 hours, no specific day of month, every month, on Monday, every year."
     * Fields whose value can't be described (blank, or not yet recognised by {@link #describeField}) are
     * silently omitted, so a partially-edited expression still yields a best-effort sentence rather than none.
     * @param cronExpression the space separated cron expression, e.g. "0 30 14 ? * MON *"
     * @return a plain English description of the whole expression, or an empty string if nothing could be described
     */
    public static String describeCronExpression(String cronExpression) {
        if (cronExpression == null) {
            return "";
        }
        String[] fields = cronExpression.trim().split(" ");
        CronExpression[] allFields = CronExpression.values();

        StringBuilder result = new StringBuilder();
        for (int index = 0; index < fields.length && index < allFields.length; index++) {
            String fieldDescription = describeField(fields[index], allFields[index]);
            if (!fieldDescription.isEmpty()) {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(fieldDescription);
            }
        }
        if (result.isEmpty()) {
            return "";
        }
        result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
        result.append(".");
        return result.toString();
    }

    public static String describeField(String valueEntered, CronExpression cronField) {
        String description = "";
        Map<String, String> lookup = switch (cronField) {
            case DAY_OF_WEEK -> dayOfWeek;
            case MONTH -> monthOfYear;
            default -> null;
        };

        switch (cronField) {
            case SECONDS:
            case MINUTES:
            case HOURS:
            case MONTH:
            case YEARS:
            case DAY_OF_MONTH:
            case DAY_OF_WEEK:
                // Complex conversions first since these may contain -
                switch (cronField) {
                    case DAY_OF_MONTH:
                    case DAY_OF_WEEK:
                        description = noSpecific(valueEntered, cronField);
                        // description might not get set by the above, continue until it does get set
                        if (description.isEmpty()) {
                            description = last(valueEntered, cronField);
                        }
                        if (description.isEmpty()) {
                            description = weekday(valueEntered, cronField);
                        }

                        if (description.isEmpty() && cronField.equals(DAY_OF_WEEK) && valueEntered.contains("#")) {
                            String[] parts = valueEntered.split("#");
                            // 1 - 4 th
                            Integer weekNumber = Ints.tryParse(parts[1]);
                            // 1 - 7
                            String dayOfWeekString = dayOfWeek.get(parts[0]);
                            if (dayOfWeekString != null && weekNumber != null && weekNumber > 1 && weekNumber < 5) {
                                return StudioBundle.message("cron.nthWeekdayOfMonth", weekNumber, ordinal("" + weekNumber), dayOfWeekString);
                            }
                        }
                }
                if (description.isEmpty()) {
                    description = wildcard(valueEntered, cronField);
                }
                if (description.isEmpty()) {
                    description = at(valueEntered, cronField, lookup);
                }
                if (description.isEmpty()) {
                    description = every(valueEntered, cronField);
                }
                if (description.isEmpty()) {
                    description = list(valueEntered, lookup);
                }
                if (description.isEmpty()) {
                    description = range(valueEntered, cronField, lookup);
                }
                break;
        }
        return description;
    }

    /**
     * Allowed values;
     * day of month: Wn
     * @param field to examine
     * @param cronField to examine
     * @return text describing the weekday
     */
    private static String weekday(String field, CronExpression cronField) {
        String description = "";
        if (cronField.equals(DAY_OF_MONTH)) {
            // e.g. W12
            if (field.length() > 2 && field.endsWith("W")) {
                String possibleCount = field.substring(0, field.length() - 2);
                Integer day = Ints.tryParse(possibleCount);
                if (day != null && day > 0 && day < 32) {
                    description = StudioBundle.message("cron.weekday.nearest");
                }
            }
        }
        return description;
    }

    /**
     * Allowed values
     * day of month: L or L-n
     * day of week:  L or nL
     * @param field to examine
     * @param cronField to examine
     * @return text describing what this is the last of
     */
    private static String last(String field, CronExpression cronField) {
        String description = "";
        if (cronField.equals(DAY_OF_MONTH)) {
            // e.g. L-6
            if (field.length() > 2 && field.startsWith("L")) {
                String possibleCount = field.substring(1, field.length() - 1);
                Integer day = Ints.tryParse(possibleCount);
                if (day != null && day < 0 && (day * -1 < 32)) {
                    description = StudioBundle.message("cron.last.dayOfMonth", day * -1);
                }
            }
        } else if (cronField.equals(DAY_OF_WEEK)) {
            if (field.strip().equals("L")) {
                description = StudioBundle.message("cron.last.dayOfWeek", cronField.getFieldName().toLowerCase());

                // e.g. 5L
            } else if (field.length() > 1 && field.endsWith("L")) {
                String possibleCount = field.substring(0, field.length() - 2);
                String day = dayOfWeekFromString(possibleCount);
                if (day != null) {
                    description = StudioBundle.message("cron.last.nthDayOfWeek", day);
                }
            }
        }
        return description;
    }

    private static String dayOfWeekFromString(String dayString) {
        Integer day = Ints.tryParse(dayString);
        if (day != null) {
            return dayOfWeek.get(day.toString());
        }
        return null;
    }
    private static Integer dayOfMonthFromString(String dayString) {
        Integer day = Ints.tryParse(dayString);
        if (day!= null && day >0 && day < 32) {
            return day;
        }
        return null;
    }

    private static String wildcard(String field, CronExpression cronField) {
        if (field.equals("*")) {
            return StudioBundle.message("cron.every", cronField.getFieldName().toLowerCase());
        }
        return "";
    }

    private static String at(String field, CronExpression cronField, Map<String, String> lookup) {
        if (isNumeric(field)) {
            return StudioBundle.message("cron.at", cronField.getFieldName().toLowerCase(), (lookup != null ? lookup.get(field) : field));
        }
        return "";
    }

    private static String every(String field, CronExpression cronField) {
        if (field.contains("/")) {
            String[] parts = field.split("/");
            if (parts.length > 1) {
                return StudioBundle.message("cron.every.step", parts[1], cronField.getFieldName().toLowerCase(), parts[0]);
            }
        }
        return "";
    }
    private static String list(String field, Map<String, String> lookup) {
        if (field.contains(",")) {
            String[] parts = field.split(",");
            if (parts.length > 1) {
                StringBuilder joined = new StringBuilder();
                for (String part : parts) {
                    joined.append(lookup != null ? lookup.get(part) : part).append(", ");
                }
                return StudioBundle.message("cron.list", joined.substring(0, joined.length() - 2));
            }
        }
        return "";
    }

    private static String range(String field, CronExpression cronField, Map<String, String> lookup) {
        if (field.contains("-")) {
            String[] parts = field.split("-");
            if (parts.length > 1) {
                return StudioBundle.message("cron.range", cronField.getFieldName().toLowerCase(),
                        (lookup != null ? lookup.get(parts[0]) : parts[0]),
                        (lookup != null ? lookup.get(parts[1]) : parts[1]));
            }
        }
        return "";
    }
    private static String noSpecific(String field, CronExpression cronField) {
        if (field.equals("?")) {
            return StudioBundle.message("cron.noSpecific", cronField.getFieldName().toLowerCase());
        }
        return "";
    }

    private static String ordinal(String value) {
        int number = Integer.parseInt(value);
        if (number == 1 || number == 21 || number == 31) {
            return "st";
        } else if (number == 2 || number == 22) {
            return "nd";
        } else if (number == 3 || number == 23) {
            return "rd";
        } else {
            return "th";
        }
    }
}
