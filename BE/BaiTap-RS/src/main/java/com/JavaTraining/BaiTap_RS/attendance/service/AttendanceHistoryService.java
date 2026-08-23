package com.JavaTraining.BaiTap_RS.attendance.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqAttendanceHistoryQuery;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResStudentAttendanceHistoryDTO;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceHistoryService {

    private final StudentRepository studentRepository;
    private final AttendanceHistoryItemCollector itemCollector;
    private final AttendanceHistoryResponseMapper responseMapper;

    public AttendanceHistoryService(
            StudentRepository studentRepository,
            AttendanceHistoryItemCollector itemCollector,
            AttendanceHistoryResponseMapper responseMapper) {
        this.studentRepository = studentRepository;
        this.itemCollector = itemCollector;
        this.responseMapper = responseMapper;
    }

    @Transactional(readOnly = true)
    public ResStudentAttendanceHistoryDTO getHistory(ReqAttendanceHistoryQuery query) {
        validateRange(query);
        Student student = findCurrentStudent();
        List<ResStudentAttendanceHistoryDTO.Item> items = itemCollector.collectItems(student.getId(), query);
        if (items.isEmpty()) {
            return responseMapper.emptyResponse(query);
        }

        List<ResStudentAttendanceHistoryDTO.Item> sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparing(ResStudentAttendanceHistoryDTO.Item::attendanceDate)
                .thenComparing(item -> item.sessionPeriod().name()));
        ResStudentAttendanceHistoryDTO.Summary summary = responseMapper.summarize(sortedItems);
        return responseMapper.page(sortedItems, summary, query.resolvedPage(), query.resolvedSize());
    }

    private Student findCurrentStudent() {
        Long userId = AuditContext.currentUserId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ học sinh"));
    }

    private void validateRange(ReqAttendanceHistoryQuery query) {
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Khoảng ngày không hợp lệ");
        }
    }
}
