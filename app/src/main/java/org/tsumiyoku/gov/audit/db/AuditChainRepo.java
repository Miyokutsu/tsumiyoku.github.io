package org.tsumiyoku.gov.audit.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Stream;

public interface AuditChainRepo extends JpaRepository<AuditChain, Long> {
    Optional<AuditChain> findTopByOrderByIdDesc();

    @Query("select a from AuditChain a order by a.id asc")
    @Transactional(readOnly = true)
    Stream<AuditChain> streamAllOrderByIdAsc();
}