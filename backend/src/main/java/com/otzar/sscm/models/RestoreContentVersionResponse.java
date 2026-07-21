package com.otzar.sscm.models;

import com.otzar.sscm.entities.Content;

public class RestoreContentVersionResponse {
    private final Content content;
    private final Integer restoredFromVersionNumber;
    private final Integer newVersionNumber;
    private final boolean changed;

    public RestoreContentVersionResponse(Content content, Integer restoredFromVersionNumber,
                                         Integer newVersionNumber, boolean changed) {
        this.content = content;
        this.restoredFromVersionNumber = restoredFromVersionNumber;
        this.newVersionNumber = newVersionNumber;
        this.changed = changed;
    }

    public Content getContent() { return content; }
    public Integer getRestoredFromVersionNumber() { return restoredFromVersionNumber; }
    public Integer getNewVersionNumber() { return newVersionNumber; }
    public boolean isChanged() { return changed; }
}
