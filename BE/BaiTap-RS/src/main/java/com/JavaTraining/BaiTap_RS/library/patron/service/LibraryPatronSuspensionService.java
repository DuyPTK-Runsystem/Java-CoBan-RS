package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronSuspension;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronSuspensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryPatronSuspensionService {

    private final LibraryPatronSuspensionRepository repository;

    @Transactional
    public void suspendManually(Long patronId, String reason, LocalDateTime now) {
        repository.save(new LibraryPatronSuspension(patronId, reason, "MANUAL", now));
    }

    @Transactional
    public void resolveAll(Long patronId, Long actorId, LocalDateTime now) {
        repository.findAllByPatronIdAndResolvedAtIsNullOrderBySuspendedAtAsc(patronId)
                .forEach(suspension -> suspension.resolve(actorId, now));
        repository.flush();
    }
}
