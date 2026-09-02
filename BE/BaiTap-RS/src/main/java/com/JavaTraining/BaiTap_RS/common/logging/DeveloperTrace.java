package com.JavaTraining.BaiTap_RS.common.logging;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Formats developer trace logs consistently without exposing request payloads.
 */
@SuppressWarnings("PMD.GuardLogStatement")
public final class DeveloperTrace {

    private static final String REQUEST_ID_KEY = "requestId";

    private DeveloperTrace() {
    }

    public static void trace(Logger logger, String operation) {
        if (logger.isInfoEnabled()) {
            logger.info(/* NOPMD GuardLogStatement */
                    ">>>{}: request accepted [{}] [{}]",
                    operation,
                    Thread.currentThread().getName(),
                    MDC.get(REQUEST_ID_KEY));
        }
    }

    public static void trace(Class<?> source, String operation) {
        trace(LoggerFactory.getLogger(source), operation);
    }

    public static void trace(Class<?> source, String operation, String detailFormat, Object... detailArguments) {
        Logger logger = LoggerFactory.getLogger(source);
        if (logger.isInfoEnabled()) {
            Object[] arguments = Arrays.copyOf(detailArguments, detailArguments.length + 2);
            arguments[detailArguments.length] = Thread.currentThread().getName();
            arguments[detailArguments.length + 1] = MDC.get(REQUEST_ID_KEY);
            logger.info(/* NOPMD GuardLogStatement */
                    ">>>{}: " + detailFormat + " [{}] [{}]",
                    prepend(operation, arguments));
        }
    }

    private static Object[] prepend(String operation, Object... arguments) {
        Object[] values = new Object[arguments.length + 1];
        values[0] = operation;
        System.arraycopy(arguments, 0, values, 1, arguments.length);
        return values;
    }
}
