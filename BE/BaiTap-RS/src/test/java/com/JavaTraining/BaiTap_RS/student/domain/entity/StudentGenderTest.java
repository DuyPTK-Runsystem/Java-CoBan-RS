package com.JavaTraining.BaiTap_RS.student.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class StudentGenderTest {

    @Test
    void studentInfoKeepsNullableAndApprovedGenderValues() {
        StudentInfo withoutGender = new StudentInfo(LocalDate.of(2012, 1, 1), null, null);
        StudentInfo female = new StudentInfo(
                LocalDate.of(2012, 1, 1), null, null, StudentGender.FEMALE);

        assertNull(withoutGender.getGender());
        assertEquals(StudentGender.FEMALE, female.getGender());
    }
}
