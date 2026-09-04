package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ScorebookContext {

    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final ScorebookRepository scorebookRepository;

    public ScorebookContext(
            ClassSubjectRepository classSubjectRepository,
            SubjectRepository subjectRepository,
            SemesterRepository semesterRepository,
            ScorebookRepository scorebookRepository) {
        this.classSubjectRepository = classSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.semesterRepository = semesterRepository;
        this.scorebookRepository = scorebookRepository;
    }

    public ClassSubject findClassSubject(Long classSubjectId) {
        return classSubjectRepository.findById(classSubjectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp-môn"));
    }

    public Subject findActiveSubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học"));
        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw conflict("Chỉ môn ACTIVE mới được cấu hình sổ điểm");
        }
        return subject;
    }

    public Subject subjectFor(Scorebook scorebook) {
        ClassSubject classSubject = findClassSubject(scorebook.getClassSubjectId());
        return findActiveSubject(classSubject.getSubjectId());
    }

    public Scorebook findScorebook(Long scorebookId) {
        return scorebookRepository.findById(scorebookId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sổ điểm"));
    }

    public Scorebook findScorebookByClassSubject(Long classSubjectId) {
        return scorebookRepository.findByClassSubjectId(classSubjectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Lớp-môn chưa có sổ điểm"));
    }

    public void validateClassSubject(ClassSubject classSubject) {
        if (classSubject.getStatus() != ClassSubjectStatus.ACTIVE) {
            throw conflict("Chỉ lớp-môn ACTIVE mới được tạo sổ điểm");
        }
    }

    public void validateSemesterForConfiguration(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));
        if (semester.getStatus() == SemesterStatus.LOCKED || semester.getStatus() == SemesterStatus.CLOSED) {
            throw conflict("Không cấu hình sổ điểm cho học kỳ đã khóa hoặc đóng");
        }
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
