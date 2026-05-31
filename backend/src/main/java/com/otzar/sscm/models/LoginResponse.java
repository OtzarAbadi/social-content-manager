package com.otzar.sscm.models;

import com.otzar.sscm.entities.User;

public class LoginResponse extends BasicResponse {
    private Long userId;
    private String token;
    private String fullName;
    private String email;
    private String username;
    private String role;

    public LoginResponse (User user) {
        super(true, null);
        this.userId = user.getUser_id();
        this.token = user.getToken();
        this.fullName = user.getFull_name();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.role = user.getRole();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
