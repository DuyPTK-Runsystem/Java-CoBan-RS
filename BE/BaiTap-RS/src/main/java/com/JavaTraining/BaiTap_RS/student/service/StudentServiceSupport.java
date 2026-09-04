package com.JavaTraining.BaiTap_RS.student.service;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqCreateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpdateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;

final class StudentServiceSupport {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private final StudentRepository repository;

    public StudentServiceSupport(StudentRepository repository) {
        this.repository = repository;
    }

    public int page(int value) {
        if (value < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Trang không được nhỏ hơn 0");
        }
        return value;
    }

    public int size(int value) {
        if (value < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Kích thước trang không được nhỏ hơn 0");
        }
        return value == 0 ? DEFAULT_PAGE_SIZE : value;
    }

    public Student find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sinh viên"));
    }

    public Student findByCode(String code) {
        return repository.findByStudentCode(code)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sinh viên"));
    }

    public ResStudentDTO response(Student student) {
        StudentInfo info = student.getStudentInfo();
        return new ResStudentDTO(student.getId(), student.getStudentCode(), student.getStudentName(),
                info == null ? null : info.getDateOfBirth(), info == null ? null : info.getAddress(),
                info == null ? null : info.getAverageScore());
    }

    public ResStudentDTO create(ReqCreateStudentDTO request) {
        Student student = new Student(request.getStudentName(), request.getStudentCode());
        student.assignInfo(new StudentInfo(request.getDateOfBirth(), request.getAddress(), request.getAverageScore()));
        return response(repository.save(student));
    }

    public ResStudentDTO update(Long id, ReqUpdateStudentDTO request) {
        Student student = find(id);
        student.setStudentName(request.getStudentName());
        StudentInfo info = student.getStudentInfo();
        if (info == null) {
            student.assignInfo(new StudentInfo(request.getDateOfBirth(), request.getAddress(), request.getAverageScore()));
        } else {
            info.setDateOfBirth(request.getDateOfBirth());
            info.setAddress(request.getAddress());
            info.setAverageScore(request.getAverageScore());
        }
        return response(repository.save(student));
    }

    public AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
