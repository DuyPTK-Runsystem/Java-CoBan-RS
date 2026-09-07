package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScorebookRepository extends JpaRepository<Scorebook, Long> {

    boolean existsByClassSubjectId(Long classSubjectId);

    Optional<Scorebook> findByClassSubjectId(Long classSubjectId);

    List<Scorebook> findAllByClassSubjectIdIn(Collection<Long> classSubjectIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select scorebook from Scorebook scorebook where scorebook.id = :id")
    Optional<Scorebook> findByIdForUpdate(@Param("id") Long id);
}
