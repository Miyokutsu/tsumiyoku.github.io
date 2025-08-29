package org.tsumiyoku.gov.audit;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tsumiyoku.gov.audit.db.AuditChain;
import org.tsumiyoku.gov.audit.db.AuditChainRepo;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.tsumiyoku.gov.audit.HmacChain.decodeKeyB64;
import static org.tsumiyoku.gov.audit.HmacChain.hmac;

@Service
@RequiredArgsConstructor
public class AuditVerifyService {
    private final AuditChainRepo repo;
    @Value("${security.audit.hmac-key}")
    String auditKeyB64;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Finding {
        long id;
        String reason;
        String details;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Report {
        long totalRows;
        int issues;
        java.util.List<Finding> findings;
        String firstCreatedAt;
        String lastCreatedAt;
        boolean ok;
    }

    @Transactional(readOnly = true)
    public Report verifyAll(int limitFindings) {
        byte[] key = decodeKeyB64(auditKeyB64);
        List<Finding> problems = new ArrayList<>();
        long total = repo.count();
        if (total == 0) return Report.builder().totalRows(0).issues(0).findings(List.of()).ok(true).build();

        try (Stream<AuditChain> stream = repo.streamAllOrderByIdAsc()) {
            byte[] prev = new byte[0];
            AuditChain first = null, last = null;
            for (Iterator<AuditChain> it = stream.iterator(); it.hasNext(); ) {
                AuditChain row = it.next();
                if (first == null) first = row;
                last = row;
                if (row.getPrevHmac() != null) {
                    if (!java.util.Arrays.equals(row.getPrevHmac(), prev))
                        add(problems, limitFindings, new Finding(row.getId(), "PREV_LINK_MISMATCH", "prev_hmac != hmac précédent"));
                } else if (prev.length != 0) {
                    add(problems, limitFindings, new Finding(row.getId(), "PREV_LINK_MISSING", "prev_hmac null avec un précédent existant"));
                }
                byte[] recalced = hmac(key, prev,
                        row.getCreatedAt().toString().getBytes(StandardCharsets.UTF_8),
                        row.getEvent().getBytes(StandardCharsets.UTF_8),
                        new byte[]{(byte) (row.isSuccess() ? 1 : 0)},
                        row.getMeta() == null ? "{}".getBytes(StandardCharsets.UTF_8) : row.getMeta().getBytes(StandardCharsets.UTF_8));
                if (!java.util.Arrays.equals(recalced, row.getHmac()))
                    add(problems, limitFindings, new Finding(row.getId(), "RECALC_MISMATCH", "hmac recalculé != stocké"));
                prev = row.getHmac();
            }
            return Report.builder()
                    .totalRows(total).issues(problems.size()).findings(problems)
                    .firstCreatedAt(first == null ? null : first.getCreatedAt().toString())
                    .lastCreatedAt(last == null ? null : last.getCreatedAt().toString())
                    .ok(problems.isEmpty()).build();
        }
    }

    private static void add(List<Finding> l, int limit, Finding f) {
        if (l.size() < limit) l.add(f);
    }
}