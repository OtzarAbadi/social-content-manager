package com.otzar.sscm.entities;

public class ContentVersionMedia {
    private Long versionMediaId; private Long contentVersionId; private String mediaUrl;
    private String mediaType; private Integer displayOrder; private String thumbnailUrl;
    public Long getVersionMediaId(){return versionMediaId;} public void setVersionMediaId(Long v){versionMediaId=v;}
    public Long getContentVersionId(){return contentVersionId;} public void setContentVersionId(Long v){contentVersionId=v;}
    public String getMediaUrl(){return mediaUrl;} public void setMediaUrl(String v){mediaUrl=v;}
    public String getMediaType(){return mediaType;} public void setMediaType(String v){mediaType=v;}
    public Integer getDisplayOrder(){return displayOrder;} public void setDisplayOrder(Integer v){displayOrder=v;}
    public String getThumbnailUrl(){return thumbnailUrl;} public void setThumbnailUrl(String v){thumbnailUrl=v;}
}
