package com.JavaTraining.BaiTap_RS.batch.studentcsv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class StudentCsvExportItemProcessor implements ItemProcessor<StudentCsvExportRow, StudentCsvExportRow> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentCsvExportItemProcessor.class);
    private static final String REQUEST_ID_KEY = "requestId";

    @Override
    public StudentCsvExportRow process(StudentCsvExportRow item) {
        if (item == null) {
            throw reject(null, "Dòng export không tồn tại");
        }
        validateStudentId(item);
        validateStudentName(item);
        validateStudentCode(item);
        return item;
    }

    private void validateStudentId(StudentCsvExportRow item) {
        if (item.studentId() == null || item.studentId() <= 0) {
            throw reject(item, "Student ID không hợp lệ");
        }
    }

    private void validateStudentName(StudentCsvExportRow item) {
        if (isBlank(item.studentName())) {
            throw reject(item, "Student name trống");
        }
    }

    private void validateStudentCode(StudentCsvExportRow item) {
        if (isBlank(item.studentCode())) {
            throw reject(item, "Student code trống");
        }
    }

    private StudentCsvExportItemException reject(StudentCsvExportRow item, String reason) {
        String studentId = item == null ? "N/A" : String.valueOf(item.studentId());
        String studentCode = item == null ? "N/A" : item.studentCode();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    ">>>BatchCsvExport (skipping student id={}, code={}, reason={}): item rejected [{}] [{}]",
                    studentId,
                    studentCode,
                    reason,
                    Thread.currentThread().getName(),
                    resolveRequestId());
        }
        return new StudentCsvExportItemException(reason);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveRequestId() {
        String requestId = MDC.get(REQUEST_ID_KEY);
        return requestId == null ? "N/A" : requestId;
    }
}
