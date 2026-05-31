package com.otzar.sscm.models;

import com.otzar.sscm.entities.User;

public class UserMeResponse {
    private Long id;
    private String username;
    private String role;

    public UserMeResponse() {
    }

    public UserMeResponse(User user) {
        this.id = user.getUser_id();
        this.username = user.getUsername();
        this.role = user.getRole();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
