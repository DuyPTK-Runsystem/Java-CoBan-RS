package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryBatchRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryBatchRunStatusService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LibraryBatchRunRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long runId, long processed, long skipped) {
        repository.findById(runId).ifPresent(run ->
                run.complete(processed, skipped, LocalDateTime.now(LIBRARY_ZONE)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long runId, long processed, long skipped) {
        repository.findById(runId).ifPresent(run ->
                run.fail(processed, skipped, LocalDateTime.now(LIBRARY_ZONE)));
    }
}
