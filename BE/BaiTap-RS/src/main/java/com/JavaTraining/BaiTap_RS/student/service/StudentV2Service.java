package com.JavaTraining.BaiTap_RS.student.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqFetchStudentV2DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpdateStudentStatusDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpsertStudentV2DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentV2DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentV2PageDTO;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
import com.JavaTraining.BaiTap_RS.student.repository.StudentDeletionGuardRepository;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentV2Service {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final StudentRepository studentRepository;
    private final StudentDeletionGuardRepository deletionGuardRepository;
    private final StudentV2AccessService accessService;
    private final StudentV2ResponseMapper responseMapper;

    public StudentV2Service(
            StudentRepository studentRepository,
            StudentDeletionGuardRepository deletionGuardRepository,
            StudentV2AccessService accessService,
            StudentV2ResponseMapper responseMapper) {
        this.studentRepository = studentRepository;
        this.deletionGuardRepository = deletionGuardRepository;
        this.accessService = accessService;
        this.responseMapper = responseMapper;
    }

    @Transactional(readOnly = true)
    public ResStudentV2PageDTO fetchStudents(ReqFetchStudentV2DTO request) {
        validatePaging(request.getPage(), request.getSize());
        StudentV2AccessService.AccessScope scope = accessService.accessScope();
        Set<Long> accessibleClassIds = scope.office() ? null : scope.classIds();
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize() == 0 ? DEFAULT_PAGE_SIZE : request.getSize(),
                StudentV2SortResolver.resolve(request.getSortField(), request.getSortDirection()));
        Page<Student> page = studentRepository.findAll(
                StudentV2Specifications.from(request, accessibleClassIds), pageable);
        Map<Long, StudentV2ResponseMapper.CurrentClass> classes = responseMapper.currentClasses(page.getContent());
        Map<Long, com.JavaTraining.BaiTap_RS.user.domain.entity.User> users = responseMapper.users(page.getContent());
        List<ResStudentV2DTO> content = page.getContent().stream()
                .map(student -> responseMapper.response(
                        student,
                        classes.get(student.getId()),
                        student.getUserId() == null ? null : users.get(student.getUserId())))
                .toList();
        return new ResStudentV2PageDTO(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ResStudentV2DTO getStudent(Long studentId) {
        return getStudent(studentRepository.findById(studentId)
                .orElseThrow(this::notFound));
    }

    @Transactional(readOnly = true)
    public ResStudentV2DTO getStudentByCode(String studentCode) {
        return getStudent(studentRepository.findByStudentCode(studentCode)
                .orElseThrow(this::notFound));
    }

    @Transactional
    public ResStudentV2DTO createStudent(ReqUpsertStudentV2DTO request) {
        if (studentRepository.existsByStudentCode(request.studentCode())) {
            throw new AppException(HttpStatus.CONFLICT, "Mã học sinh đã tồn tại");
        }
        Student student = new Student(request.studentName(), request.studentCode());
        student.assignInfo(new com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo(
                request.dateOfBirth(), request.address(), null, request.gender()));
        Student saved = studentRepository.save(student);
        return responseMapper.response(saved, null, null);
    }

    @Transactional
    public ResStudentV2DTO updateStudent(Long studentId, ReqUpsertStudentV2DTO request) {
        Student student = studentRepository.findById(studentId).orElseThrow(this::notFound);
        if (!student.getStudentCode().equals(request.studentCode())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không hỗ trợ thay đổi mã học sinh");
        }
        student.setStudentName(request.studentName());
        com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo info = student.getStudentInfo();
        if (info == null) {
            student.assignInfo(new com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo(
                    request.dateOfBirth(), request.address(), null, request.gender()));
        } else {
            info.setDateOfBirth(request.dateOfBirth());
            info.setAddress(request.address());
            info.setGender(request.gender());
        }
        return responseMapper.response(
                student, responseMapper.currentClass(student.getId()), responseMapper.user(student.getUserId()));
    }

    @Transactional
    public ResStudentV2DTO transitionStatus(Long studentId, ReqUpdateStudentStatusDTO request) {
        if (request.status() == StudentStatus.ACTIVE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ chuyển sang INACTIVE hoặc GRADUATED");
        }
        Student student = studentRepository.findById(studentId).orElseThrow(this::notFound);
        student.setStatus(request.status());
        return responseMapper.response(
                student, responseMapper.currentClass(student.getId()), responseMapper.user(student.getUserId()));
    }

    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = studentRepository.findById(studentId).orElseThrow(this::notFound);
        if (deletionGuardRepository.existsByStudentId(studentId)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Không thể xóa học sinh đã có dữ liệu học vụ; hãy chuyển trạng thái");
        }
        studentRepository.delete(student);
    }

    private ResStudentV2DTO getStudent(Student student) {
        StudentV2ResponseMapper.CurrentClass currentClass = responseMapper.currentClass(student.getId());
        accessService.assertCanReadStudent(currentClass == null ? null : currentClass.id());
        return responseMapper.response(student, currentClass, responseMapper.user(student.getUserId()));
    }

    private void validatePaging(int page, int size) {
        if (page < 0 || size < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Phân trang không hợp lệ");
        }
    }

    private AppException notFound() {
        return new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh");
    }

}
