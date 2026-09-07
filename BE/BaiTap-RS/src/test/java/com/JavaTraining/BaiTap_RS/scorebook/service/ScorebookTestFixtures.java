package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import org.springframework.test.util.ReflectionTestUtils;

final class ScorebookTestFixtures {

    private ScorebookTestFixtures() {
    }

    /* default */ static Scorebook scorebook(ScorebookStatus status) {
        Scorebook scorebook = new Scorebook(20L, status);
        ReflectionTestUtils.setField(scorebook, "id", 90L);
        return scorebook;
    }

    /* default */ static ClassSubject classSubject(ClassSubjectStatus status) {
        ClassSubject classSubject = new ClassSubject(10L, 70L, 80L, status);
        ReflectionTestUtils.setField(classSubject, "id", 20L);
        return classSubject;
    }

    /* default */ static Subject subject(SubjectType type) {
        Subject subject = new Subject("SUBJ", "Môn học", type, ApplicationScope.CLASS, SubjectStatus.ACTIVE);
        ReflectionTestUtils.setField(subject, "id", 70L);
        return subject;
    }

    /* default */ static Semester semester(SemesterStatus status) {
        Semester semester = new Semester(
                10L,
                "HK1",
                "Học kỳ 1",
                1,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 12, 31),
                null,
                status);
        ReflectionTestUtils.setField(semester, "id", 80L);
        return semester;
    }

    /* default */ static AssessmentColumn column(Long id, AssessmentType type, AssessmentColumnStatus status) {
        AssessmentColumn column = new AssessmentColumn(
                90L,
                type,
                1,
                type.name(),
                type.standardWeight(),
                type.isRequiredByStructure());
        ReflectionTestUtils.setField(column, "id", id);
        column.setStatus(status);
        return column;
    }
}
