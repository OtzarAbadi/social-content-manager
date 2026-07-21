package com.otzar.sscm.models;

import javax.validation.constraints.NotBlank;

public class RejectContentRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
