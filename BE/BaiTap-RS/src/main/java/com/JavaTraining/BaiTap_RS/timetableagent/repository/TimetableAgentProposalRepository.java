package com.JavaTraining.BaiTap_RS.timetableagent.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableAgentProposalRepository extends JpaRepository<TimetableAgentProposal, Long> {

    @Query("select proposal from TimetableAgentProposal proposal "
            + "where proposal.id = :id and proposal.identity.actorId = :actorId")
    Optional<TimetableAgentProposal> findByIdAndActorId(@Param("id") Long id, @Param("actorId") Long actorId);
}
