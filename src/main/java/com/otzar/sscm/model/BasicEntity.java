package com.otzar.sscm.model;

public class BasicEntity {
    private int id;
    private boolean deleted;

    public BasicEntity () {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
