package com.JavaTraining.BaiTap_RS.batch.studentcsv;

import java.time.LocalDate;

public record StudentCsvExportRow(
        Long studentId,
        String studentName,
        String studentCode,
        String address,
        Double averageScore,
        LocalDate dateOfBirth) {
}
