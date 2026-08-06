package com.otzar.sscm.repository;

import com.otzar.sscm.entities.ContentMedia;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository @Transactional
public class ContentMediaRepository {
    private final Persist persist; public ContentMediaRepository(Persist persist){this.persist=persist;}
    public List<ContentMedia> findByContentId(Long id){return persist.getQuerySession()
            .createQuery("FROM ContentMedia WHERE contentId=:id ORDER BY displayOrder, mediaId",ContentMedia.class)
            .setParameter("id",id).list();}
    public ContentMedia save(ContentMedia media){persist.save(media);return media;}
    public void deleteByContentId(Long id){persist.getQuerySession().createQuery("DELETE FROM ContentMedia WHERE contentId=:id").setParameter("id",id).executeUpdate();}
}
