package com.JavaTraining.BaiTap_RS.library.circulation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryPolicyRepository extends JpaRepository<LibraryCirculationPolicy, Long> {

    @Query("select p from LibraryCirculationPolicy p left join fetch p.fineTiers "
            + "where p.snapshot.effectiveAt <= :now order by p.snapshot.effectiveAt desc, p.id desc")
    List<LibraryCirculationPolicy> findEffective(@Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LibraryCirculationPolicy p where p.snapshot.policyVersion = :version")
    Optional<LibraryCirculationPolicy> findByVersionForUpdate(@Param("version") String version);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LibraryCirculationPolicy p where p.id = (select max(p2.id) from LibraryCirculationPolicy p2)")
    Optional<LibraryCirculationPolicy> findLatestForUpdate();

    @Query("select p from LibraryCirculationPolicy p where p.snapshot.policyVersion = :version")
    Optional<LibraryCirculationPolicy> findByVersion(@Param("version") String version);

    Optional<LibraryCirculationPolicy> findFirstByOrderByIdDesc();
}
