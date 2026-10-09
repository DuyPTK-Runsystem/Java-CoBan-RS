package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryBatchRun;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryBatchRunRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryBatchRunCreationService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LibraryBatchRunRepository runRepository;
    private final LibraryOperationAuditService auditService;

    @Transactional
    public LibraryBatchRun createOrRestart(LocalDate runDate, Long actorId) {
        LocalDateTime now = LocalDateTime.now(LIBRARY_ZONE);
        LibraryBatchRun run = runRepository.findMatchingRunsOrderedByStartedAt("libraryOverdueFineJob", runDate,
                actorId, "FAILED", PageRequest.of(0, 1)).stream().findFirst().orElse(null);
        boolean restart = run != null;
        boolean resumeInterrupted = false;
        if (!restart) {
            run = runRepository.findMatchingRunsOrderedByStartedAt("libraryOverdueFineJob", runDate,
                    actorId, "STARTED", PageRequest.of(0, 1)).stream().findFirst().orElse(null);
            resumeInterrupted = run != null;
        }
        if (restart) {
            run.restart(now);
            runRepository.saveAndFlush(run);
        } else if (!resumeInterrupted) {
            run = runRepository.saveAndFlush(new LibraryBatchRun("libraryOverdueFineJob", runDate, actorId, now));
        }
        String action;
        if (restart || resumeInterrupted) {
            action = "LIBRARY_BATCH_RESTARTED";
        } else {
            action = "LIBRARY_BATCH_LAUNCHED";
        }
        auditService.record(actorId, action,
                "library_batch_run", run.getId(), null,
                java.util.Map.of("runDate", runDate.toString(), "jobName", run.getJobName()));
        return run;
    }
}
