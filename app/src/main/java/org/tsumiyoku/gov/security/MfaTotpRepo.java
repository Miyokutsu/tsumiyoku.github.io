package org.tsumiyoku.gov.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MfaTotpRepo extends JpaRepository<MfaTotp, UUID> {
}
