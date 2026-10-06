package com.JavaTraining.BaiTap_RS.timetableagent.ai;

public record TimetableAgentProposalPrompt(
        String systemInstructions,
        String snapshotJson,
        String userRequest,
        String outputSchemaJson) {
}
