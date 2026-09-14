package com.safesignal.safety.exception;

public class StreetSegmentNotFoundException extends RuntimeException {

    public StreetSegmentNotFoundException(Long segmentId) {
        super("Street segment " + segmentId + " was not found");
    }
}
