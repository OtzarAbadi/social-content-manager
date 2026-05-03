package com.otzar.sscm.repository;

import com.otzar.sscm.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}