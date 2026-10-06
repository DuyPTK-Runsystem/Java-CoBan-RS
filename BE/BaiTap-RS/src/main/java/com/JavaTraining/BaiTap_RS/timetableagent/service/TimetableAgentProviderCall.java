package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentInvalidOutputException;
import org.springframework.http.HttpStatus;

public final class TimetableAgentProviderCall {

    private static final String REQUEST_ID = "requestId";

    private TimetableAgentProviderCall() {
    }

    public static <T> T call(Supplier<T> supplier, long deadline) {
        long remaining = remaining(deadline);
        String requestId = org.slf4j.MDC.get(REQUEST_ID);
        FutureTask<T> call = new FutureTask<>(() -> withRequestId(supplier, requestId));
        Thread.ofVirtual().name("timetable-agent-proposal").start(call);
        try {
            return requireValue(call.get(remaining, TimeUnit.NANOSECONDS));
        } catch (TimeoutException exception) {
            call.cancel(true);
            throw new AppException(HttpStatus.GATEWAY_TIMEOUT, "The timetable model request timed out.", exception);
        } catch (InterruptedException exception) {
            call.cancel(true);
            Thread.currentThread().interrupt();
            throw new AppException(HttpStatus.REQUEST_TIMEOUT, "The timetable model request was cancelled.", exception);
        } catch (ExecutionException exception) {
            throw propagate(exception);
        }
    }

    /* default */ static <T> T withRequestId(Supplier<T> supplier, String requestId) {
        String previous = org.slf4j.MDC.get(REQUEST_ID);
        org.slf4j.MDC.put(REQUEST_ID, requestId == null || requestId.isBlank() ? "N/A" : requestId);
        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                org.slf4j.MDC.remove(REQUEST_ID);
            } else {
                org.slf4j.MDC.put(REQUEST_ID, previous);
            }
        }
    }

    private static long remaining(long deadline) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
            throw new AppException(HttpStatus.GATEWAY_TIMEOUT, "The timetable model request timed out.");
        }
        return remaining;
    }

    private static <T> T requireValue(T value) {
        if (value == null) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "The timetable model returned no proposal.");
        }
        return value;
    }

    private static RuntimeException propagate(ExecutionException exception) {
        if (exception.getCause() instanceof TimetableAgentInvalidOutputException invalidOutput) {
            return new TimetableAgentInvalidOutputException(invalidOutput.getMessage(), exception);
        }
        return new AppException(HttpStatus.BAD_GATEWAY, "The timetable model request failed.", exception);
    }
}
