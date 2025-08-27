package org.tsumiyoku.gov.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SessionServerRepo extends JpaRepository<SessionServer, byte[]> {
    @Query("select s from SessionServer s where s.id=:id and s.revoked=false and s.expiresAt> :now")
    Optional<SessionServer> findActive(@Param("id") byte[] id, @Param("now") Instant now);
}
