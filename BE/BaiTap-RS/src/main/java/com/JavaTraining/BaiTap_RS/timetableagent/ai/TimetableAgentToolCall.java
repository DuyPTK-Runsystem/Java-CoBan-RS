package com.JavaTraining.BaiTap_RS.timetableagent.ai;

public record TimetableAgentToolCall(
        String callId,
        String name,
        String argumentsJson) {
}
