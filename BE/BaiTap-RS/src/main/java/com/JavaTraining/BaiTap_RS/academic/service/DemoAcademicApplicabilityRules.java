package com.JavaTraining.BaiTap_RS.academic.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;

final class DemoAcademicApplicabilityRules {

    private static final String HK2 = "HK2";
    private static final String HOA_HOC = "HOA_HOC";
    private static final String CONG_NGHE = "CONG_NGHE";
    private static final String NGHE_DIEN = "NGHE_DIEN";
    private static final String NGHE_NONG_NGHIEP = "NGHE_NONG_NGHIEP";

    private DemoAcademicApplicabilityRules() {
    }

    /* default */ static boolean isApplicable(Subject subject, int grade, String semesterCode) {
        if (isSkill(subject)) {
            return grade >= 8 && HK2.equals(semesterCode);
        }
        if (HOA_HOC.equals(subject.getCode())) {
            return grade >= 8;
        }
        return !(CONG_NGHE.equals(subject.getCode()) && grade == 9 && HK2.equals(semesterCode));
    }

    /* default */ static boolean isApplicableForClass(
            Subject subject,
            SchoolClass schoolClass,
            int grade,
            String semesterCode) {
        return isApplicable(subject, grade, semesterCode)
                && (!isSkill(subject) || skillForClass(schoolClass).equals(subject.getCode()));
    }

    private static boolean isSkill(Subject subject) {
        return NGHE_DIEN.equals(subject.getCode()) || NGHE_NONG_NGHIEP.equals(subject.getCode());
    }

    private static String skillForClass(SchoolClass schoolClass) {
        String classCode = schoolClass.getClassCode();
        int section = Character.digit(classCode.charAt(classCode.length() - 1), 10);
        return section % 2 == 1 ? NGHE_DIEN : NGHE_NONG_NGHIEP;
    }
}
