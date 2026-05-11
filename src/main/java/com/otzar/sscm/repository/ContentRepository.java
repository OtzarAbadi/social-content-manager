package com.otzar.sscm.repository;

import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class ContentRepository {

    private final Persist persist;

    public ContentRepository(Persist persist) {
        this.persist = persist;
    }

}
