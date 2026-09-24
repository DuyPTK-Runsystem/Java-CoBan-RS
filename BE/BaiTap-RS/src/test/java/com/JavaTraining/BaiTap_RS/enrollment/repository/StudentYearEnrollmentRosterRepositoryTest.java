package com.JavaTraining.BaiTap_RS.enrollment.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.ClassTransferHistory;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:enrollment-roster-repository;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed.demo.enabled=false"
})
class StudentYearEnrollmentRosterRepositoryTest {

    private static final LocalDateTime ENROLLED_AT = LocalDateTime.of(2026, 9, 1, 8, 0);
    private static final LocalDateTime TRANSFERRED_AT = LocalDateTime.of(2026, 9, 10, 8, 0);

    @Autowired
    private StudentYearEnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassTransferHistoryRepository historyRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    private Long sourceClassId;
    private Long targetClassId;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAllInBatch();
        enrollmentRepository.deleteAllInBatch();
        schoolClassRepository.deleteAllInBatch();
        SchoolClass source = schoolClassRepository.saveAndFlush(
                new SchoolClass(10L, 1L, "10A", "10A", 40, SchoolClassStatus.ACTIVE));
        SchoolClass target = schoolClassRepository.saveAndFlush(
                new SchoolClass(10L, 1L, "10B", "10B", 40, SchoolClassStatus.ACTIVE));
        sourceClassId = source.getId();
        targetClassId = target.getId();
    }

    @Test
    void countsClassAtHistoricalInstantFromLatestTransfer() {
        StudentYearEnrollment enrollment = enrollment(101L, sourceClassId);
        historyRepository.saveAndFlush(new ClassTransferHistory(
                enrollment.getId(), null, sourceClassId, ENROLLED_AT, null, null));
        historyRepository.saveAndFlush(new ClassTransferHistory(
                enrollment.getId(), sourceClassId, targetClassId, TRANSFERRED_AT, "Chuyển lớp", null));

        assertEquals(1, enrollmentRepository.countRosterAt(sourceClassId, TRANSFERRED_AT.minusMinutes(1)));
        assertEquals(0, enrollmentRepository.countRosterAt(sourceClassId, TRANSFERRED_AT.plusMinutes(1)));
        assertEquals(1, enrollmentRepository.countRosterAt(targetClassId, TRANSFERRED_AT.plusMinutes(1)));
    }

    @Test
    void countsEnrollmentsWithoutTransferHistoryOnlyForTheirCurrentClass() {
        enrollment(201L, sourceClassId);
        enrollment(202L, sourceClassId);
        enrollment(203L, targetClassId);

        LocalDateTime queriedAt = ENROLLED_AT.plusHours(1);

        assertEquals(2, enrollmentRepository.countRosterAt(sourceClassId, queriedAt));
        assertEquals(1, enrollmentRepository.countRosterAt(targetClassId, queriedAt));
    }

    @Test
    void excludesEnrollmentAfterCompletionAndCountsInitialEnrollmentWithoutHistory() {
        StudentYearEnrollment completed = enrollment(102L, sourceClassId);
        completed.setCompletedAt(TRANSFERRED_AT);
        enrollmentRepository.saveAndFlush(completed);
        historyRepository.saveAndFlush(new ClassTransferHistory(
                completed.getId(), null, sourceClassId, ENROLLED_AT, null, null));

        assertEquals(1, enrollmentRepository.countRosterAt(sourceClassId, TRANSFERRED_AT.minusMinutes(1)));
        assertEquals(0, enrollmentRepository.countRosterAt(sourceClassId, TRANSFERRED_AT));
        assertEquals(0, enrollmentRepository.countRosterAt(sourceClassId, TRANSFERRED_AT.plusMinutes(1)));

        StudentYearEnrollment withoutHistory = enrollment(103L, sourceClassId);
        assertEquals(2, enrollmentRepository.countRosterAt(sourceClassId, ENROLLED_AT.plusHours(1)));
        assertEquals(EnrollmentStatus.ACTIVE, withoutHistory.getStatus());
    }

    private StudentYearEnrollment enrollment(Long studentId, Long classId) {
        StudentYearEnrollment enrollment = enrollmentRepository.saveAndFlush(new StudentYearEnrollment(
                studentId, 10L, classId, EnrollmentStatus.ACTIVE, ENROLLED_AT));
        ReflectionTestUtils.setField(enrollment, "updatedAt", ENROLLED_AT);
        return enrollment;
    }
}
