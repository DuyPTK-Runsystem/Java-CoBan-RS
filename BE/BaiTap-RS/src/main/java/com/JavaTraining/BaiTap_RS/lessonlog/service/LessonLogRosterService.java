package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogRosterService {
    private final StudentYearEnrollmentRepository enrollmentRepository;

    public int count(Long classId, LocalDateTime at) {
        return Math.toIntExact(enrollmentRepository.countRosterAt(classId, at));
    }
}
