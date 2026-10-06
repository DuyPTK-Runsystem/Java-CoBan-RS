package com.JavaTraining.BaiTap_RS.timetableagent.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableAgentApprovalRepository extends JpaRepository<TimetableAgentApproval, Long> {

    Optional<TimetableAgentApproval> findFirstByProposalIdAndProposalVersionOrderByApprovedAtDesc(
            Long proposalId, Long proposalVersion);

    boolean existsByProposalIdAndProposalVersionAndProposalHashAndActorId(
            Long proposalId, Long proposalVersion, String proposalHash, Long actorId);
}
