package com.otzar.sscm.models;

import com.otzar.sscm.entities.Admin;
import com.otzar.sscm.entities.User;

public class SocialManagerResponse {
    private final Long adminId;
    private final Long userId;
    private final String fullName;
    private final String username;

    public SocialManagerResponse(Admin admin, User user) {
        this.adminId = admin.getAdminId();
        this.userId = user.getUser_id();
        this.fullName = user.getFull_name();
        this.username = user.getUsername();
    }

    public Long getAdminId() { return adminId; }
    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
}
