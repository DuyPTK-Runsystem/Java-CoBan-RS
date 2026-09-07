package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ScoreChangeRequestContext {

    private final AssessmentColumnRepository columnRepository;
    private final ScorebookRepository scorebookRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final StudentScoreRepository scoreRepository;

    public ScoreChangeRequestContext(
            AssessmentColumnRepository columnRepository,
            ScorebookRepository scorebookRepository,
            ClassSubjectRepository classSubjectRepository,
            SemesterRepository semesterRepository,
            StudentRepository studentRepository,
            StudentYearEnrollmentRepository enrollmentRepository,
            StudentScoreRepository scoreRepository) {
        this.columnRepository = columnRepository;
        this.scorebookRepository = scorebookRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.semesterRepository = semesterRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.scoreRepository = scoreRepository;
    }

    public AssessmentColumn findColumn(Long columnId) {
        return columnRepository.findById(columnId)
                .orElseThrow(() -> notFound("Không tìm thấy cột điểm"));
    }

    public AssessmentColumn findActiveColumn(Long columnId) {
        AssessmentColumn column = findColumn(columnId);
        if (column.getStatus() != AssessmentColumnStatus.ACTIVE) {
            throw conflict("Cột điểm không còn hoạt động");
        }
        return column;
    }

    public Scorebook findScorebook(Long scorebookId) {
        return scorebookRepository.findById(scorebookId)
                .orElseThrow(() -> notFound("Không tìm thấy sổ điểm"));
    }

    public void validateRequestableScorebook(Scorebook scorebook) {
        if (scorebook.getStatus() != ScorebookStatus.OPEN
                && scorebook.getStatus() != ScorebookStatus.PUBLISHED) {
            throw conflict("Sổ điểm phải ở trạng thái OPEN hoặc PUBLISHED để tạo yêu cầu sửa điểm");
        }
    }

    public ClassSubject findActiveClassSubject(Long classSubjectId) {
        ClassSubject classSubject = classSubjectRepository.findById(classSubjectId)
                .orElseThrow(() -> notFound("Không tìm thấy lớp-môn"));
        if (classSubject.getStatus() != ClassSubjectStatus.ACTIVE) {
            throw conflict("Lớp-môn không còn hoạt động");
        }
        return classSubject;
    }

    public Semester findSemester(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> notFound("Không tìm thấy học kỳ"));
        if (semester.getStatus() == SemesterStatus.DRAFT) {
            throw conflict("Học kỳ chưa được kích hoạt");
        }
        return semester;
    }

    public void validateStudentAndEnrollment(Long studentId, Semester semester, Long classId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> notFound("Không tìm thấy học sinh"));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw conflict("Học sinh không ở trạng thái ACTIVE");
        }
        StudentYearEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(studentId, semester.getAcademicYearId())
                .orElseThrow(() -> conflict("Học sinh chưa được xếp lớp trong năm học này"));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE
                || !enrollment.getCurrentClassId().equals(classId)) {
            throw conflict("Học sinh không thuộc lớp của môn này");
        }
    }

    public java.util.Optional<StudentScore> findScore(Long columnId, Long studentId) {
        return scoreRepository.findByAssessmentColumnIdAndStudentId(columnId, studentId);
    }

    private AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, message);
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
