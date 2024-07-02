package org.ikasan.studio.ui.component.properties;

//import org.quartz.CronExpression;

import com.google.common.primitives.Ints;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@Getter
public enum CronExpression {

    SECONDS(     0, "Seconds",      true, "*", "0-59",            "*  n  n1-n12   n1,n2,..  n1/n2"),
    MINUTES(     1, "Minutes",      true, "*", "0-59",            "*  n  n1-n12   n1,n2,..  n1/n2"),
    HOURS(       2, "Hours",        true, "*", "0-23",            "*  n  n1-n12   n1,n2,..  n1/n2"),
    DAY_OF_MONTH(3, "Day of Month", true, "?", "1-12 or JAN-DEC", "*  n  n1-n12   n1,n2,..  n1/n2  ?  L  W"),
    MONTH(       4, "Month",        true, "*", "1-31",            "*  n  n1-n12   n1,n2,..  n1/n2"),
    DAY_OF_WEEK( 5, "Day of Week",  true, "?", "1-7 or SUN-SAT",  "*  n  n1-n12   n1,n2,..  n1/n2  ?  L  #"),
    YEARS(       6, "Year",         true, "*", "1970-2099",       "*  n  n1-n12   n1,n2,..  n1/n2");

    int index;
    String fieldName;
    boolean mandatory;
    String defaultValue;
    String allowedValues;
    String specialCharacters;

    CronExpression(int index, String fieldName, boolean mandatory, String defaultValue, String allowedValues, String specialCharacters) {
        this.index = index;
        this.defaultValue = defaultValue;
        this.fieldName = fieldName;
        this.mandatory = mandatory;
        this.allowedValues = allowedValues;
        this.specialCharacters = specialCharacters;
    }

    public static final Map<String, String> dayOfWeek;
        static {
            Map<String, String> tempMap = new HashMap<>();
            tempMap.put("1", "Sunday");
            tempMap.put("2", "Monday");
            tempMap.put("3", "Tuesday");
            tempMap.put("4", "Wednesday");
            tempMap.put("5", "Thursday");
            tempMap.put("6", "Friday");
            tempMap.put("7", "Saturday");
            tempMap.put("SUN", "Sunday");
            tempMap.put("MON", "Monday");
            tempMap.put("TUE", "Tuesday");
            tempMap.put("WED", "Wednesday");
            tempMap.put("THU", "Thrsday");
            tempMap.put("FRI", "Friday");
            tempMap.put("SAT", "Saturday");
            dayOfWeek = Map.copyOf(tempMap); // making it unmodifiable
        }
    public static final Map<String, String> monthOfYear;
        static {
            Map<String, String> tempMap = new HashMap<>();
            tempMap.put("1", "January");
            tempMap.put("2", "February");
            tempMap.put("3", "March");
            tempMap.put("4", "April");
            tempMap.put("5", "May");
            tempMap.put("6", "June");
            tempMap.put("7", "July");
            tempMap.put("8", "August");
            tempMap.put("9", "September");
            tempMap.put("10", "October");
            tempMap.put("11", "November");
            tempMap.put("12", "December");
            tempMap.put("JAN", "January");
            tempMap.put("FEB", "February");
            tempMap.put("MAR", "March");
            tempMap.put("APR", "April");
            tempMap.put("MAY", "May");
            tempMap.put("JUN", "June");
            tempMap.put("JUL", "July");
            tempMap.put("AUG", "August");
            tempMap.put("SEP", "September");
            tempMap.put("OCT", "October");
            tempMap.put("NOV", "November");
            tempMap.put("DEC", "December");
            monthOfYear = Map.copyOf(tempMap); // making it unmodifiable
        }

    public static String explainCronExpression(String cronExpression) throws ParseException {
        String[] fields = cronExpression.split(" ");

        Map<CronExpression, String> explanations = new HashMap<>();
        for (int index = 0 ; index < fields.length ; index++) {
            CronExpression cronField = CronExpression.values()[index];
            explanations.put(cronField, fields[index]);
        }
        StringBuilder result = new StringBuilder();
        for (Map.Entry<CronExpression, String> entry : explanations.entrySet()) {
            result.append(entry.getKey() + ": " + describeField(entry.getValue(), entry.getKey()));
        }
        return result.toString();
    }

    public static CronExpression getFromName(String fieldName) {
        for (CronExpression cronField : CronExpression.values()) {
            if (cronField.fieldName.equals(fieldName)) {
                return cronField;
            }
        }
        return null;
    }

    public static String describeField(String valueEntered, String cronFieldString) {
        CronExpression cronField = getFromName(cronFieldString);
        if (cronField != null && valueEntered!= null && !valueEntered.isBlank()) {
            return describeField(valueEntered, cronField);
        }
        return "";
    }

    public static String describeField(String valueEntered, CronExpression cronField) {
        String description = "";
        Map<String, String> lookup = null;

        switch (cronField) {
            case DAY_OF_WEEK:
                lookup = dayOfWeek;
                break;
            case MONTH:
                lookup = monthOfYear;
                break;
        }
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
                        if (description.isEmpty()) {
                            description = noSpecific(valueEntered, cronField.fieldName);
                        }
                        if (description.isEmpty()) {
                            description = noSpecific(valueEntered, cronField.fieldName);
                        }
                        if (description.isEmpty()) {
                            description = last(valueEntered, cronField);
                        }
                        if (description.isEmpty()) {
                            description = weekday(valueEntered, cronField.fieldName);
                        }

                        if (description.isEmpty() && cronField.equals(DAY_OF_WEEK) && valueEntered.contains("#")) {
                            String[] parts = valueEntered.split("#");
                            // 1 - 4 th
                            Integer weekNumber = Ints.tryParse(parts[1]);
                            // 1 - 7
                            String dayOfWeekString = dayOfWeek.get(Ints.tryParse(parts[0]));
                            if (dayOfWeekString != null && weekNumber != null && weekNumber > 1 && weekNumber < 5) {
                                return "the " + weekNumber + ordinal("" + weekNumber) + " " + dayOfWeekString + " of the month";
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
     * @param field
     * @param fieldName
     * @return
     */
    protected static String weekday(String field, String fieldName) {
        String description = "";
        if (fieldName.equals(DAY_OF_MONTH)) {
            // e.g. W12
            if (description.isEmpty() && field.length() > 2 && field.endsWith("W")) {
                String possibleCount = field.substring(0, field.length() - 2);
                Integer day = Ints.tryParse(possibleCount);
                if (day != null && day > 0 && day < 32) {
                    description = "the nearest weekday in the current month";
                }
            }
        }
        return description;
    }

    /**
     * Allowed values\;
     * day of month: L or L-n
     * day of week:  L or nL
     * @param field
     * @param cronField
     * @return
     */
    protected static String last(String field, CronExpression cronField) {
        String description = "";
        if (cronField.equals(DAY_OF_MONTH)) {
            // e.g. L-6
            if (description.isEmpty() && field.length() > 2 && field.startsWith("L")) {
                String possibleCount = field.substring(1, field.length() - 1);
                Integer day = Ints.tryParse(possibleCount);
                if (day != null && day < 0 && (day * -1 < 32)) {
                    description = "" + (day * -1) + "days before the end of the month";
                }
            }
        } else if (cronField.equals(DAY_OF_WEEK)) {
            if (field.strip().equals("L")) {
                description = "the last " + cronField.fieldName.toLowerCase() ;

                // e.g. 5L
            } else if (field.length() > 1 && field.endsWith("L")) {
                String possibleCount = field.substring(0, field.length() - 2);
                String day = dayOfWeekFromString(possibleCount);
                if (day != null) {
                    description = "the last " + day + " of the month" ;
                }
            }
        }
        return description;
    }

    protected static String dayOfWeekFromString(String dayString) {
        Integer day = Ints.tryParse(dayString);
        if (day == null) {
            return dayOfWeek.get(day);
        }
        return null;
    }
    protected static Integer dayOfMonthFromString(String dayString) {
        Integer day = Ints.tryParse(dayString);
        if (day >0 && day < 32) {
            return day;
        }
        return null;
    }

    protected static String wildcard(String field, CronExpression cronField) {
        if (field.equals("*")) {
            return "every " + cronField.fieldName.toLowerCase();
        }
        return "";
    }

    protected static String at(String field, CronExpression cronField, Map<String, String> lookup) {
        if (StringUtils.isNumeric(field)) {
            return "at " + cronField.fieldName.toLowerCase() + " " + (lookup != null ? lookup.get(field) : field);
        }
        return "";
    }

    protected static String every(String field, CronExpression cronField) {
        if (field.contains("/")) {
            String[] parts = field.split("/");
            if (parts.length > 1) {
                return "every " + parts[1] + " " + cronField.fieldName.toLowerCase() + " starting at " + parts[0];
            }
        }
        return "";
    }
    protected static String list(String field, Map<String, String> lookup) {
        if (field.contains(",")) {
            String[] parts = field.split(",");
            if (parts.length > 1) {
                StringBuilder result = new StringBuilder("on ");
                for (String part : parts) {
                    result.append(lookup != null ? lookup.get(part) : part).append(", ");
                }
                return result.substring(0, result.length() - 2);
            }
        }
        return "";
    }

    protected static String range(String field, CronExpression cronField, Map<String, String> lookup) {
        if (field.contains("-")) {
            String[] parts = field.split("-");
            if (parts.length > 1) {
                return "every " + cronField.fieldName.toLowerCase() + " between " +
                        (lookup != null ? lookup.get(parts[0]) : parts[0]) + " and " +
                        (lookup != null ? lookup.get(parts[1]) : parts[1]);
            }
        }
        return "";
    }
    protected static String noSpecific(String field, String fieldName) {
        if (field.equals("?")) {
            return "no specific "+ fieldName.toLowerCase();
        };
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

