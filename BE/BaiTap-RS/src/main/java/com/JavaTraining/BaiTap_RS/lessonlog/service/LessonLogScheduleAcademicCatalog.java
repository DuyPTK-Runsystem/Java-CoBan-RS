package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

/** Reads academic names and class-subject relationships for schedule responses. */
@Service
@RequiredArgsConstructor
public class LessonLogScheduleAcademicCatalog {
    private final ClassSubjectRepository classSubjects;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final TeacherRepository teachers;

    public Optional<ClassSubject> classSubject(Long id) {
        return classSubjects.findById(id);
    }

    public Optional<SchoolClass> schoolClass(Long id) {
        return classes.findById(id);
    }

    public Optional<Subject> subject(Long id) {
        return subjects.findById(id);
    }

    public Optional<Teacher> teacher(Long id) {
        return teachers.findById(id);
    }
}
