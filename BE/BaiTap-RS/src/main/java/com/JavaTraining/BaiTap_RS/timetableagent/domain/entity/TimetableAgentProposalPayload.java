package com.JavaTraining.BaiTap_RS.timetableagent.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimetableAgentProposalPayload {

    private static final String JSON_COLUMN_TYPE = "LONGTEXT";
    @Column(name = "snapshot_json", nullable = false, columnDefinition = JSON_COLUMN_TYPE)
    private String snapshotJson;
    @Column(name = "request_json", nullable = false, columnDefinition = JSON_COLUMN_TYPE)
    private String requestJson;
    @Column(name = "proposal_json", nullable = false, columnDefinition = JSON_COLUMN_TYPE)
    private String proposalJson;
    @Column(name = "validation_json", nullable = false, columnDefinition = JSON_COLUMN_TYPE)
    private String validationJson;
    @Column(name = "issues_json", nullable = false, columnDefinition = JSON_COLUMN_TYPE)
    private String issuesJson;
    @Column(name = "diff_json", nullable = false, columnDefinition = JSON_COLUMN_TYPE)
    private String diffJson;
}
