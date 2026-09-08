package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentTransferScoreDataLoader {

    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final ScorebookRepository scorebookRepository;
    private final AssessmentColumnRepository columnRepository;
    private final StudentScoreRepository scoreRepository;

    public EnrollmentTransferScoreDataLoader(
            ClassSubjectRepository classSubjectRepository,
            SubjectRepository subjectRepository,
            ScorebookRepository scorebookRepository,
            AssessmentColumnRepository columnRepository,
            StudentScoreRepository scoreRepository) {
        this.classSubjectRepository = classSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.scorebookRepository = scorebookRepository;
        this.columnRepository = columnRepository;
        this.scoreRepository = scoreRepository;
    }

    public TransferScoreData loadScoreData(TransferScoreDataRequest request) {
        Semester semester = request.semester();
        if (semester == null) {
            throw conflict("Học kỳ là bắt buộc cho hỗ trợ chuyển điểm");
        }
        if (semester.getStatus() == SemesterStatus.LOCKED || semester.getStatus() == SemesterStatus.CLOSED) {
            throw conflict("Không thể chuyển điểm khi học kỳ đã khóa hoặc đóng");
        }
        List<ClassSubject> sourceSubjects = activeSubjects(request.sourceClass(), semester);
        List<ClassSubject> targetSubjects = activeSubjects(request.targetClass(), semester);
        Map<Long, Scorebook> sourceBooks = scorebooksBySubject(sourceSubjects);
        Map<Long, Scorebook> targetBooks = scorebooksBySubject(targetSubjects);
        List<AssessmentColumn> sourceColumns = columnsFor(sourceBooks.values());
        List<AssessmentColumn> targetColumns = columnsFor(targetBooks.values());
        Map<Long, StudentScore> sourceScores = findScores(sourceColumns, request.student());
        Map<Long, StudentScore> targetScores = findScores(targetColumns, request.student());
        Map<Long, Subject> subjects = subjectsById(sourceSubjects);
        Map<Long, ClassSubject> sourceBySubject = bySubject(sourceSubjects);
        Map<Long, ClassSubject> targetBySubject = bySubject(targetSubjects);
        Map<Long, Scorebook> sourceScorebooks = scorebooksByColumn(sourceColumns, sourceBooks);
        Map<Long, Scorebook> targetScorebooks = scorebooksByColumn(targetColumns, targetBooks);
        Map<Long, Long> sourceColumnSubjects = columnSubjects(sourceColumns, sourceBySubject, sourceBooks);
        Map<Long, Long> targetColumnSubjects = columnSubjects(targetColumns, targetBySubject, targetBooks);
        return new TransferScoreData(sourceBySubject, targetBySubject, subjects, sourceColumns, targetColumns,
                sourceScores, targetScores, sourceScorebooks, targetScorebooks,
                sourceColumnSubjects, targetColumnSubjects);
    }

    private List<ClassSubject> activeSubjects(SchoolClass schoolClass, Semester semester) {
        return classSubjectRepository
                .findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(schoolClass.getId(), semester.getId()).stream()
                .filter(subject -> subject.getStatus() == ClassSubjectStatus.ACTIVE).toList();
    }

    private Map<Long, Scorebook> scorebooksBySubject(List<ClassSubject> classSubjects) {
        return scorebookRepository.findAllByClassSubjectIdIn(
                        classSubjects.stream().map(ClassSubject::getId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Scorebook::getClassSubjectId, book -> book));
    }

    private List<AssessmentColumn> columnsFor(Collection<Scorebook> scorebooks) {
        if (scorebooks.isEmpty()) {
            return List.of();
        }
        return columnRepository.findAllByScorebookIdInOrderByScorebookIdAscAssessmentTypeAscColumnNoAsc(
                scorebooks.stream().map(Scorebook::getId).toList());
    }

    private Map<Long, StudentScore> findScores(List<AssessmentColumn> columns, Student student) {
        if (columns.isEmpty()) {
            return Map.of();
        }
        return scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(
                        columns.stream().map(AssessmentColumn::getId).toList(), List.of(student.getId())).stream()
                .collect(Collectors.toMap(StudentScore::getAssessmentColumnId, score -> score));
    }

    private Map<Long, Subject> subjectsById(List<ClassSubject> classSubjects) {
        return subjectRepository.findAllById(classSubjects.stream().map(ClassSubject::getSubjectId)
                        .collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Subject::getId, subject -> subject));
    }

    private Map<Long, ClassSubject> bySubject(List<ClassSubject> classSubjects) {
        return classSubjects.stream().collect(Collectors.toMap(ClassSubject::getSubjectId, subject -> subject));
    }

    private Map<Long, Scorebook> scorebooksByColumn(
            List<AssessmentColumn> columns, Map<Long, Scorebook> booksBySubject) {
        Map<Long, Scorebook> result = new HashMap<>();
        booksBySubject.values().forEach(book -> columns.stream()
                .filter(column -> column.getScorebookId().equals(book.getId()))
                .forEach(column -> result.put(column.getId(), book)));
        return result;
    }

    private Map<Long, Long> columnSubjects(
            List<AssessmentColumn> columns,
            Map<Long, ClassSubject> subjects,
            Map<Long, Scorebook> books) {
        Map<Long, Long> result = new HashMap<>();
        subjects.values().forEach(classSubject -> {
            Scorebook scorebook = books.get(classSubject.getId());
            if (scorebook != null) {
                columns.stream().filter(column -> column.getScorebookId().equals(scorebook.getId()))
                        .forEach(column -> result.put(column.getId(), classSubject.getSubjectId()));
            }
        });
        return result;
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}

record TransferScoreContext(
        com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment enrollment,
        com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear year,
        SchoolClass sourceClass,
        SchoolClass targetClass,
        Semester semester,
        Student student) {
}

record TransferScoreData(
        Map<Long, ClassSubject> sourceBySubject,
        Map<Long, ClassSubject> targetBySubject,
        Map<Long, Subject> subjects,
        List<AssessmentColumn> sourceColumns,
        List<AssessmentColumn> targetColumns,
        Map<Long, StudentScore> sourceScores,
        Map<Long, StudentScore> targetScores,
        Map<Long, Scorebook> sourceScorebookByColumnId,
        Map<Long, Scorebook> targetScorebookByColumnId,
        Map<Long, Long> sourceColumnSubjects,
        Map<Long, Long> targetColumnSubjects) {
}

record TransferScoreDataRequest(SchoolClass sourceClass, SchoolClass targetClass, Semester semester, Student student) {
}
