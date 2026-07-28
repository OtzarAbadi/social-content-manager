package com.otzar.sscm.models;

import javax.validation.constraints.Pattern;

public class UpdateClientRequest {

    private Long userId;
    private Long adminId;
    private Boolean clearAdminAssignment;
    @Pattern(regexp = ".*\\S.*", message = "Business name must not be blank")
    private String businessName;
    private String phone;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public Boolean getClearAdminAssignment() { return clearAdminAssignment; }
    public void setClearAdminAssignment(Boolean clearAdminAssignment) { this.clearAdminAssignment = clearAdminAssignment; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
