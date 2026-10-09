package com.JavaTraining.BaiTap_RS.library.patron.repository;

import java.util.Collection;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronSuspension;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryPatronSuspensionRepository extends JpaRepository<LibraryPatronSuspension, Long> {

    List<LibraryPatronSuspension> findAllByPatronIdAndResolvedAtIsNullOrderBySuspendedAtAsc(Long patronId);

    List<LibraryPatronSuspension> findAllByPatronIdInAndResolvedAtIsNullOrderByPatronIdAscSuspendedAtAsc(
            Collection<Long> patronIds);

    boolean existsByPatronIdAndResolvedAtIsNull(Long patronId);

    boolean existsByPatronIdAndResolvedAtIsNullAndSource(Long patronId, String source);

    List<LibraryPatronSuspension> findAllByPatronIdAndResolvedAtIsNullAndSourceOrderBySuspendedAtAsc(
            Long patronId, String source);
}
