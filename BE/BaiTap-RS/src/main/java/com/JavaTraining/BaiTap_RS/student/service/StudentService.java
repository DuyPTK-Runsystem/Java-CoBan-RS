package com.JavaTraining.BaiTap_RS.student.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqCreateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqFetchStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpdateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentCodeDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentPageDTO;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class StudentService {
    private static final int GENERATE_CODE_BATCH_SIZE = 20;
    private static final int MAX_GENERATE_CODE_BATCH_ATTEMPTS = 5;
    private final StudentRepository studentRepository;
    private final StudentCodeGenerator studentCodeGenerator;
    private final StudentServiceSupport support;

    public StudentService(StudentRepository studentRepository, StudentCodeGenerator studentCodeGenerator) {
        this.studentRepository = studentRepository;
        this.studentCodeGenerator = studentCodeGenerator;
        this.support = new StudentServiceSupport(studentRepository);
    }

    @Transactional(readOnly = true)
    public ResStudentPageDTO fetchStudents(ReqFetchStudentDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ StudentService.class, "StudentService.fetchStudents");
        Pageable pageable = PageRequest.of(support.page(request.getPage()), support.size(request.getSize()),
                StudentSortResolver.resolve(request.getSortField(), request.getSortDirection()));
        Page<Student> page = studentRepository.findAll(StudentSpecifications.from(request), pageable);
        List<ResStudentDTO> content = page.getContent().stream().map(support::response).toList();
        return new ResStudentPageDTO(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ResStudentDTO getStudent(Long studentId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ StudentService.class, "StudentService.getStudent");
        return support.response(support.find(studentId));
    }

    @Transactional(readOnly = true)
    public ResStudentDTO getStudentByCode(String studentCode) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ StudentService.class, "StudentService.getStudentByCode");
        return support.response(support.findByCode(studentCode));
    }

    @Transactional
    public ResStudentDTO createStudent(ReqCreateStudentDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ StudentService.class, "StudentService.createStudent");
        if (studentRepository.existsByStudentCode(request.getStudentCode())) {
            throw support.conflict("Mã sinh viên đã tồn tại");
        }
        return support.create(request);
    }

    @Transactional
    public ResStudentDTO updateStudent(Long studentId, ReqUpdateStudentDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ StudentService.class, "StudentService.updateStudent");
        return support.update(studentId, request);
    }

    @Transactional
    public void deleteStudent(Long studentId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ StudentService.class, "StudentService.deleteStudent");
        studentRepository.delete(support.find(studentId));
    }

    @Transactional(readOnly = true)
    public ResStudentCodeDTO generateStudentCode() {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ StudentService.class, "StudentService.generateStudentCode");
        Set<String> existingCodes = new HashSet<>();
        for (int attempt = 0; attempt < MAX_GENERATE_CODE_BATCH_ATTEMPTS; attempt++) {
            Set<String> candidates = studentCodeGenerator.generateCandidates(GENERATE_CODE_BATCH_SIZE);
            existingCodes.clear();
            existingCodes.addAll(studentRepository.findExistingStudentCodes(candidates));
            ResStudentCodeDTO available = candidates.stream().filter(candidate -> !existingCodes.contains(candidate))
                    .findFirst().map(ResStudentCodeDTO::new).orElse(null);
            if (available != null) {
                return available;
            }
        }
        throw new AppException(HttpStatus.CONFLICT, "Không thể tạo mã sinh viên duy nhất");
    }
}
