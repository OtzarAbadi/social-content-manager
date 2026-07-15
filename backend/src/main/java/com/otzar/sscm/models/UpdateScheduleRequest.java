package com.otzar.sscm.models;

import java.time.LocalDateTime;

public class UpdateScheduleRequest {
    private LocalDateTime plannedPublishDate;

    public LocalDateTime getPlannedPublishDate() { return plannedPublishDate; }
    public void setPlannedPublishDate(LocalDateTime plannedPublishDate) {
        this.plannedPublishDate = plannedPublishDate;
    }
}
