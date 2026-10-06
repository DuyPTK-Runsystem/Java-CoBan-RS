package com.JavaTraining.BaiTap_RS.timetableagent.ai;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

public final class TimetableAgentJsonFields {

    private TimetableAgentJsonFields() {
    }

    public static void requireObjectWithExactFields(JsonNode value, Set<String> expected, String label) {
        if (value == null || !value.isObject()) {
            throw invalid(label + " must be an object");
        }
        Set<String> actual = new HashSet<>();
        value.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            throw invalid(label + " has missing or unknown fields");
        }
    }

    public static String requireText(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isTextual()) {
            throw invalid(field + " must be a string");
        }
        return value.textValue();
    }

    public static void requireNonBlankText(JsonNode object, String field) {
        if (requireText(object, field).isBlank()) {
            throw invalid(field + " must not be blank");
        }
    }

    public static void requirePositiveInteger(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (!isPositiveInteger(value)) {
            throw invalid(field + " must be a positive integer");
        }
    }

    public static void requireNullablePositiveInteger(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || (!value.isNull() && !isPositiveInteger(value))) {
            throw invalid(field + " must be a positive integer or null");
        }
    }

    private static boolean isPositiveInteger(JsonNode value) {
        return value != null && value.isIntegralNumber() && value.canConvertToLong() && value.longValue() > 0;
    }

    public static LocalDate requireDate(JsonNode object, String field) {
        String value = requireText(object, field);
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw invalid(field + " must use ISO date format");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new TimetableAgentInvalidOutputException("Model date must use ISO date format: " + field, exception);
        }
    }

    public static TimetableAgentInvalidOutputException invalid(String detail) {
        return new TimetableAgentInvalidOutputException("Model output did not match the timetable proposal contract: "
                + detail + ".");
    }
}
