package com.JavaTraining.BaiTap_RS.batch.studentcsv;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

public class StudentCsvExportWriter implements ItemWriter<StudentCsvExportRow>, StepExecutionListener {

    private static final String HEADER = "student_id,student_name,student_code,address,average_score,date_of_birth\r\n";
    private final StudentCsvExportResultStore resultStore;
    private ByteArrayOutputStream output;
    private long executionId;

    public StudentCsvExportWriter(StudentCsvExportResultStore resultStore) {
        this.resultStore = resultStore;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        executionId = stepExecution.getJobExecution().getId();
        output = new ByteArrayOutputStream();
        output.writeBytes(HEADER.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void write(Chunk<? extends StudentCsvExportRow> items) {
        for (StudentCsvExportRow item : items) {
            output.writeBytes(toLine(item).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        resultStore.store(executionId, output.toByteArray());
        return null;
    }

    private String toLine(StudentCsvExportRow item) {
        return String.join(
                ",",
                escape(item.studentId()),
                escape(item.studentName()),
                escape(item.studentCode()),
                escape(item.address()),
                escape(item.averageScore()),
                escape(item.dateOfBirth())) + "\r\n";
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (!requiresQuotes(text)) {
            return text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private boolean requiresQuotes(String value) {
        return value.contains(",") || value.contains("\"") || value.contains("\r") || value.contains("\n");
    }
}
