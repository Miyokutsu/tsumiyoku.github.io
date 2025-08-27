package org.tsumiyoku.gov.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VCRepo extends JpaRepository<VerifiableCredential, UUID> {
    boolean existsBySubject_IdAndTypeAndStatus(UUID subjectId, String type, String status);
}