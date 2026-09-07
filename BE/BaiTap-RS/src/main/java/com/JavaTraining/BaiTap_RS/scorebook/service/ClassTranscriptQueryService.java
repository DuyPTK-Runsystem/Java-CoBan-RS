package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectTermResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class ClassTranscriptQueryService {

        private final SchoolClassRepository schoolClassRepository;
        private final SemesterRepository semesterRepository;
        private final StudentYearEnrollmentRepository enrollmentRepository;
        private final StudentRepository studentRepository;
        private final StudentTermTranscriptRepository termTranscriptRepository;
        private final StudentAnnualTranscriptRepository annualTranscriptRepository;
        private final StudentSubjectTermResultRepository termResultRepository;
        private final StudentSubjectAnnualResultRepository annualResultRepository;
        private final TranscriptAccessGuard accessGuard;
        private final TranscriptTermResponseMapper termMapper;
        private final TranscriptResponseSupport responseSupport;

        public ClassTranscriptQueryService(
                        SchoolClassRepository schoolClassRepository,
                        SemesterRepository semesterRepository,
                        StudentYearEnrollmentRepository enrollmentRepository,
                        StudentRepository studentRepository,
                        StudentTermTranscriptRepository termTranscriptRepository,
                        StudentAnnualTranscriptRepository annualTranscriptRepository,
                        StudentSubjectTermResultRepository termResultRepository,
                        StudentSubjectAnnualResultRepository annualResultRepository,
                        TranscriptAccessGuard accessGuard,
                        TranscriptTermResponseMapper termMapper,
                        TranscriptResponseSupport responseSupport) {
                this.schoolClassRepository = schoolClassRepository;
                this.semesterRepository = semesterRepository;
                this.enrollmentRepository = enrollmentRepository;
                this.studentRepository = studentRepository;
                this.termTranscriptRepository = termTranscriptRepository;
                this.annualTranscriptRepository = annualTranscriptRepository;
                this.termResultRepository = termResultRepository;
                this.annualResultRepository = annualResultRepository;
                this.accessGuard = accessGuard;
                this.termMapper = termMapper;
                this.responseSupport = responseSupport;
        }

        @Transactional(readOnly = true)
        public ResClassTermTranscriptDTO getClassTermTranscript(Long classId, Long semesterId) {
                trace("getClassTermTranscript");
                SchoolClass schoolClass = schoolClassRepository.findById(classId)
                                .orElseThrow(() -> notFound("Không tìm thấy lớp học"));
                Semester semester = semesterRepository.findById(semesterId)
                                .orElseThrow(() -> notFound("Không tìm thấy học kỳ"));
                if (!semester.getAcademicYearId().equals(schoolClass.getAcademicYearId())) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "Học kỳ không thuộc năm học của lớp");
                }

                accessGuard.assertCanReadClass(classId, schoolClass.getAcademicYearId(), List.of(semester));

                List<StudentYearEnrollment> enrollments = enrollmentRepository
                                .findByCurrentClassIdAndStatusOrderByStudentIdAsc(classId, EnrollmentStatus.ACTIVE);
                if (enrollments.isEmpty()) {
                        return new ResClassTermTranscriptDTO(
                                        classId, schoolClass.getClassCode(), schoolClass.getClassName(),
                                        schoolClass.getAcademicYearId(), semesterId, List.of());
                }

                List<Long> studentIds = enrollments.stream().map(StudentYearEnrollment::getStudentId).toList();
                Map<Long, Student> studentsMap = studentRepository.findAllById(studentIds).stream()
                                .collect(Collectors.toMap(Student::getId, Function.identity()));

                List<StudentTermTranscript> termTranscripts = termTranscriptRepository
                                .findAllBySemesterIdAndStudentIdIn(semesterId, studentIds);
                Map<Long, StudentTermTranscript> transcriptMap = termTranscripts.stream()
                                .collect(Collectors.toMap(StudentTermTranscript::getStudentId, Function.identity()));

                List<Long> transcriptIds = termTranscripts.stream().map(StudentTermTranscript::getId).toList();
                List<StudentSubjectTermResult> allResults = transcriptIds.isEmpty() ? List.of()
                                : termResultRepository.findAllByTermTranscriptIdInOrderBySubjectIdAsc(transcriptIds);
                Map<Long, List<StudentSubjectTermResult>> resultsByTranscriptId = allResults.stream()
                                .collect(Collectors.groupingBy(StudentSubjectTermResult::getTermTranscriptId));

                List<Long> classSubjectIds = allResults.stream()
                                .map(StudentSubjectTermResult::getClassSubjectId).distinct().toList();
                Map<Long, ClassSubject> classSubjects = responseSupport.findClassSubjects(classSubjectIds);

                List<ResClassTermTranscriptDTO.ClassTermStudentRowDTO> studentRows = new ArrayList<>();
                for (Long studentId : studentIds) {
                        Student student = studentsMap.get(studentId);
                        String studentCode = student != null ? student.getStudentCode() : "";
                        String fullName = student != null ? student.getStudentName() : "";
                        StudentTermTranscript term = transcriptMap.get(studentId);

                        if (term == null) {
                                studentRows.add(new ResClassTermTranscriptDTO.ClassTermStudentRowDTO(
                                                studentId, studentCode, fullName, null, null, List.of()));
                        } else {
                                List<StudentSubjectTermResult> results = resultsByTranscriptId
                                                .getOrDefault(term.getId(), List.of());
                                List<ResStudentTermTranscriptDTO.ResTermSubjectResultDTO> subjects = termMapper
                                                .map(studentId, results, classSubjects);
                                studentRows.add(new ResClassTermTranscriptDTO.ClassTermStudentRowDTO(
                                                studentId, studentCode, fullName, term.getCalculationStatus(),
                                                term.getDtbhk(), subjects));
                        }
                }

                return new ResClassTermTranscriptDTO(
                                classId, schoolClass.getClassCode(), schoolClass.getClassName(),
                                schoolClass.getAcademicYearId(), semesterId, studentRows);
        }

        @Transactional(readOnly = true)
        public ResClassAnnualTranscriptDTO getClassAnnualTranscript(Long classId, Long academicYearId) {
                trace("getClassAnnualTranscript");
                SchoolClass schoolClass = schoolClassRepository.findById(classId)
                                .orElseThrow(() -> notFound("Không tìm thấy lớp học"));
                if (!schoolClass.getAcademicYearId().equals(academicYearId)) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "Năm học không khớp với lớp");
                }

                List<Semester> semesters = semesterRepository
                                .findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYearId);
                accessGuard.assertCanReadClass(classId, academicYearId, semesters);

                List<StudentYearEnrollment> enrollments = enrollmentRepository
                                .findByCurrentClassIdAndStatusOrderByStudentIdAsc(classId, EnrollmentStatus.ACTIVE);
                if (enrollments.isEmpty()) {
                        return new ResClassAnnualTranscriptDTO(
                                        classId, schoolClass.getClassCode(), schoolClass.getClassName(),
                                        academicYearId, List.of());
                }

                List<Long> studentIds = enrollments.stream().map(StudentYearEnrollment::getStudentId).toList();
                Map<Long, Student> studentsMap = studentRepository.findAllById(studentIds).stream()
                                .collect(Collectors.toMap(Student::getId, Function.identity()));

                List<StudentAnnualTranscript> annualTranscripts = annualTranscriptRepository
                                .findAllByAcademicYearIdAndStudentIdIn(academicYearId, studentIds);
                Map<Long, StudentAnnualTranscript> annualMap = annualTranscripts.stream()
                                .collect(Collectors.toMap(StudentAnnualTranscript::getStudentId, Function.identity()));

                List<Long> annualIds = annualTranscripts.stream().map(StudentAnnualTranscript::getId).toList();
                List<StudentSubjectAnnualResult> allAnnualResults = annualIds.isEmpty() ? List.of()
                                : annualResultRepository.findAllByAnnualTranscriptIdInOrderBySubjectIdAsc(annualIds);
                Map<Long, List<StudentSubjectAnnualResult>> resultsByAnnualId = allAnnualResults.stream()
                                .collect(Collectors.groupingBy(StudentSubjectAnnualResult::getAnnualTranscriptId));

                List<Long> termResultIds = allAnnualResults.stream()
                                .flatMap(r -> Stream.of(r.getHk1TermResultId(), r.getHk2TermResultId()))
                                .filter(Objects::nonNull).distinct().toList();
                Map<Long, StudentSubjectTermResult> termResultsMap = termResultIds.isEmpty() ? Map.of()
                                : termResultRepository.findAllById(termResultIds).stream()
                                                .collect(Collectors.toMap(StudentSubjectTermResult::getId,
                                                                Function.identity()));

                List<ResClassAnnualTranscriptDTO.ClassAnnualStudentRowDTO> studentRows = new ArrayList<>();
                for (Long studentId : studentIds) {
                        Student student = studentsMap.get(studentId);
                        String studentCode = student != null ? student.getStudentCode() : "";
                        String fullName = student != null ? student.getStudentName() : "";
                        StudentAnnualTranscript annual = annualMap.get(studentId);

                        if (annual == null) {
                                studentRows.add(new ResClassAnnualTranscriptDTO.ClassAnnualStudentRowDTO(
                                                studentId, studentCode, fullName, null, null, null, null, List.of()));
                        } else {
                                List<StudentSubjectAnnualResult> annualResults = resultsByAnnualId
                                                .getOrDefault(annual.getId(), List.of());
                                List<ResStudentAnnualTranscriptDTO.ResAnnualSubjectResultDTO> subjects = responseSupport
                                                .mapAnnualResults(annualResults, termResultsMap);
                                studentRows.add(new ResClassAnnualTranscriptDTO.ClassAnnualStudentRowDTO(
                                                studentId, studentCode, fullName, annual.getCalculationStatus(),
                                                annual.getRegularDtbcn(), annual.getFinalDtbcn(),
                                                annual.getResultSource(), subjects));
                        }
                }

                return new ResClassAnnualTranscriptDTO(
                                classId, schoolClass.getClassCode(), schoolClass.getClassName(),
                                academicYearId, studentRows);
        }

        private AppException notFound(String message) {
                return new AppException(HttpStatus.NOT_FOUND, message);
        }

        private void trace(String operation) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */ ClassTranscriptQueryService.class,
                                "ClassTranscriptQueryService." + operation);
        }
}
