package org.tsumiyoku.gov.vote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Repos
public interface ReferendumRepo extends JpaRepository<Referendum, UUID> {}
