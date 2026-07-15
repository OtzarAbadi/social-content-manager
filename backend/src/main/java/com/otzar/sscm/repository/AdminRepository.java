package com.otzar.sscm.repository;

import com.otzar.sscm.entities.Admin;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class AdminRepository {
    private final Persist persist;
    public AdminRepository(Persist persist) { this.persist = persist; }
    public Optional<Admin> findById(Long id) { return Optional.ofNullable(persist.loadObject(Admin.class, id)); }
}
