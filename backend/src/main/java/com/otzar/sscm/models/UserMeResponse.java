package com.otzar.sscm.models;

import com.otzar.sscm.entities.User;

public class UserMeResponse {
    private Long id;
    private Long clientId;
    private String username;
    private String email;
    private String role;
    private String fullName;

    public UserMeResponse() {
    }

    public UserMeResponse(User user) {
        this.id = user.getUser_id();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.fullName = user.getFull_name();
    }

    public UserMeResponse(User user, Long clientId) {
        this(user);
        this.clientId = clientId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
