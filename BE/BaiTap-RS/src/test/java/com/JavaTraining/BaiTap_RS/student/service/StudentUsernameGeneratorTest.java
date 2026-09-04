package com.JavaTraining.BaiTap_RS.student.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StudentUsernameGeneratorTest {

    private final StudentUsernameGenerator generator = new StudentUsernameGenerator();

    @Test
    void generateRemovesVietnameseDiacriticsAndAppendsStudentCodeSuffix() {
        Assertions.assertEquals(
                "khanhduy1234567",
                generator.generate("Khánh Duy", "STU1234567"),
                "short normalized name should be used directly");
    }

    @Test
    void generateUsesNameInitialsWhenFullNameWouldExceedUsernameLimit() {
        Assertions.assertEquals(
                "ptkd1234567",
                generator.generate("Phạm Trần Khánh Duy", "STU1234567"),
                "long normalized name should fall back to initials");
    }
}
