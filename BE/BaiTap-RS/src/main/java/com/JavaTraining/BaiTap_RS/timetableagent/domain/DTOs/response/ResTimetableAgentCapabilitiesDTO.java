package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response;

public record ResTimetableAgentCapabilitiesDTO(
        boolean canGenerate,
        boolean canApprove,
        boolean canExecute) {
}
