package com.otzar.sscm.model;

public class ClientEntity extends BasicEntity {
    private SocialManagerEntity socialManagerEntity;
    private String businessName;
    private String phone;

    public ClientEntity () {
    }

    public SocialManagerEntity getSocialManagerEntity() {
        return socialManagerEntity;
    }

    public void setSocialManagerEntity(SocialManagerEntity socialManagerEntity) {
        this.socialManagerEntity = socialManagerEntity;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
