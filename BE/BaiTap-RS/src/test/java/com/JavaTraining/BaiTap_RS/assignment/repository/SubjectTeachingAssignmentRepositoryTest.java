package com.JavaTraining.BaiTap_RS.assignment.repository;

import java.time.LocalDate;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assignment-scorebook-context;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class SubjectTeachingAssignmentRepositoryTest {

    private static final Long TEACHER_ID = 200L;
    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 9, 29);

    @Autowired
    private EffectiveSubjectTeachingAssignmentRepository repository;

    @BeforeEach
    void clearAssignments() {
        repository.deleteAll();
    }

    @Test
    void findEffectiveByTeacherIncludesInclusiveDateBoundariesAndOpenEndedAssignmentsOnly() {
        save(20L, EFFECTIVE_DATE, EFFECTIVE_DATE, AssignmentStatus.ACTIVE, TEACHER_ID);
        save(21L, EFFECTIVE_DATE.minusDays(1), null, AssignmentStatus.ACTIVE, TEACHER_ID);
        save(22L, EFFECTIVE_DATE.plusDays(1), null, AssignmentStatus.ACTIVE, TEACHER_ID);
        save(23L, EFFECTIVE_DATE.minusDays(2), EFFECTIVE_DATE.minusDays(1), AssignmentStatus.ACTIVE, TEACHER_ID);
        save(24L, EFFECTIVE_DATE.minusDays(1), null, AssignmentStatus.ENDED, TEACHER_ID);
        save(25L, EFFECTIVE_DATE.minusDays(1), null, AssignmentStatus.ACTIVE, 201L);

        Set<Long> classSubjectIds = repository.findEffectiveByTeacherId(TEACHER_ID, EFFECTIVE_DATE)
                .stream()
                .map(SubjectTeachingAssignment::getClassSubjectId)
                .collect(java.util.stream.Collectors.toSet());

        Assertions.assertEquals(Set.of(20L, 21L), classSubjectIds);
    }

    private void save(
            Long classSubjectId,
            LocalDate validFrom,
            LocalDate validTo,
            AssignmentStatus status,
            Long teacherId) {
        repository.save(new SubjectTeachingAssignment(
                classSubjectId, teacherId, validFrom, validTo, status, 1L));
    }
}
