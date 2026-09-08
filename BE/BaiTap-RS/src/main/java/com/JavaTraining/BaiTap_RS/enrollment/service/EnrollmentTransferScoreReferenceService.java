package com.JavaTraining.BaiTap_RS.enrollment.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentTransferScoreReferenceService {

    private final SemesterRepository semesterRepository;
    private final AssessmentColumnRepository columnRepository;
    private final ScorebookRepository scorebookRepository;
    private final ClassSubjectRepository classSubjectRepository;

    public EnrollmentTransferScoreReferenceService(
            SemesterRepository semesterRepository,
            AssessmentColumnRepository columnRepository,
            ScorebookRepository scorebookRepository,
            ClassSubjectRepository classSubjectRepository) {
        this.semesterRepository = semesterRepository;
        this.columnRepository = columnRepository;
        this.scorebookRepository = scorebookRepository;
        this.classSubjectRepository = classSubjectRepository;
    }

    public Semester findSemester(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));
    }

    public Long scoreDataColumnSemester(Long columnId) {
        AssessmentColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy cột điểm"));
        Scorebook scorebook = scorebookRepository.findById(column.getScorebookId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sổ điểm"));
        ClassSubject classSubject = classSubjectRepository.findById(scorebook.getClassSubjectId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp-môn"));
        return classSubject.getSemesterId();
    }
}
