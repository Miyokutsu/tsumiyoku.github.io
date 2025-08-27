package org.tsumiyoku.gov.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CitizenRepo extends JpaRepository<Citizen, UUID> {
    Optional<Citizen> findByEmail(String email);
}