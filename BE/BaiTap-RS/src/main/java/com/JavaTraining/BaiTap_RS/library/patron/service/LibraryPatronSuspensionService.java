package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronSuspension;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronSuspensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryPatronSuspensionService {

    private static final String MANUAL_SOURCE = "MANUAL";
    private final LibraryPatronSuspensionRepository repository;

    @Transactional
    public void suspendManually(Long patronId, String reason, LocalDateTime now) {
        repository.save(new LibraryPatronSuspension(patronId, reason, "MANUAL", now));
    }

    @Transactional
    public boolean resolveManual(Long patronId, Long actorId, LocalDateTime now) {
        List<LibraryPatronSuspension> manualSuspensions = repository
                .findAllByPatronIdAndResolvedAtIsNullAndSourceOrderBySuspendedAtAsc(patronId, MANUAL_SOURCE);
        manualSuspensions.forEach(suspension -> suspension.resolve(actorId, now));
        if (!manualSuspensions.isEmpty()) {
            repository.flush();
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean hasUnresolved(Long patronId) {
        return repository.existsByPatronIdAndResolvedAtIsNull(patronId);
    }
}
