package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSchoolClassDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SchoolClassTranscriptAccessService {

    private final SchoolClassRepository schoolClassRepository;
    private final TeacherRepository teacherRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;

    /* default */ List<ResSchoolClassDTO> listAccessibleClasses(Long academicYearId) {
        if (hasAnyRole("ADMIN", "ACADEMIC_OFFICE")) {
            List<SchoolClass> classes = academicYearId == null
                    ? schoolClassRepository.findAll(Sort.by(Sort.Direction.ASC, "classCode"))
                    : schoolClassRepository.findAllByAcademicYearIdOrderByClassCodeAsc(academicYearId);
            return classes.stream().map(this::toResponse).toList();
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
                : schoolClassRepository.findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(classIds, academicYearId);
        return classes.stream().map(this::toResponse).toList();
    }

    private boolean hasAnyRole(String... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> List.of(roles).stream()
                        .anyMatch(role -> authority.getAuthority().equals("ROLE_" + role)));
    }

    private ResSchoolClassDTO toResponse(SchoolClass schoolClass) {
        return new ResSchoolClassDTO(
                schoolClass.getId(), schoolClass.getAcademicYearId(), schoolClass.getGradeLevelId(),
                schoolClass.getClassCode(), schoolClass.getClassName(), schoolClass.getCapacity(),
                schoolClass.getStatus());
    }
}
