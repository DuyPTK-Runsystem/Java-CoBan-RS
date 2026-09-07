package com.JavaTraining.BaiTap_RS.student.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GuardLogStatement"})
public class StudentLookupService {

    private static final String STUDENT_NOT_FOUND_MESSAGE = "Không tìm thấy học sinh";

    private final StudentRepository studentRepository;

    public StudentLookupService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public Student resolveStudent(Long studentId, String studentCode) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                StudentLookupService.class,
                "StudentLookupService.resolveStudent");
        String normalizedCode = normalizeCode(studentCode);
        if (studentId == null && normalizedCode == null) {
            throw badRequest("Phải cung cấp studentId hoặc studentCode");
        }
        if (studentId != null && normalizedCode != null) {
            Student studentById = findById(studentId);
            Student studentByCode = findByCode(normalizedCode);
            if (!studentById.getId().equals(studentByCode.getId())) {
                throw badRequest("studentId và studentCode không khớp với cùng một học sinh");
            }
            return studentById;
        }
        return studentId != null ? findById(studentId) : findByCode(normalizedCode);
    }

    @Transactional(readOnly = true)
    public List<Student> resolveStudents(Collection<Long> studentIds, Collection<String> studentCodes) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                StudentLookupService.class,
                "StudentLookupService.resolveStudents");
        Set<Long> uniqueIds = normalizeIds(studentIds);
        Set<String> uniqueCodes = normalizeCodes(studentCodes);
        validateIdentifiersPresent(uniqueIds, uniqueCodes);

        List<Student> studentsById = findAllByIds(uniqueIds);
        List<Student> studentsByCode = findAllByCodes(uniqueCodes);

        Map<Long, Student> students = new LinkedHashMap<>();
        studentsById.forEach(student -> students.put(student.getId(), student));
        studentsByCode.forEach(student -> students.put(student.getId(), student));
        return List.copyOf(students.values());
    }

    private Student findById(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(this::studentNotFound);
    }

    private Student findByCode(String studentCode) {
        return studentRepository.findByStudentCode(studentCode)
                .orElseThrow(this::studentNotFound);
    }

    private List<Student> findAllByIds(Set<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return List.of();
        }
        List<Student> students = studentRepository.findAllById(studentIds);
        if (students.size() != studentIds.size()) {
            throw studentNotFound();
        }
        return students;
    }

    private List<Student> findAllByCodes(Set<String> studentCodes) {
        if (studentCodes.isEmpty()) {
            return List.of();
        }
        List<Student> students = studentRepository.findAllByStudentCodeIn(studentCodes);
        if (students.size() != studentCodes.size()) {
            throw studentNotFound();
        }
        return students;
    }

    private void validateIdentifiersPresent(Set<Long> studentIds, Set<String> studentCodes) {
        if (studentIds.isEmpty() && studentCodes.isEmpty()) {
            throw badRequest("Phải cung cấp studentId hoặc studentCode");
        }
    }

    private Set<Long> normalizeIds(Collection<Long> studentIds) {
        if (studentIds == null) {
            return Set.of();
        }
        return studentIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizeCodes(Collection<String> studentCodes) {
        if (studentCodes == null) {
            return Set.of();
        }
        return studentCodes.stream()
                .map(this::normalizeCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeCode(String studentCode) {
        if (studentCode == null || studentCode.isBlank()) {
            return null;
        }
        return studentCode.trim();
    }

    private AppException badRequest(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, message);
    }

    private AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, message);
    }

    private AppException studentNotFound() {
        return notFound(STUDENT_NOT_FOUND_MESSAGE);
    }
}
