package com.JavaTraining.BaiTap_RS.notification.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class NotificationAudienceService {

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentYearEnrollmentRepository studentYearEnrollmentRepository;
    private final StudentRepository studentRepository;

    public NotificationAudienceService(
            UserRepository userRepository,
            SchoolClassRepository schoolClassRepository,
            StudentYearEnrollmentRepository studentYearEnrollmentRepository,
            StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentYearEnrollmentRepository = studentYearEnrollmentRepository;
        this.studentRepository = studentRepository;
    }

    public Set<Long> resolveAudienceUserIds(Notification notification, List<Long> explicitRecipientUserIds) {
        if (notification.getAudienceType() == NotificationAudienceType.INDIVIDUAL) {
            return resolveIndividualAudience(notification, explicitRecipientUserIds);
        } else if (notification.getAudienceType() == NotificationAudienceType.CLASS) {
            return resolveClassAudience(notification);
        } else if (notification.getAudienceType() == NotificationAudienceType.SCHOOL) {
            return resolveSchoolAudience();
        } else {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Loại đối tượng nhận thông báo không hợp lệ");
        }
    }

    private Set<Long> resolveIndividualAudience(Notification notification, List<Long> explicitRecipientUserIds) {
        Set<Long> userIds = new HashSet<>();
        if (explicitRecipientUserIds != null && !explicitRecipientUserIds.isEmpty()) {
            userIds.addAll(explicitRecipientUserIds);
        } else {
            userIds.addAll(parseTargetReferenceUserIds(notification.getTargetReference()));
        }

        if (userIds.isEmpty()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Danh sách người nhận cho thông báo cá nhân không được để trống");
        }

        List<User> foundUsers = userRepository.findAllById(userIds);
        if (foundUsers.size() != userIds.size()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Một hoặc nhiều người nhận không tồn tại trong hệ thống");
        }

        return foundUsers.stream().map(User::getId).collect(Collectors.toSet());
    }

    private Set<Long> parseTargetReferenceUserIds(String targetReference) {
        Set<Long> userIds = new HashSet<>();
        if (targetReference == null || targetReference.isBlank()) {
            return userIds;
        }
        String[] parts = targetReference.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    userIds.add(Long.parseLong(trimmed));
                } catch (NumberFormatException e) {
                    throw new AppException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "Mã người nhận không hợp lệ: " + trimmed,
                            e);
                }
            }
        }
        return userIds;
    }

    private Set<Long> resolveClassAudience(Notification notification) {
        String targetRef = notification.getTargetReference();
        if (targetRef == null || targetRef.isBlank()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Mã lớp không được để trống đối với thông báo theo lớp");
        }

        Long classId;
        try {
            classId = Long.parseLong(targetRef.trim());
        } catch (NumberFormatException e) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Mã lớp không hợp lệ: " + targetRef, e);
        }

        if (!schoolClassRepository.existsById(classId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học với ID: " + classId);
        }

        List<StudentYearEnrollment> enrollments = studentYearEnrollmentRepository
                .findByCurrentClassIdAndStatusOrderByStudentIdAsc(classId, EnrollmentStatus.ACTIVE);

        if (enrollments.isEmpty()) {
            return Set.of();
        }

        List<Long> studentIds = enrollments.stream().map(StudentYearEnrollment::getStudentId).toList();
        List<Student> students = studentRepository.findAllById(studentIds);

        return students.stream()
                .map(Student::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Long> resolveSchoolAudience() {
        return userRepository.findAll().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }
}
