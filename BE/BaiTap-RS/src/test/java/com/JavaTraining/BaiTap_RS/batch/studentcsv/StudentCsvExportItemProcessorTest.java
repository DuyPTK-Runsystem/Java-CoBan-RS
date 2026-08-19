package com.JavaTraining.BaiTap_RS.batch.studentcsv;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StudentCsvExportItemProcessorTest {

    private final StudentCsvExportItemProcessor processor = new StudentCsvExportItemProcessor();

    @Test
    void processReturnsValidRow() {
        StudentCsvExportRow row = new StudentCsvExportRow(
                1L,
                "Nguyen Van A",
                "STU1234567",
                "Da Nang",
                8.5,
                LocalDate.of(2012, 4, 22));

        Assertions.assertSame(row, processor.process(row), "valid row should continue to the writer");
    }

    @Test
    void processRejectsRowWithBlankStudentName() {
        StudentCsvExportRow row = new StudentCsvExportRow(
                1L,
                " ",
                "STU1234567",
                null,
                null,
                null);

        Assertions.assertThrows(
                StudentCsvExportItemException.class,
                () -> processor.process(row),
                "blank student name should be an item-level export failure");
    }
}
