package com.otzar.sscm.models;

import com.otzar.sscm.entities.User;

public class LoginResponse extends BasicResponse {
    private String token;
    private String fullName;

    public LoginResponse (User user) {
        super(true, null);
        this.token = user.getToken();
        this.fullName = user.getFull_name();
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
}
