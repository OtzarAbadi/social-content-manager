package com.otzar.sscm.repository;

import com.otzar.sscm.model.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

    List<Content> findByClientId(Long clientId);
}