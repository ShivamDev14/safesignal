package com.safesignal.report.exception;

public class SafetyReportNotFoundException extends RuntimeException {

    public SafetyReportNotFoundException(Long streetSegmentId) {
        super("Street segment " + streetSegmentId + " was not found");
    }
}
