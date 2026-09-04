package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TranscriptCurrentStudentResolver {

    private final StudentRepository studentRepository;

    public Long currentStudentId() {
        return studentRepository.findByUserId(AuditContext.currentUserId())
                .map(com.JavaTraining.BaiTap_RS.student.domain.entity.Student::getId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy hồ sơ học sinh của tài khoản hiện tại"));
    }
}
