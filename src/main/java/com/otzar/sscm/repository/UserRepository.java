package com.otzar.sscm.repository;

import com.otzar.sscm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}