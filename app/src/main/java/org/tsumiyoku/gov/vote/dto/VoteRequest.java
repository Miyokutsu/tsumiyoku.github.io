package org.tsumiyoku.gov.vote.dto;

import java.util.UUID;

public record VoteRequest(UUID referendumId, String choice) {
}