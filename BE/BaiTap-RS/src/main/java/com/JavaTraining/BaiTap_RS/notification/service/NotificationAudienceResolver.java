package com.JavaTraining.BaiTap_RS.notification.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationAudienceProjectionRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.http.HttpStatus;

public class NotificationAudienceResolver {

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final NotificationAudienceProjectionRepository projectionRepository;
    private final NotificationAudienceCanonicalizer canonicalizer;

    public NotificationAudienceResolver(
            UserRepository userRepository,
            SchoolClassRepository schoolClassRepository,
            StudentYearEnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            NotificationAudienceProjectionRepository projectionRepository,
            NotificationAudienceCanonicalizer canonicalizer) {
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.projectionRepository = projectionRepository;
        this.canonicalizer = canonicalizer;
    }

    public boolean hasProjection() {
        return projectionRepository != null;
    }

    public void validateCreateIndividuals(List<Long> userIds) {
        if (userIds.isEmpty()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Danh sách người nhận cho thông báo cá nhân không được để trống");
        }
        List<Long> eligibleIds = projectionRepository.findEligibleUserIds(userIds);
        if (eligibleIds.size() != userIds.size()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Một hoặc nhiều người nhận không tồn tại hoặc không còn đủ điều kiện");
        }
    }

    public void validateCreateClass(Long classId) {
        if (!projectionRepository.existsEligibleClass(classId)) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Lớp không tồn tại, chưa hoạt động hoặc không có người nhận đủ điều kiện");
        }
    }

    public Set<Long> resolveIndividual(Notification notification, List<Long> explicitRecipientUserIds) {
        Set<Long> userIds = explicitRecipientUserIds == null || explicitRecipientUserIds.isEmpty()
                ? canonicalizer.legacyIndividualReference(notification.getTargetReference())
                : new TreeSet<>(explicitRecipientUserIds);
        rejectEmptyIndividuals(userIds);
        List<User> foundUsers = userRepository.findAllById(userIds);
        if (foundUsers.size() != userIds.size()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Một hoặc nhiều người nhận không tồn tại trong hệ thống");
        }
        validateCurrentEligibility(userIds);
        return new LinkedHashSet<>(new TreeSet<>(foundUsers.stream().map(User::getId).toList()));
    }

    public Set<Long> resolveClass(Notification notification) {
        Long classId = canonicalizer.parseClassId(notification.getTargetReference());
        if (!schoolClassRepository.existsById(classId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học với ID: " + classId);
        }
        if (projectionRepository != null) {
            validateCreateClass(classId);
            return new LinkedHashSet<>(projectionRepository.findEligibleUserIdsForClass(classId));
        }
        return resolveLegacyClass(classId);
    }

    public Set<Long> resolveSchoolUsers() {
        return userRepository.findAll().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    private void rejectEmptyIndividuals(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Danh sách người nhận cho thông báo cá nhân không được để trống");
        }
    }

    private void validateCurrentEligibility(Set<Long> userIds) {
        if (projectionRepository != null
                && projectionRepository.findEligibleUserIds(userIds).size() != userIds.size()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Một hoặc nhiều người nhận không còn đủ điều kiện nhận thông báo");
        }
    }

    private Set<Long> resolveLegacyClass(Long classId) {
        List<StudentYearEnrollment> enrollments = enrollmentRepository
                .findByCurrentClassIdAndStatusOrderByStudentIdAsc(classId, EnrollmentStatus.ACTIVE);
        if (enrollments.isEmpty()) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Lớp không có người nhận đủ điều kiện");
        }
        List<Long> studentIds = enrollments.stream().map(StudentYearEnrollment::getStudentId).toList();
        Set<Long> userIds = studentRepository.findAllById(studentIds).stream()
                .map(Student::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        if (userIds.isEmpty()) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Lớp không có người nhận đủ điều kiện");
        }
        return userIds;
    }
}
