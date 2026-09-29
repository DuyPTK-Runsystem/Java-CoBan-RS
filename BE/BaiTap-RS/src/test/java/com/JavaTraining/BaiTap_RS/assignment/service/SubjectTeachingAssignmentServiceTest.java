package com.JavaTraining.BaiTap_RS.assignment.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.response.ResSubjectTeachingAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.EffectiveSubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubjectTeachingAssignmentServiceTest {

    @Mock
    private SubjectTeachingAssignmentRepository assignmentRepository;

    @Mock
    private EffectiveSubjectTeachingAssignmentRepository effectiveAssignmentRepository;

    @Mock
    private SubjectTeachingAssignmentGuard guard;

    @Mock
    private AssignmentAuditService auditService;

    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @InjectMocks
    private SubjectTeachingAssignmentService service;

    @Test
    void effectiveScorebookAssignmentsIncludeAcademicYearAndAssignmentContext() {
        LocalDate effectiveDate = LocalDate.of(2026, 9, 29);
        SubjectTeachingAssignment assignment = new SubjectTeachingAssignment(
                20L, 200L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
        ClassSubject classSubject = new ClassSubject(3L, 9L, 2L, ClassSubjectStatus.ACTIVE);
        SchoolClass schoolClass = new SchoolClass(
                1L, 6L, "6A1", "Lớp 6A1", 35, SchoolClassStatus.ACTIVE);
        Subject subject = new Subject("TOAN", "Toán", SubjectType.ACADEMIC, ApplicationScope.GRADE, SubjectStatus.ACTIVE);
        Semester semester = new Semester(
                1L, "HK1", "Học kỳ 1", 1, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), null,
                SemesterStatus.ACTIVE);
        ReflectionTestUtils.setField(classSubject, "id", 20L);
        ReflectionTestUtils.setField(schoolClass, "id", 3L);
        ReflectionTestUtils.setField(subject, "id", 9L);
        ReflectionTestUtils.setField(semester, "id", 2L);

        Mockito.when(effectiveAssignmentRepository.findEffectiveByTeacherId(200L, effectiveDate))
                .thenReturn(List.of(assignment));
        Mockito.when(classSubjectRepository.findById(20L)).thenReturn(Optional.of(classSubject));
        Mockito.when(schoolClassRepository.findById(3L)).thenReturn(Optional.of(schoolClass));
        Mockito.when(subjectRepository.findById(9L)).thenReturn(Optional.of(subject));
        Mockito.when(semesterRepository.findById(2L)).thenReturn(Optional.of(semester));

        List<ResSubjectTeachingAssignmentDTO> results = service.listEffectiveScorebookAssignments(200L, effectiveDate);

        Assertions.assertEquals(1, results.size());
        ResSubjectTeachingAssignmentDTO result = results.get(0);
        Assertions.assertEquals(20L, result.classSubjectId());
        Assertions.assertEquals(3L, result.classId());
        Assertions.assertEquals(9L, result.subjectId());
        Assertions.assertEquals(2L, result.semesterId());
        Assertions.assertEquals(1L, result.academicYearId());
    }
}
