package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.Iterator;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicability;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectApplicabilityClassRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class ClassSubjectApplicabilityProvisioningServiceTest {

    @Test
    void createForGradeApplicabilityCreatesOnlyMissingClassSubjectsInSemester() {
        ClassSubjectRepository classSubjectRepository = Mockito.mock(ClassSubjectRepository.class);
        ClassSubjectApplicabilityClassRepository classRepository =
                Mockito.mock(ClassSubjectApplicabilityClassRepository.class);
        ClassSubjectApplicabilityProvisioningService service =
                new ClassSubjectApplicabilityProvisioningService(classSubjectRepository, classRepository);
        SchoolClass firstClass = schoolClass(101L, "6A");
        SchoolClass secondClass = schoolClass(102L, "6B");
        Mockito.when(classRepository.findAllByAcademicYearIdAndGradeLevelIdOrderByClassCodeAsc(10L, 1L))
                .thenReturn(List.of(firstClass, secondClass));
        Mockito.when(classSubjectRepository.findAllBySubjectIdAndSemesterIdAndClassIdIn(
                Mockito.eq(70L), Mockito.eq(80L), Mockito.anyCollection()))
                .thenReturn(List.of(new ClassSubject(101L, 70L, 80L, ClassSubjectStatus.ACTIVE)));

        service.createForGradeApplicability(applicability(), semester());

        Mockito.verify(classSubjectRepository).saveAll(Mockito.argThat(items -> matchesCreatedClassSubject(items)));
    }

    private boolean matchesCreatedClassSubject(Iterable<ClassSubject> items) {
        Iterator<ClassSubject> iterator = items.iterator();
        if (!iterator.hasNext()) {
            return false;
        }
        ClassSubject created = iterator.next();
        return !iterator.hasNext()
                && Long.valueOf(102L).equals(created.getClassId())
                && Long.valueOf(70L).equals(created.getSubjectId())
                && Long.valueOf(80L).equals(created.getSemesterId())
                && created.getStatus() == ClassSubjectStatus.ACTIVE;
    }

    private SubjectApplicability applicability() {
        SubjectApplicability applicability = new SubjectApplicability(
                70L, 80L, ApplicationScope.GRADE, 1L, null, SubjectApplicabilityStatus.ACTIVE);
        ReflectionTestUtils.setField(applicability, "id", 501L);
        return applicability;
    }

    private Semester semester() {
        return new Semester(10L, "HK1", "Học kỳ 1", 1,
                java.time.LocalDate.of(2026, 8, 15), java.time.LocalDate.of(2027, 5, 31),
                null, SemesterStatus.ACTIVE);
    }

    private SchoolClass schoolClass(Long id, String code) {
        SchoolClass schoolClass = new SchoolClass(10L, 1L, code, code, 40, SchoolClassStatus.ACTIVE);
        ReflectionTestUtils.setField(schoolClass, "id", id);
        return schoolClass;
    }
}
