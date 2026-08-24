package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
import org.springframework.stereotype.Service;

/**
 * NFR-CALC-005/007: Quản lý trạng thái transcript khi điểm thay đổi.
 * Tạo mới hoặc tăng source_version và đặt status = IN_PROGRESS.
 */
@Service
public class TranscriptStateService {

    private final StudentAnnualTranscriptRepository annualRepository;
    private final StudentTermTranscriptRepository termRepository;

    public TranscriptStateService(
            StudentAnnualTranscriptRepository annualRepository,
            StudentTermTranscriptRepository termRepository) {
        this.annualRepository = annualRepository;
        this.termRepository = termRepository;
    }

    /**
     * Tìm hoặc tạo annual transcript, tăng source_version và đặt IN_PROGRESS.
     * Trả về source_version mới nhất để dùng cho calculation task.
     */
    public long touchAnnualTranscript(Long studentId, Long academicYearId) {
        StudentAnnualTranscript annual = annualRepository
                .findForUpdate(studentId, academicYearId)
                .orElseGet(() -> annualRepository.save(
                        new StudentAnnualTranscript(studentId, academicYearId)));

        annual.incrementSourceVersion();
        annual.setCalculationStatus(CalculationStatus.IN_PROGRESS);
        annualRepository.save(annual);
        return annual.getSourceVersion();
    }

    /**
     * Tìm hoặc tạo term transcript, tăng source_version và đặt IN_PROGRESS.
     */
    public void touchTermTranscript(Long annualTranscriptId, Long semesterId, Long studentId) {
        StudentAnnualTranscript annual = annualRepository.findById(annualTranscriptId)
                .orElseThrow();

        StudentTermTranscript term = termRepository
                .findByAnnualTranscriptIdAndSemesterId(annual.getId(), semesterId)
                .orElseGet(() -> termRepository.save(
                        new StudentTermTranscript(annual.getId(), semesterId, studentId)));

        term.incrementSourceVersion();
        term.setCalculationStatus(CalculationStatus.IN_PROGRESS);
        termRepository.save(term);
    }

    /**
     * Tìm hoặc tạo cả annual + term transcript, trả về source_version mới nhất của
     * annual.
     */
    public long touchTranscripts(Long studentId, Long academicYearId, Long semesterId) {
        long newSourceVersion = touchAnnualTranscript(studentId, academicYearId);

        StudentAnnualTranscript annual = annualRepository
                .findByStudentIdAndAcademicYearId(studentId, academicYearId)
                .orElseThrow();
        touchTermTranscript(annual.getId(), semesterId, studentId);

        return newSourceVersion;
    }
}
