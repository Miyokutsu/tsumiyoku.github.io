package org.tsumiyoku.gov.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AdminApprovalRepo extends JpaRepository<AdminApproval, java.util.UUID> {
    @Query(value = """
              SELECT *
              FROM admin_approval
            
              WHERE(:role IS NULL OR roles@>ARRAY[:role]::text[])
            
              AND(expires_at IS NULL OR expires_at>:now)
            """, nativeQuery = true)

    List<AdminApproval> findActiveByRole(@Param("role") String role, @Param("now") Instant now);

    @Query(value = """
            
              SELECT EXISTS(SELECT 1 FROM admin_approval
                      WHERE citizen_id=:cid
                      AND(expires_at IS NULL OR expires_at>:now)
            
              AND roles
              @>ARRAY['OWNER']::text[])
            """, nativeQuery = true)

    boolean isOwner(@Param("cid") java.util.UUID citizenId, @Param("now") Instant now);

    @Query(value = "SELECT * FROM admin_approval WHERE citizen_id = :cid", nativeQuery = true)
    Optional<AdminApproval> findRaw(@Param("cid") java.util.UUID citizenId);
}