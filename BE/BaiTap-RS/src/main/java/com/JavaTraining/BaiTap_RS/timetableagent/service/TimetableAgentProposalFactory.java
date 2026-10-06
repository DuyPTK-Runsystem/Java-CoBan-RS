package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.Duration;
import java.time.Instant;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalIdentity;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalPayload;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus;

public final class TimetableAgentProposalFactory {

    private TimetableAgentProposalFactory() {
    }

    public static TimetableAgentProposal create(Long actorId, TimetableAgentSnapshot snapshot,
            TimetableAgentProposalGenerator.GeneratedProposal completed, TimetableAgentPayloadCodec codec,
            Duration ttl) {
        String proposalJson = codec.write(completed.proposal());
        String issuesJson = codec.write(completed.result().issues());
        String diffJson = codec.write(completed.result().diff());
        String proposalHash = TimetableAgentPayloadCodec.hash(snapshot.sourceFingerprint() + "\n" + proposalJson
                + "\n" + issuesJson + "\n" + diffJson);
        TimetableAgentProposalIdentity identity = new TimetableAgentProposalIdentity(actorId,
                snapshot.targetRevisionId(), snapshot.semesterId(), snapshot.expectedVersion(), 1L,
                snapshot.snapshotId(), snapshot.sourceFingerprint(), proposalHash);
        TimetableAgentProposalPayload payload = new TimetableAgentProposalPayload(snapshot.snapshotJson(),
                snapshot.requestJson(), proposalJson, codec.write(completed.result()), issuesJson, diffJson);
        return new TimetableAgentProposal(identity, payload,
                TimetableAgentProposalStatus.valueOf(completed.result().status().name()), Instant.now().plus(ttl));
    }
}
