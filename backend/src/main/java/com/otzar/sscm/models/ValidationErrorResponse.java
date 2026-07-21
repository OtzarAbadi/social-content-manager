package com.otzar.sscm.models;

import java.util.Map;

public class ValidationErrorResponse {
    private final int status;
    private final String error;
    private final Map<String, String> fieldErrors;

    public ValidationErrorResponse(int status, String error, Map<String, String> fieldErrors) {
        this.status = status;
        this.error = error;
        this.fieldErrors = fieldErrors;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
