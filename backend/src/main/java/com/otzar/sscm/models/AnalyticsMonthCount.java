package com.otzar.sscm.models;

public class AnalyticsMonthCount {
    private final String month;
    private final long count;

    public AnalyticsMonthCount(String month, long count) {
        this.month = month;
        this.count = count;
    }

    public String getMonth() { return month; }
    public long getCount() { return count; }
}
