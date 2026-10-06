package com.JavaTraining.BaiTap_RS.timetableagent.ai;

/** Repairable schema violation; provider failures and refusal must not enter repair loops. */
public class TimetableAgentInvalidOutputException extends TimetableAgentModelException {

    private static final long serialVersionUID = 1L;

    public TimetableAgentInvalidOutputException(String message) {
        super(message);
    }

    public TimetableAgentInvalidOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
