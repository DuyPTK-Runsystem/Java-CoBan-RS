package com.JavaTraining.BaiTap_RS.student.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqCreateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqFetchStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpdateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentCodeDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentPageDTO;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int GENERATE_CODE_BATCH_SIZE = 20;
    private static final int MAX_GENERATE_CODE_BATCH_ATTEMPTS = 5;

    private final StudentRepository studentRepository;
    private final StudentCodeGenerator studentCodeGenerator;

    public StudentService(StudentRepository studentRepository, StudentCodeGenerator studentCodeGenerator) {
        this.studentRepository = studentRepository;
        this.studentCodeGenerator = studentCodeGenerator;
    }

    @Transactional(readOnly = true)
    public ResStudentPageDTO fetchStudents(ReqFetchStudentDTO request) {
        Pageable pageable = PageRequest.of(
                resolvePage(request.getPage()),
                resolveSize(request.getSize()),
                StudentSortResolver.resolve(request.getSortField(), request.getSortDirection()));
        Page<Student> page = studentRepository.findAll(StudentSpecifications.from(request), pageable);
        List<ResStudentDTO> content = page.getContent().stream()
                .map(this::toStudentDTO)
                .toList();
        return new ResStudentPageDTO(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ResStudentDTO getStudent(Long studentId) {
        return toStudentDTO(findStudent(studentId));
    }

    @Transactional
    public ResStudentDTO createStudent(ReqCreateStudentDTO request) {
        if (studentRepository.existsByStudentCode(request.getStudentCode())) {
            throw new AppException(HttpStatus.CONFLICT, "Mã sinh viên đã tồn tại");
        }
        Student student = new Student(request.getStudentName(), request.getStudentCode());
        StudentInfo info = new StudentInfo(request.getDateOfBirth(), request.getAddress(), request.getAverageScore());
        student.assignInfo(info);
        return toStudentDTO(studentRepository.save(student));
    }

    @Transactional
    public ResStudentDTO updateStudent(Long studentId, ReqUpdateStudentDTO request) {
        Student student = findStudent(studentId);
        student.setStudentName(request.getStudentName());
        StudentInfo info = student.getStudentInfo();
        if (info == null) {
            info = new StudentInfo(request.getDateOfBirth(), request.getAddress(), request.getAverageScore());
            student.assignInfo(info);
        } else {
            info.setDateOfBirth(request.getDateOfBirth());
            info.setAddress(request.getAddress());
            info.setAverageScore(request.getAverageScore());
        }
        return toStudentDTO(studentRepository.save(student));
    }

    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = findStudent(studentId);
        studentRepository.delete(student);
    }

    /**
     * Hàm sinh mã HS ngẫu nhiên
     * 
     * <p>
     * Dựa trên thuật toán sinh mã HS ngẫu nhiên và tính toán riêng của dev,
     * khi và chỉ khi tỉ lệ lấp đầy database đạt <b>95.4993%</b> thì tỉ lệ trả về
     * lỗi không
     * thể tạo mã HS duy nhất là <b>1%</b>
     * </p>
     * 
     * <p>
     * Số lượng HS tối đa được lưu trong db là <b>10.000.000</b> HS
     * </p>
     * 
     * @return Object chứa mã HS ngẫu nhiên gồm 10 kí tự ("STU" + 7 kí tự số) được
     *         sinh ra theo định dạng
     *         "STUxxxxxxx". Mỗi 'x' là một chữ số từ 0-9
     * @throws AppException nếu không thể tạo mã HS duy nhất
     */
    @Transactional(readOnly = true)
    public ResStudentCodeDTO generateStudentCode() {
        Set<String> existingCodes = new HashSet<>();
        for (int attempt = 0; attempt < MAX_GENERATE_CODE_BATCH_ATTEMPTS; attempt++) {
            Set<String> candidates = studentCodeGenerator.generateCandidates(GENERATE_CODE_BATCH_SIZE);
            existingCodes.clear();
            existingCodes.addAll(studentRepository.findExistingStudentCodes(candidates));
            ResStudentCodeDTO availableCode = candidates.stream()
                    .filter(candidate -> !existingCodes.contains(candidate))
                    .findFirst()
                    .map(ResStudentCodeDTO::new)
                    .orElse(null);
            if (availableCode != null) {
                return availableCode;
            }
        }
        throw new AppException(HttpStatus.CONFLICT, "Không thể tạo mã sinh viên duy nhất");
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sinh viên"));
    }

    private int resolvePage(int page) {
        if (page < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Trang không được nhỏ hơn 0");
        }
        return page;
    }

    private int resolveSize(int size) {
        if (size < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Kích thước trang không được nhỏ hơn 0");
        }
        if (size == 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return size;
    }

    private ResStudentDTO toStudentDTO(Student student) {
        StudentInfo info = student.getStudentInfo();
        return new ResStudentDTO(
                student.getId(),
                student.getStudentCode(),
                student.getStudentName(),
                info == null ? null : info.getDateOfBirth(),
                info == null ? null : info.getAddress(),
                info == null ? null : info.getAverageScore());
    }
}
