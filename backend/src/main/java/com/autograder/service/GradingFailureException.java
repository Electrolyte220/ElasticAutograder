package com.autograder.service;

import com.autograder.model.FailureReason;

public class GradingFailureException extends RuntimeException {
    private final FailureReason failureReason;

    public GradingFailureException(FailureReason failureReason, String message) {
        super(message);
        this.failureReason = failureReason;
    }

    public GradingFailureException(FailureReason failureReason, String message, Throwable cause) {
        super(message, cause);
        this.failureReason = failureReason;
    }

    public FailureReason getFailureReason() {
        return failureReason;
    }
}