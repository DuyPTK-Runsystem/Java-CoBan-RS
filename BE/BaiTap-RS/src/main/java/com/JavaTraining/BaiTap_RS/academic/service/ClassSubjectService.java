package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateClassSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateClassSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResClassSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectApplicabilityRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class ClassSubjectService {

    private final ClassSubjectRepository classSubjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectApplicabilityRepository applicabilityRepository;

    public ClassSubjectService(
            ClassSubjectRepository classSubjectRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            SemesterRepository semesterRepository,
            SubjectApplicabilityRepository applicabilityRepository) {
        this.classSubjectRepository = classSubjectRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.semesterRepository = semesterRepository;
        this.applicabilityRepository = applicabilityRepository;
    }

    @Transactional(readOnly = true)
    public List<ResClassSubjectDTO> listByClassAndSemester(Long classId, Long semesterId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ClassSubjectService.class,
                "ClassSubjectService.listByClassAndSemester");
        return classSubjectRepository.findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(classId, semesterId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ResClassSubjectDTO createClassSubject(ReqCreateClassSubjectDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ClassSubjectService.class,
                "ClassSubjectService.createClassSubject");
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp"));
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học"));
        Semester semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));
        if (!schoolClass.getAcademicYearId().equals(semester.getAcademicYearId())) {
            throw conflict("Lớp không thuộc năm học của học kỳ");
        }
        if (semester.getStatus() == SemesterStatus.CLOSED) {
            throw conflict("Không cấu hình lớp-môn cho học kỳ đã CLOSED");
        }
        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw conflict("Chỉ môn ACTIVE mới được cấu hình cho lớp");
        }
        if (classSubjectRepository.existsByClassIdAndSubjectIdAndSemesterId(
                request.classId(), request.subjectId(), request.semesterId())) {
            throw conflict("Lớp đã có môn này trong học kỳ");
        }
        ensureSubjectApplicable(schoolClass.getGradeLevelId(), subject, request);
        ClassSubject classSubject = new ClassSubject(
                request.classId(),
                request.subjectId(),
                request.semesterId(),
                request.status());
        return toResponse(classSubjectRepository.save(classSubject));
    }

    @Transactional
    public ResClassSubjectDTO updateClassSubject(Long id, ReqUpdateClassSubjectDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ClassSubjectService.class,
                "ClassSubjectService.updateClassSubject");
        ClassSubject classSubject = classSubjectRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp-môn"));
        classSubject.setStatus(request.status());
        return toResponse(classSubject);
    }

    private void ensureSubjectApplicable(
            Long gradeLevelId,
            Subject subject,
            ReqCreateClassSubjectDTO request) {
        boolean applicable;
        if (subject.getApplicationScope() == ApplicationScope.GRADE) {
            applicable = applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelIdAndStatus(
                    request.subjectId(),
                    request.semesterId(),
                    ApplicationScope.GRADE,
                    gradeLevelId,
                    SubjectApplicabilityStatus.ACTIVE);
        } else {
            applicable = applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndClassIdAndStatus(
                    request.subjectId(),
                    request.semesterId(),
                    ApplicationScope.CLASS,
                    request.classId(),
                    SubjectApplicabilityStatus.ACTIVE);
        }
        if (!applicable) {
            throw conflict("Môn học chưa được cấu hình áp dụng cho lớp/học kỳ");
        }
    }

    private ResClassSubjectDTO toResponse(ClassSubject classSubject) {
        return new ResClassSubjectDTO(
                classSubject.getId(),
                classSubject.getClassId(),
                classSubject.getSubjectId(),
                classSubject.getSemesterId(),
                classSubject.getStatus());
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
