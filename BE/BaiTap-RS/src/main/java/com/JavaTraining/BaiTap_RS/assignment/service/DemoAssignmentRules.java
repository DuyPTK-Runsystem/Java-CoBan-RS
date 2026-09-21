package com.JavaTraining.BaiTap_RS.assignment.service;

import java.util.Map;
import java.util.Set;

final class DemoAssignmentRules {

    private static final int MAX_WEEKLY_PERIODS = 24;
    private static final int STANDARD_WEEKLY_PERIODS = 19;
    private static final int HOMEROOM_REDUCTION = 4;
    private static final String HK2 = "HK2";
    private static final String NGHE_DIEN = "NGHE_DIEN";
    private static final String NGHE_NONG_NGHIEP = "NGHE_NONG_NGHIEP";
    private static final String HOA_HOC = "HOA_HOC";
    private static final String CONG_NGHE = "CONG_NGHE";
    private static final String TOAN = "TOAN";
    private static final String VAT_LY = "VAT_LY";
    private static final String SINH_HOC = "SINH_HOC";
    private static final String NGU_VAN = "NGU_VAN";
    private static final String NGOAI_NGU = "NGOAI_NGU";
    private static final String LICH_SU = "LICH_SU";
    private static final String DIA_LY = "DIA_LY";
    private static final String GDCD = "GDCD";
    private static final String TIN_HOC = "TIN_HOC";

    private static final Map<String, Set<String>> SUBJECTS_BY_TEACHER = Map.ofEntries(
            Map.entry("GV001", Set.of(TOAN, TIN_HOC)),
            Map.entry("GV002", Set.of(NGU_VAN, LICH_SU)),
            Map.entry("GV003", Set.of(VAT_LY, CONG_NGHE)),
            Map.entry("GV004", Set.of(HOA_HOC, SINH_HOC)),
            Map.entry("GV005", Set.of(NGOAI_NGU, GDCD)),
            Map.entry("GV006", Set.of(DIA_LY, NGOAI_NGU)),
            Map.entry("GV007", Set.of(TOAN, NGU_VAN)),
            Map.entry("GV008", Set.of(VAT_LY, TIN_HOC)),
            Map.entry("GV009", Set.of(HOA_HOC, SINH_HOC)),
            Map.entry("GV010", Set.of(CONG_NGHE, NGHE_DIEN)),
            Map.entry("GV011", Set.of(NGHE_NONG_NGHIEP, GDCD)),
            Map.entry("GV012", Set.of(TOAN, DIA_LY)),
            Map.entry("GV013", Set.of(NGU_VAN, LICH_SU)),
            Map.entry("GV014", Set.of(VAT_LY, HOA_HOC)),
            Map.entry("GV015", Set.of(SINH_HOC, TIN_HOC)),
            Map.entry("GV016", Set.of(NGOAI_NGU, GDCD)),
            Map.entry("GV017", Set.of(DIA_LY, CONG_NGHE)),
            Map.entry("GV018", Set.of(TOAN, NGOAI_NGU)),
            Map.entry("GV019", Set.of(NGU_VAN, NGHE_DIEN)),
            Map.entry("GV020", Set.of(LICH_SU, NGHE_NONG_NGHIEP)));

    private static final Map<String, int[]> WEEKLY_PERIODS = Map.ofEntries(
            Map.entry(TOAN, new int[] {4, 4, 4, 4}),
            Map.entry(VAT_LY, new int[] {2, 2, 2, 2}),
            Map.entry(HOA_HOC, new int[] {0, 0, 2, 2}),
            Map.entry(SINH_HOC, new int[] {2, 2, 2, 2}),
            Map.entry(NGU_VAN, new int[] {4, 4, 4, 5}),
            Map.entry(NGOAI_NGU, new int[] {3, 3, 3, 2}),
            Map.entry(LICH_SU, new int[] {1, 2, 2, 1}),
            Map.entry(DIA_LY, new int[] {1, 2, 2, 1}),
            Map.entry(GDCD, new int[] {1, 1, 1, 1}),
            Map.entry(TIN_HOC, new int[] {2, 2, 2, 2}),
            Map.entry(CONG_NGHE, new int[] {2, 2, 1, 1}));

    private DemoAssignmentRules() {
    }

    /* default */ static Map<String, Set<String>> subjectEligibility() {
        return SUBJECTS_BY_TEACHER;
    }

    /* default */ static void validateEligibilityLimits() {
        SUBJECTS_BY_TEACHER.forEach((teacher, subjects) -> {
            if (subjects.size() > 2) {
                throw new IllegalStateException("Teacher " + teacher + " exceeds 2 subjects in A.5");
            }
        });
    }

    /* default */ static int weeklyPeriods(String subjectCode, int grade, String semesterCode) {
        if (isSkill(subjectCode)) {
            return skillPeriods(grade, semesterCode);
        }
        if (grade < 6 || grade > 9 || !WEEKLY_PERIODS.containsKey(subjectCode)) {
            return 0;
        }
        if (excludedByScope(subjectCode, grade, semesterCode)) {
            return 0;
        }
        return WEEKLY_PERIODS.get(subjectCode)[grade - 6];
    }

    private static int skillPeriods(int grade, String semesterCode) {
        return HK2.equals(semesterCode) && grade >= 8 ? 2 : 0;
    }

    private static boolean excludedByScope(String subjectCode, int grade, String semesterCode) {
        return HOA_HOC.equals(subjectCode) && grade < 8
                || CONG_NGHE.equals(subjectCode) && HK2.equals(semesterCode) && grade == 9;
    }

    /* default */ static boolean isSkill(String subjectCode) {
        return NGHE_DIEN.equals(subjectCode) || NGHE_NONG_NGHIEP.equals(subjectCode);
    }

    /* default */ static String skillForClass(String classCode) {
        int section = Character.digit(classCode.charAt(classCode.length() - 1), 10);
        return section % 2 == 1 ? NGHE_DIEN : NGHE_NONG_NGHIEP;
    }

    /* default */ static int minimumWeeklyPeriods(String teacherCode) {
        int teacherNumber = Integer.parseInt(teacherCode.substring(2));
        return teacherNumber <= 16 ? STANDARD_WEEKLY_PERIODS - HOMEROOM_REDUCTION : STANDARD_WEEKLY_PERIODS;
    }

    /* default */ static int maxWeeklyPeriods() {
        return MAX_WEEKLY_PERIODS;
    }

    /* default */ static boolean teacherCanTeach(String teacherCode, String subjectCode) {
        return SUBJECTS_BY_TEACHER.getOrDefault(teacherCode, Set.of()).contains(subjectCode);
    }
}
