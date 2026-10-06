package com.JavaTraining.BaiTap_RS.timetableagent.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentAction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableAgentActionRepository extends JpaRepository<TimetableAgentAction, Long> {

    Optional<TimetableAgentAction> findByActorIdAndIdempotencyKey(Long actorId, String idempotencyKey);

    Optional<TimetableAgentAction> findByIdAndActorId(Long id, Long actorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select action from TimetableAgentAction action where action.id = :id")
    Optional<TimetableAgentAction> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select action from TimetableAgentAction action "
            + "where action.actorId = :actorId and action.idempotencyKey = :key")
    Optional<TimetableAgentAction> findByActorIdAndIdempotencyKeyForUpdate(
            @Param("actorId") Long actorId, @Param("key") String key);
}
