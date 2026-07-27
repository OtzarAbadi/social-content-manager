package com.otzar.sscm.service;

public class InstagramPublishException extends RuntimeException {
    public enum Reason {
        NOT_CONFIGURED,
        CONTENT_NOT_FOUND,
        CONTENT_NOT_APPROVED,
        IMAGE_REQUIRED,
        IMAGE_NOT_PUBLIC,
        META_API_FAILURE
    }

    private final Reason reason;

    public InstagramPublishException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public InstagramPublishException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
