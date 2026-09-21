package com.JavaTraining.BaiTap_RS.assignment.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;

final class DemoAssignmentFixture {

    /* default */
    static final List<String> EXPECTED_CLASS_CODES = List.of(
            classCode(6, 1), classCode(6, 2), classCode(6, 3), classCode(6, 4),
            classCode(7, 1), classCode(7, 2), classCode(7, 3), classCode(7, 4),
            classCode(8, 1), classCode(8, 2), classCode(8, 3), classCode(8, 4),
            classCode(9, 1), classCode(9, 2), classCode(9, 3), classCode(9, 4));
    /* default */
    static final List<String> EXPECTED_TEACHER_CODES = teacherCodes(20);

    private DemoAssignmentFixture() {
    }

    /* default */
    static void validateRequiredReferences(
            Set<String> classCodes,
            Set<String> teacherCodes,
            Set<String> subjectCodes) {
        requireAll("class", EXPECTED_CLASS_CODES, classCodes);
        requireAll("teacher", EXPECTED_TEACHER_CODES, teacherCodes);
        requireAll("subject", DemoAssignmentRules.subjectEligibility().values().stream()
                .flatMap(Set::stream).distinct().sorted().toList(), subjectCodes);
    }

    /* default */
    static Map<String, Long> loadSubjectIds(SubjectRepository subjectRepository) {
        return subjectRepository.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(
                        subject -> subject.getCode(),
                        subject -> subject.getId(),
                        (left, right) -> {
                            throw new IllegalStateException("Duplicate subject code in seed catalog");
                        },
                        LinkedHashMap::new));
    }

    /* default */
    static <T> Map<String, T> indexByCode(
            List<T> items,
            Function<T, String> codeExtractor,
            String itemType) {
        if (items == null) {
            throw new IllegalStateException("Missing " + itemType + " list for Plan 081 assignments");
        }
        return items.stream().collect(Collectors.toMap(
                codeExtractor,
                Function.identity(),
                (left, right) -> {
                    throw new IllegalStateException("Duplicate " + itemType + " code: "
                            + codeExtractor.apply(left));
                },
                LinkedHashMap::new));
    }

    private static void requireAll(String itemType, List<String> expected, Set<String> actual) {
        List<String> missing = expected.stream()
                .filter(code -> !actual.contains(code))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing " + itemType + " data for Plan 081: " + missing);
        }
    }

    private static List<String> teacherCodes(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(DemoAssignmentFixture::teacherCode)
                .toList();
    }

    private static String teacherCode(int index) {
        return String.format("GV%03d", index);
    }

    private static String classCode(int grade, int section) {
        return grade + "A" + section;
    }
}
