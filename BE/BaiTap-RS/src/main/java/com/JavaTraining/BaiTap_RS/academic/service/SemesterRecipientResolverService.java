package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ClassSubjectIncompleteDetail;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterCompletenessSummaryDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterRecipientInfo;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings({
        "PMD.CouplingBetweenObjects",
        "PMD.CyclomaticComplexity",
        "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.GuardLogStatement"
})
public class SemesterRecipientResolverService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final SemesterNotificationTemplateService templateService;

    @Value("${spring.mail.username:}")
    private String defaultAdminEmail;

    public SemesterRecipientResolverService(
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            SemesterRepository semesterRepository,
            SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository,
            HomeroomAssignmentRepository homeroomAssignmentRepository,
            SemesterNotificationTemplateService templateService) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.semesterRepository = semesterRepository;
        this.subjectTeachingAssignmentRepository = subjectTeachingAssignmentRepository;
        this.homeroomAssignmentRepository = homeroomAssignmentRepository;
        this.templateService = templateService;
    }

    @Transactional(readOnly = true)
    public List<SemesterRecipientInfo> resolveRecipients(
            Long semesterId,
            String checkpointCode,
            SemesterCompletenessSummaryDTO summary,
            List<ClassSubjectIncompleteDetail> incompleteDetails) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterRecipientResolverService.class,
                        "SemesterRecipientResolverService.resolveRecipients");
        List<SemesterRecipientInfo> result = new ArrayList<>();
        Semester semester = semesterRepository.findById(semesterId).orElse(null);
        String semesterName = semester != null ? semester.getName() : "Học kỳ " + semesterId;

        // 1. Academic Office / Admin recipients
        resolveAcademicOfficeRecipients(semesterName, checkpointCode, summary, result);

        if (incompleteDetails == null || incompleteDetails.isEmpty()) {
            return result;
        }

        // 2. Subject Teacher recipients
        resolveSubjectTeacherRecipients(semesterName, checkpointCode, incompleteDetails, result);

        // 3. Homeroom Teacher recipients
        resolveHomeroomTeacherRecipients(semesterName, checkpointCode, incompleteDetails, result);

        return result;
    }

    private void resolveAcademicOfficeRecipients(
            String semesterName,
            String checkpointCode,
            SemesterCompletenessSummaryDTO summary,
            List<SemesterRecipientInfo> result) {
        List<User> adminUsers = userRepository.findAcademicOfficeAndAdminUsers();
        Set<String> processedEmails = new LinkedHashSet<>();

        String subject = templateService.buildAcademicOfficeSubject(semesterName, checkpointCode);
        List<String> allDetails = summary != null ? summary.details() : List.of();
        String body = templateService.buildAcademicOfficeBody(semesterName, checkpointCode, summary, allDetails);

        for (User user : adminUsers) {
            String email = null;
            Long teacherId = null;
            String name = user.getUsername();

            Optional<Teacher> teacherOpt = teacherRepository.findByUserId(user.getId());
            if (teacherOpt.isPresent()) {
                Teacher teacher = teacherOpt.get();
                email = teacher.getEmail();
                teacherId = teacher.getId();
                name = teacher.getTeacherName();
            } else if (user.getUsername() != null && user.getUsername().contains("@")) {
                email = user.getUsername();
            }

            if (email != null && !email.isBlank() && processedEmails.add(email)) {
                result.add(new SemesterRecipientInfo(
                        email,
                        "ACADEMIC_OFFICE",
                        teacherId,
                        name,
                        subject,
                        body,
                        allDetails));
            }
        }

        if (result.isEmpty() && defaultAdminEmail != null && !defaultAdminEmail.isBlank()) {
            result.add(new SemesterRecipientInfo(
                    defaultAdminEmail,
                    "ACADEMIC_OFFICE",
                    null,
                    "Phòng Giáo vụ",
                    subject,
                    body,
                    allDetails));
        }
    }

    private void resolveSubjectTeacherRecipients(
            String semesterName,
            String checkpointCode,
            List<ClassSubjectIncompleteDetail> incompleteDetails,
            List<SemesterRecipientInfo> result) {
        Map<Long, List<String>> issuesByTeacher = new HashMap<>();

        for (ClassSubjectIncompleteDetail detail : incompleteDetails) {
            Optional<SubjectTeachingAssignment> assignmentOpt =
                    subjectTeachingAssignmentRepository.findFirstByClassSubjectIdAndStatus(
                            detail.classSubjectId(), AssignmentStatus.ACTIVE);
            if (assignmentOpt.isPresent()) {
                Long teacherId = assignmentOpt.get().getTeacherId();
                issuesByTeacher.computeIfAbsent(teacherId, k -> new ArrayList<>())
                        .addAll(detail.issues());
            }
        }

        for (Map.Entry<Long, List<String>> entry : issuesByTeacher.entrySet()) {
            Long teacherId = entry.getKey();
            List<String> issues = entry.getValue();
            teacherRepository.findById(teacherId).ifPresent(teacher -> {
                String email = teacher.getEmail();
                if (email != null && !email.isBlank()) {
                    String subject = String.format("[Nhắc nhở nhập điểm] Học kỳ %s (Mốc %s)",
                            semesterName, checkpointCode);
                    String body = templateService.buildTeacherBody(
                            teacher.getTeacherName(),
                            semesterName,
                            checkpointCode,
                            "Giáo viên bộ môn",
                            issues);
                    result.add(new SemesterRecipientInfo(
                            email,
                            "SUBJECT_TEACHER",
                            teacherId,
                            teacher.getTeacherName(),
                            subject,
                            body,
                            issues));
                }
            });
        }
    }

    private void resolveHomeroomTeacherRecipients(
            String semesterName,
            String checkpointCode,
            List<ClassSubjectIncompleteDetail> incompleteDetails,
            List<SemesterRecipientInfo> result) {
        Map<Long, List<String>> issuesByTeacher = new HashMap<>();

        Map<Long, List<ClassSubjectIncompleteDetail>> detailsByClass = new HashMap<>();
        for (ClassSubjectIncompleteDetail detail : incompleteDetails) {
            detailsByClass.computeIfAbsent(detail.classId(), k -> new ArrayList<>()).add(detail);
        }

        for (Map.Entry<Long, List<ClassSubjectIncompleteDetail>> classEntry : detailsByClass.entrySet()) {
            Long classId = classEntry.getKey();
            List<ClassSubjectIncompleteDetail> classIssues = classEntry.getValue();

            Optional<HomeroomAssignment> homeroomOpt =
                    homeroomAssignmentRepository.findFirstByClassIdAndStatus(classId, AssignmentStatus.ACTIVE);
            if (homeroomOpt.isPresent()) {
                Long teacherId = homeroomOpt.get().getTeacherId();
                List<String> classIssueStrings = new ArrayList<>();
                for (ClassSubjectIncompleteDetail d : classIssues) {
                    classIssueStrings.addAll(d.issues());
                }
                issuesByTeacher.computeIfAbsent(teacherId, k -> new ArrayList<>())
                        .addAll(classIssueStrings);
            }
        }

        for (Map.Entry<Long, List<String>> entry : issuesByTeacher.entrySet()) {
            Long teacherId = entry.getKey();
            List<String> issues = entry.getValue();
            teacherRepository.findById(teacherId).ifPresent(teacher -> {
                String email = teacher.getEmail();
                if (email != null && !email.isBlank()) {
                    String subject = String.format("[Báo cáo tiến độ điểm lớp chủ nhiệm] Học kỳ %s", semesterName);
                    String body = templateService.buildTeacherBody(
                            teacher.getTeacherName(),
                            semesterName,
                            checkpointCode,
                            "Giáo viên chủ nhiệm",
                            issues);
                    result.add(new SemesterRecipientInfo(
                            email,
                            "HOMEROOM_TEACHER",
                            teacherId,
                            teacher.getTeacherName(),
                            subject,
                            body,
                            issues));
                }
            });
        }
    }
}
