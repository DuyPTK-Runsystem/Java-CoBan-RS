package com.JavaTraining.BaiTap_RS.timetableagent.ai;

public class TimetableAgentModelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TimetableAgentModelException(String message) {
        super(message);
    }

    public TimetableAgentModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
