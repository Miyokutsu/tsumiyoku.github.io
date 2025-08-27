package org.tsumiyoku.gov.vote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VoteRepo extends JpaRepository<Vote, UUID> {
    boolean existsByCitizen_IdAndReferendum_Id(UUID citizenId, UUID refId);
}
