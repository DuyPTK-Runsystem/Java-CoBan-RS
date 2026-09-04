package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSchoolClassDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSchoolClassDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSchoolClassDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class SchoolClassService {

        private final AcademicYearRepository academicYearRepository;
        private final GradeLevelRepository gradeLevelRepository;
        private final SchoolClassRepository schoolClassRepository;
        private final SchoolClassValidator validator;
        private final TeacherRepository teacherRepository;
        private final HomeroomAssignmentRepository homeroomAssignmentRepository;

        public SchoolClassService(
                        AcademicYearRepository academicYearRepository,
                        GradeLevelRepository gradeLevelRepository,
                        SchoolClassRepository schoolClassRepository,
                        SchoolClassValidator validator,
                        TeacherRepository teacherRepository,
                        HomeroomAssignmentRepository homeroomAssignmentRepository) {
                this.academicYearRepository = academicYearRepository;
                this.gradeLevelRepository = gradeLevelRepository;
                this.schoolClassRepository = schoolClassRepository;
                this.validator = validator;
                this.teacherRepository = teacherRepository;
                this.homeroomAssignmentRepository = homeroomAssignmentRepository;
        }

        @Transactional(readOnly = true)
        public List<ResSchoolClassDTO> listSchoolClasses(Long academicYearId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                                SchoolClassService.class,
                                "SchoolClassService.listSchoolClasses");
                List<SchoolClass> classes = academicYearId == null
                                ? schoolClassRepository.findAll(Sort.by(Sort.Direction.ASC, "classCode"))
                                : schoolClassRepository.findAllByAcademicYearIdOrderByClassCodeAsc(academicYearId);
                return classes.stream().map(this::toResponse).toList();
        }

        @Transactional
        public ResSchoolClassDTO createSchoolClass(ReqCreateSchoolClassDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                                SchoolClassService.class,
                                "SchoolClassService.createSchoolClass");
                AcademicYear year = findAcademicYear(request.academicYearId());
                GradeLevel grade = findGradeLevel(request.gradeLevelId());
                validator.validateClassCreation(year, grade, request.status());
                validator.rejectDuplicateClassCode(request.academicYearId(), request.classCode(), null);
                SchoolClass schoolClass = new SchoolClass(
                                request.academicYearId(),
                                request.gradeLevelId(),
                                request.classCode(),
                                request.className(),
                                request.capacity(),
                                request.status());
                return toResponse(schoolClassRepository.save(schoolClass));
        }

        @Transactional
        public ResSchoolClassDTO updateSchoolClass(Long id, ReqUpdateSchoolClassDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                                SchoolClassService.class,
                                "SchoolClassService.updateSchoolClass");
                SchoolClass schoolClass = findSchoolClass(id);
                validator.ensureClassNotClosed(schoolClass);
                AcademicYear year = findAcademicYear(schoolClass.getAcademicYearId());
                GradeLevel grade = findGradeLevel(request.gradeLevelId());
                validator.validateClassCreation(year, grade, request.status());
                validator.rejectDuplicateClassCode(schoolClass.getAcademicYearId(), request.classCode(), id);
                validator.rejectGradeChangeWithEnrollment(id, request.gradeLevelId(), schoolClass.getGradeLevelId());
                schoolClass.setGradeLevelId(request.gradeLevelId());
                schoolClass.setClassCode(request.classCode());
                schoolClass.setClassName(request.className());
                schoolClass.setCapacity(request.capacity());
                schoolClass.setStatus(request.status());
                return toResponse(schoolClass);
        }

        // BR-CLASS-006: lớp đóng chỉ được xem và không nhận enrollment mới.
        @Transactional
        public ResSchoolClassDTO closeSchoolClass(Long id) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                                SchoolClassService.class,
                                "SchoolClassService.closeSchoolClass");
                SchoolClass schoolClass = findSchoolClass(id);
                validator.ensureClassNotClosed(schoolClass);
                schoolClass.setStatus(SchoolClassStatus.CLOSED);
                return toResponse(schoolClass);
        }

        // BR-CLASS-011: không xóa lớp đã phát sinh enrollment hoặc lịch sử.
        @Transactional
        public void deleteSchoolClass(Long id) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                                SchoolClassService.class,
                                "SchoolClassService.deleteSchoolClass");
                SchoolClass schoolClass = findSchoolClass(id);
                validator.validateDelete(id);
                schoolClassRepository.delete(schoolClass);
        }

        private AcademicYear findAcademicYear(Long id) {
                return academicYearRepository.findById(id)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy năm học"));
        }

        private GradeLevel findGradeLevel(Long id) {
                return gradeLevelRepository.findById(id)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khối"));
        }

        private SchoolClass findSchoolClass(Long id) {
                return schoolClassRepository.findById(id)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp"));
        }

        @Transactional(readOnly = true)
        public List<ResSchoolClassDTO> listAccessibleClassesForTranscript(Long academicYearId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                                SchoolClassService.class,
                                "SchoolClassService.listAccessibleClassesForTranscript");
                if (hasAnyRole("ADMIN", "ACADEMIC_OFFICE")) {
                        return listSchoolClasses(academicYearId);
                }
                if (!hasAnyRole("TEACHER")) {
                        return List.of();
                }
                Long currentUserId = AuditContext.currentUserId();
                if (currentUserId == null) {
                        return List.of();
                }
                Teacher teacher = teacherRepository.findByUserId(currentUserId).orElse(null);
                if (teacher == null) {
                        return List.of();
                }
                List<Long> classIds = homeroomAssignmentRepository.findClassIdsByTeacherIdAndStatus(
                                teacher.getId(), AssignmentStatus.ACTIVE);
                if (classIds.isEmpty()) {
                        return List.of();
                }
                List<SchoolClass> classes = academicYearId == null
                                ? schoolClassRepository.findAllByIdInOrderByClassCodeAsc(classIds)
                                : schoolClassRepository.findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(classIds,
                                                academicYearId);
                return classes.stream().map(this::toResponse).toList();
        }

        private boolean hasAnyRole(String... roles) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null) {
                        return false;
                }
                for (String role : roles) {
                        boolean matched = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
                        if (matched) {
                                return true;
                        }
                }
                return false;
        }

        private ResSchoolClassDTO toResponse(SchoolClass schoolClass) {
                return new ResSchoolClassDTO(
                                schoolClass.getId(),
                                schoolClass.getAcademicYearId(),
                                schoolClass.getGradeLevelId(),
                                schoolClass.getClassCode(),
                                schoolClass.getClassName(),
                                schoolClass.getCapacity(),
                                schoolClass.getStatus());
        }

}
