package org.tsumiyoku.gov.vote;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tsumiyoku.gov.auth.CitizenRepo;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final VoteRepo voteRepo; private final ReferendumRepo refRepo; private final CitizenRepo citizenRepo;

    @Transactional
    public void cast(UUID citizenId, UUID referendumId, String choice) {
        var ref = refRepo.findById(referendumId).orElseThrow();
        var now = Instant.now();
        if (now.isBefore(ref.getStartsAt()) || now.isAfter(ref.getEndsAt())) {
            throw new IllegalStateException("Referendum not open");
        }
        if (voteRepo.existsByCitizen_IdAndReferendum_Id(citizenId, referendumId)) {
            throw new IllegalStateException("Already voted");
        }
        var citizen = citizenRepo.findById(citizenId).orElseThrow();
        voteRepo.save(Vote.builder().citizen(citizen).referendum(ref).choice(choice).build());
    }
}