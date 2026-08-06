package com.otzar.sscm.entities;

import java.time.LocalDateTime;

public class ContentMedia {
    private Long mediaId;
    private Long contentId;
    private String mediaUrl;
    private String mediaType;
    private Integer displayOrder;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    public Long getMediaId(){return mediaId;} public void setMediaId(Long v){mediaId=v;}
    public Long getContentId(){return contentId;} public void setContentId(Long v){contentId=v;}
    public String getMediaUrl(){return mediaUrl;} public void setMediaUrl(String v){mediaUrl=v;}
    public String getMediaType(){return mediaType;} public void setMediaType(String v){mediaType=v;}
    public Integer getDisplayOrder(){return displayOrder;} public void setDisplayOrder(Integer v){displayOrder=v;}
    public String getThumbnailUrl(){return thumbnailUrl;} public void setThumbnailUrl(String v){thumbnailUrl=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
