package com.JavaTraining.BaiTap_RS.student.service;

import java.util.HashSet;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentCodeDTO;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentCodeGenerationService {

    private static final int GENERATE_CODE_BATCH_SIZE = 20;
    private static final int MAX_GENERATE_CODE_BATCH_ATTEMPTS = 5;

    private final StudentRepository studentRepository;
    private final StudentCodeGenerator studentCodeGenerator;

    public StudentCodeGenerationService(StudentRepository studentRepository, StudentCodeGenerator studentCodeGenerator) {
        this.studentRepository = studentRepository;
        this.studentCodeGenerator = studentCodeGenerator;
    }

    @Transactional(readOnly = true)
    public ResStudentCodeDTO generateStudentCode() {
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
        throw new AppException(HttpStatus.CONFLICT, "Unable to generate a unique student code");
    }
}
