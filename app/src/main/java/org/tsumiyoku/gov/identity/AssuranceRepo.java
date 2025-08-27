package org.tsumiyoku.gov.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssuranceRepo extends JpaRepository<Assurance, UUID> {
}