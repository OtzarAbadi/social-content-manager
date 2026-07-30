package com.otzar.sscm.repository;

import com.otzar.sscm.entities.InstagramConnectionSettings;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class InstagramConnectionSettingsRepository {
    private final Persist persist;

    public InstagramConnectionSettingsRepository(Persist persist) {
        this.persist = persist;
    }

    public Optional<InstagramConnectionSettings> find() {
        return persist.getQuerySession()
                .createQuery("FROM InstagramConnectionSettings ORDER BY settingsId", InstagramConnectionSettings.class)
                .setMaxResults(1)
                .uniqueResultOptional();
    }

    public InstagramConnectionSettings save(InstagramConnectionSettings settings) {
        persist.save(settings);
        return settings;
    }
}
