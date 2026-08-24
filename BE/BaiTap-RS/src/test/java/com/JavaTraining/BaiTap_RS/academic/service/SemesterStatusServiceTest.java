package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDate;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SemesterStatusServiceTest {

        private static final String SEMESTER_CODE = "HK1";
        private static final String SEMESTER_NAME = "Học kỳ 1";
        private static final LocalDate START_DATE = LocalDate.of(2026, 8, 15);
        private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

        @Mock
        private SemesterRepository semesterRepository;

        @Mock
        private AcademicYearRepository academicYearRepository;

        @Mock
        private AcademicCatalogAuditService auditService;

        @Test
        void createSemesterWithNullStatusDefaultsToDraft() {
                SemesterService service = new SemesterService(
                                semesterRepository,
                                academicYearRepository,
                                new SemesterMapper(),
                                auditService);
                Mockito.when(academicYearRepository.findById(10L)).thenReturn(Optional.of(academicYear()));
                Mockito.when(semesterRepository.existsByAcademicYearIdAndCode(10L, SEMESTER_CODE)).thenReturn(false);
                Mockito.when(semesterRepository
                                .existsByAcademicYearIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                                10L, END_DATE, START_DATE))
                                .thenReturn(false);
                Mockito.when(semesterRepository.save(Mockito.any(Semester.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ResSemesterDTO result = service.createSemester(new ReqCreateSemesterDTO(
                                10L,
                                SEMESTER_CODE,
                                SEMESTER_NAME,
                                1,
                                START_DATE,
                                END_DATE,
                                null,
                                null));

                Assertions.assertEquals(SemesterStatus.DRAFT, result.status(), "null status must default to DRAFT");
        }

        @Test
        void createSemesterWithExplicitStatusPreservesStatus() {
                SemesterService service = new SemesterService(
                                semesterRepository,
                                academicYearRepository,
                                new SemesterMapper(),
                                auditService);
                Mockito.when(academicYearRepository.findById(10L)).thenReturn(Optional.of(academicYear()));
                Mockito.when(semesterRepository.existsByAcademicYearIdAndCode(10L, SEMESTER_CODE)).thenReturn(false);
                Mockito.when(semesterRepository
                                .existsByAcademicYearIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                                10L, END_DATE, START_DATE))
                                .thenReturn(false);
                Mockito.when(semesterRepository.save(Mockito.any(Semester.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ResSemesterDTO result = service.createSemester(new ReqCreateSemesterDTO(
                                10L,
                                SEMESTER_CODE,
                                SEMESTER_NAME,
                                1,
                                START_DATE,
                                END_DATE,
                                null,
                                SemesterStatus.ACTIVE));

                Assertions.assertEquals(SemesterStatus.ACTIVE, result.status(), "explicit status should be preserved");
        }

        @Test
        void updateSemesterWithNullStatusPreservesExistingStatus() {
                SemesterService service = new SemesterService(
                                semesterRepository,
                                academicYearRepository,
                                new SemesterMapper(),
                                auditService);
                Semester existing = semester();
                existing.setStatus(SemesterStatus.DRAFT);
                Mockito.when(semesterRepository.findById(80L)).thenReturn(Optional.of(existing));
                Mockito.when(academicYearRepository.findById(10L)).thenReturn(Optional.of(academicYear()));
                Mockito.when(semesterRepository.existsByAcademicYearIdAndCodeAndIdNot(10L, SEMESTER_CODE, 80L))
                                .thenReturn(false);
                Mockito.when(semesterRepository
                                .existsByAcademicYearIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                                                10L, END_DATE, START_DATE, 80L))
                                .thenReturn(false);

                ResSemesterDTO result = service.updateSemester(80L, new ReqUpdateSemesterDTO(
                                SEMESTER_CODE,
                                SEMESTER_NAME,
                                1,
                                START_DATE,
                                END_DATE,
                                null,
                                null));

                Assertions.assertEquals(SemesterStatus.DRAFT, result.status(),
                                "null status should preserve existing status");
        }

        @Test
        void updateSemesterWithExplicitStatusUpdatesStatus() {
                SemesterService service = new SemesterService(
                                semesterRepository,
                                academicYearRepository,
                                new SemesterMapper(),
                                auditService);
                Semester existing = semester();
                existing.setStatus(SemesterStatus.DRAFT);
                Mockito.when(semesterRepository.findById(80L)).thenReturn(Optional.of(existing));
                Mockito.when(academicYearRepository.findById(10L)).thenReturn(Optional.of(academicYear()));
                Mockito.when(semesterRepository.existsByAcademicYearIdAndCodeAndIdNot(10L, SEMESTER_CODE, 80L))
                                .thenReturn(false);
                Mockito.when(semesterRepository
                                .existsByAcademicYearIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                                                10L, END_DATE, START_DATE, 80L))
                                .thenReturn(false);

                ResSemesterDTO result = service.updateSemester(80L, new ReqUpdateSemesterDTO(
                                SEMESTER_CODE,
                                SEMESTER_NAME,
                                1,
                                START_DATE,
                                END_DATE,
                                null,
                                SemesterStatus.ACTIVE));

                Assertions.assertEquals(SemesterStatus.ACTIVE, result.status(), "explicit status should update status");
        }

        private AcademicYear academicYear() {
                AcademicYear year = new AcademicYear(
                                "2026-2027",
                                LocalDate.of(2026, 8, 1),
                                LocalDate.of(2027, 5, 31),
                                AcademicYearStatus.ACTIVE,
                                null);
                ReflectionTestUtils.setField(year, "id", 10L);
                return year;
        }

        private Semester semester() {
                Semester semester = new Semester(
                                10L,
                                SEMESTER_CODE,
                                SEMESTER_NAME,
                                1,
                                START_DATE,
                                END_DATE,
                                null,
                                SemesterStatus.ACTIVE);
                ReflectionTestUtils.setField(semester, "id", 80L);
                return semester;
        }
}
