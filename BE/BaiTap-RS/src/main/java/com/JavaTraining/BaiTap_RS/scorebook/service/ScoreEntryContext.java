package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
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
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Tra cứu và kiểm tra tính hợp lệ của dữ liệu trước khi nhập/sửa điểm.
 */
@Component
public class ScoreEntryContext {

    private final AssessmentColumnRepository columnRepository;
    private final ScorebookRepository scorebookRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;

    public ScoreEntryContext(
            AssessmentColumnRepository columnRepository,
            ScorebookRepository scorebookRepository,
            ClassSubjectRepository classSubjectRepository,
            SemesterRepository semesterRepository,
            StudentRepository studentRepository,
            StudentYearEnrollmentRepository enrollmentRepository) {
        this.columnRepository = columnRepository;
        this.scorebookRepository = scorebookRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.semesterRepository = semesterRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public AssessmentColumn findActiveColumn(Long columnId) {
        AssessmentColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> notFound("Không tìm thấy cột điểm"));
        if (column.getStatus() != AssessmentColumnStatus.ACTIVE) {
            throw conflict("Cột điểm không còn hoạt động");
        }
        return column;
    }

    public Scorebook findWritableScorebook(Long scorebookId) {
        Scorebook scorebook = scorebookRepository.findById(scorebookId)
                .orElseThrow(() -> notFound("Không tìm thấy sổ điểm"));
        if (scorebook.getStatus() != ScorebookStatus.OPEN
                && scorebook.getStatus() != ScorebookStatus.PUBLISHED) {
            throw conflict("Sổ điểm phải ở trạng thái OPEN hoặc PUBLISHED để nhập điểm");
        }
        return scorebook;
    }

    public ClassSubject findClassSubject(Long classSubjectId) {
        return classSubjectRepository.findById(classSubjectId)
                .orElseThrow(() -> notFound("Không tìm thấy lớp-môn"));
    }

    public Semester findSemesterForScoring(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> notFound("Không tìm thấy học kỳ"));
        if (semester.getStatus() == SemesterStatus.LOCKED
                || semester.getStatus() == SemesterStatus.CLOSED) {
            throw conflict("Không thể nhập điểm cho học kỳ đã khóa hoặc đóng");
        }
        return semester;
    }

    public Student findActiveStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> notFound("Không tìm thấy học sinh"));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw conflict("Học sinh không ở trạng thái ACTIVE");
        }
        return student;
    }

    public void validateEnrollment(Long studentId, Semester semester, Long classId) {
        StudentYearEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(studentId, semester.getAcademicYearId())
                .orElseThrow(() -> conflict("Học sinh chưa được xếp lớp trong năm học này"));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw conflict("Hồ sơ xếp lớp không ở trạng thái ACTIVE");
        }
        if (!enrollment.getCurrentClassId().equals(classId)) {
            throw conflict("Học sinh không thuộc lớp của môn này");
        }
    }

    private AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, message);
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
