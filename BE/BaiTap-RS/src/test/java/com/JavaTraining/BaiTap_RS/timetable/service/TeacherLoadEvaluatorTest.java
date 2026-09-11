package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadEligibility;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicy;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadEligibilityRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD")
class TeacherLoadEvaluatorTest {

        @Mock
        private TeacherRepository teacherRepository;

        @Mock
        private TeacherLoadPolicyService policyService;

        @Mock
        private TeacherLoadEligibilityRepository eligibilityRepository;

        @Mock
        private HomeroomAssignmentRepository homeroomRepository;

        @Mock
        private SubjectTeachingAssignmentRepository assignmentRepository;

        private TeacherLoadEvaluator evaluator;

        private TeacherLoadPolicy defaultPolicy;

        @BeforeEach
        void setUp() {
                evaluator = new TeacherLoadEvaluator(
                                teacherRepository, policyService, eligibilityRepository,
                                homeroomRepository, assignmentRepository);

                defaultPolicy = new TeacherLoadPolicy(
                                "QD-2026", "Quy định năm 2026",
                                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                                19, 4, 3);
                ReflectionTestUtils.setField(defaultPolicy, "id", 1L);
        }

        @Test
        void evaluateLoads_emptyEntries_returnsEmpty() {
                List<ResTeacherLoadDTO> res = evaluator.evaluateLoads(
                                List.of(), LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), null);
                Assertions.assertTrue(res.isEmpty());
        }

        @Test
        void evaluateLoads_standardTeacher_target19() {
                Mockito.when(policyService.getActivePolicy()).thenReturn(Optional.of(defaultPolicy));

                SubjectTeachingAssignment a1 = new SubjectTeachingAssignment(
                                1L, 100L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                ReflectionTestUtils.setField(a1, "id", 10L);
                Mockito.when(assignmentRepository.findAllById(List.of(10L))).thenReturn(List.of(a1));

                Teacher teacher = new Teacher(
                                1L, "GV100", "Nguyễn Văn A", LocalDate.of(1985, 1, 1), "MALE", "0900000000",
                                "a@school.edu.vn", "Toán", LocalDate.of(2015, 9, 1), TeacherStatus.ACTIVE);
                ReflectionTestUtils.setField(teacher, "id", 100L);
                Mockito.when(teacherRepository.findAllById(Mockito.anySet())).thenReturn(List.of(teacher));

                Mockito.when(homeroomRepository.findAllByTeacherIdOrderByValidFromDesc(100L))
                                .thenReturn(List.of());
                Mockito.when(eligibilityRepository.findActiveByTeacherIdAndDate(Mockito.eq(100L), Mockito.any()))
                                .thenReturn(List.of());

                // 19 entries assigned to teacher 100
                TimetableEntry e1 = new TimetableEntry(1L, 10L, 1L, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
                ReflectionTestUtils.setField(e1, "id", 1L);

                List<ResTeacherLoadDTO> list = evaluator.evaluateLoads(
                                List.of(e1), LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), null);

                Assertions.assertEquals(1, list.size());
                ResTeacherLoadDTO dto = list.get(0);
                Assertions.assertEquals(100L, dto.teacherId());
                Assertions.assertEquals(1, dto.assignedPeriods());
                Assertions.assertEquals(19, dto.basePeriods());
                Assertions.assertEquals(0, dto.reductions());
                Assertions.assertEquals(19, dto.targetPeriods());
                Assertions.assertEquals(-18, dto.difference());
                Assertions.assertEquals("LOAD_BELOW_TARGET", dto.evaluationStatus());
        }

        @Test
        void evaluateLoads_homeroomAndNursing_reducesTo12() {
                Mockito.when(policyService.getActivePolicy()).thenReturn(Optional.of(defaultPolicy));

                SubjectTeachingAssignment a1 = new SubjectTeachingAssignment(
                                1L, 100L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                ReflectionTestUtils.setField(a1, "id", 10L);
                Mockito.when(assignmentRepository.findAllById(List.of(10L))).thenReturn(List.of(a1));

                Teacher teacher = new Teacher(
                                1L, "GV100", "Cô Trần Thị B", LocalDate.of(1985, 1, 1), "FEMALE", "0900000000",
                                "b@school.edu.vn", "Văn", LocalDate.of(2015, 9, 1), TeacherStatus.ACTIVE);
                ReflectionTestUtils.setField(teacher, "id", 100L);
                Mockito.when(teacherRepository.findAllById(Mockito.anySet())).thenReturn(List.of(teacher));

                // Homeroom active
                HomeroomAssignment hr = new HomeroomAssignment(
                                1L, 100L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                Mockito.when(homeroomRepository.findAllByTeacherIdOrderByValidFromDesc(100L))
                                .thenReturn(List.of(hr));

                // Nursing child under 12m active
                TeacherLoadEligibility elig = new TeacherLoadEligibility(
                                100L, "NURSING_CHILD_UNDER_12M",
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), "Giấy khai sinh");
                Mockito.when(eligibilityRepository.findActiveByTeacherIdAndDate(Mockito.eq(100L), Mockito.any()))
                                .thenReturn(List.of(elig));

                TimetableEntry e1 = new TimetableEntry(1L, 10L, 1L, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
                ReflectionTestUtils.setField(e1, "id", 1L);

                List<ResTeacherLoadDTO> list = evaluator.evaluateLoads(
                                List.of(e1), LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), null);

                Assertions.assertEquals(1, list.size());
                ResTeacherLoadDTO dto = list.get(0);
                Assertions.assertEquals(19, dto.basePeriods());
                Assertions.assertEquals(7, dto.reductions()); // 4 homeroom + 3 nursing
                Assertions.assertEquals(12, dto.targetPeriods());
        }

        @Test
        void evaluateLoads_aboveTarget_statusAboveTarget() {
                Mockito.when(policyService.getActivePolicy()).thenReturn(Optional.of(defaultPolicy));

                SubjectTeachingAssignment a1 = new SubjectTeachingAssignment(
                                1L, 100L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                ReflectionTestUtils.setField(a1, "id", 10L);
                Mockito.when(assignmentRepository.findAllById(List.of(10L))).thenReturn(List.of(a1));

                Teacher teacher = new Teacher(
                                1L, "GV100", "Thầy Vũ Văn C", LocalDate.of(1985, 1, 1), "MALE", "0900000000",
                                "c@school.edu.vn", "Toán", LocalDate.of(2015, 9, 1), TeacherStatus.ACTIVE);
                teacher.setTeacherName("Thầy Vũ Văn C");
                ReflectionTestUtils.setField(teacher, "id", 100L);
                Mockito.when(teacherRepository.findAllById(Mockito.anySet())).thenReturn(List.of(teacher));

                Mockito.when(homeroomRepository.findAllByTeacherIdOrderByValidFromDesc(100L))
                                .thenReturn(List.of());
                Mockito.when(eligibilityRepository.findActiveByTeacherIdAndDate(Mockito.eq(100L), Mockito.any()))
                                .thenReturn(List.of());

                // 20 entries for teacher (target is 19)
                List<TimetableEntry> entries = java.util.stream.IntStream.range(0, 20)
                                .mapToObj(i -> {
                                        TimetableEntry e = new TimetableEntry(1L, 10L, (long) (i + 1), null,
                                                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
                                        ReflectionTestUtils.setField(e, "id", (long) (i + 1));
                                        return e;
                                }).toList();

                List<ResTeacherLoadDTO> list = evaluator.evaluateLoads(
                                entries, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), null);

                Assertions.assertEquals(1, list.size());
                ResTeacherLoadDTO dto = list.get(0);
                Assertions.assertEquals(20, dto.assignedPeriods());
                Assertions.assertEquals(19, dto.targetPeriods());
                Assertions.assertEquals(1, dto.difference());
                Assertions.assertEquals("LOAD_ABOVE_TARGET", dto.evaluationStatus());
        }
}
