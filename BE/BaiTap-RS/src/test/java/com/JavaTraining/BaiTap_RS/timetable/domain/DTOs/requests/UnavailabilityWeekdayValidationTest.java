package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UnavailabilityWeekdayValidationTest {

    @Test
    void createAndUpdateValidateIsoWeekdayBoundsAndAllowSpecificDateWithoutWeekday() throws Exception {
        List<ValidationCase> validationCases = validationCases();
        List<ValidationResult> expectedResults = validationCases.stream()
                .map(validationCase -> new ValidationResult(
                        validationCase.name(), validationCase.expectedViolations()))
                .toList();
        List<ValidationResult> actualResults;
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            actualResults = validationCases.stream()
                    .map(validationCase -> new ValidationResult(
                            validationCase.name(),
                            validator.validate(validationCase.request()).stream()
                                    .map(violation -> violation.getPropertyPath().toString())
                                    .sorted()
                                    .collect(Collectors.toList())))
                    .toList();
        }
        Assertions.assertEquals(expectedResults, actualResults,
                "create and update must accept ISO weekdays 1 and 7, reject 0 and 8, and allow a specific date");
    }

    private static List<ValidationCase> validationCases() {
        List<String> noViolations = List.of();
        List<String> weekdayViolation = List.of("dayOfWeek");
        LocalDate specificDate = LocalDate.of(2026, 10, 5);
        return List.of(
                new ValidationCase("create Monday", createRequest(1, null), noViolations),
                new ValidationCase("create Sunday", createRequest(7, null), noViolations),
                new ValidationCase("create specific date", createRequest(null, specificDate), noViolations),
                new ValidationCase("create weekday zero", createRequest(0, null), weekdayViolation),
                new ValidationCase("create weekday eight", createRequest(8, null), weekdayViolation),
                new ValidationCase("update Monday", updateRequest(1, null), noViolations),
                new ValidationCase("update Sunday", updateRequest(7, null), noViolations),
                new ValidationCase("update specific date", updateRequest(null, specificDate), noViolations),
                new ValidationCase("update weekday zero", updateRequest(0, null), weekdayViolation),
                new ValidationCase("update weekday eight", updateRequest(8, null), weekdayViolation));
    }

    private static ReqCreateUnavailabilityDTO createRequest(Integer weekday, LocalDate specificDate) {
        return new ReqCreateUnavailabilityDTO(1L, 2L, weekday, specificDate,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                SessionType.MORNING, "1", null);
    }

    private static ReqUpdateUnavailabilityDTO updateRequest(Integer weekday, LocalDate specificDate) {
        return new ReqUpdateUnavailabilityDTO(1L, weekday, specificDate,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                SessionType.MORNING, "1", null);
    }

    private record ValidationCase(String name, Object request, List<String> expectedViolations) {
    }

    private record ValidationResult(String name, List<String> violations) {
    }
}
