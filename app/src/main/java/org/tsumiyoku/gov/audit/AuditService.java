package org.tsumiyoku.gov.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tsumiyoku.gov.audit.db.AuditChain;
import org.tsumiyoku.gov.audit.db.AuditChainRepo;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.tsumiyoku.gov.audit.HmacChain.decodeKeyB64;
import static org.tsumiyoku.gov.audit.HmacChain.hmac;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditChainRepo repo;
    @Value("${security.audit.hmac-key}")
    String auditKeyB64;

    @Transactional
    public void append(UUID actor, String event, boolean success, Map<String, Object> meta) {
        var key = decodeKeyB64(auditKeyB64);
        Optional<AuditChain> last = repo.findTopByOrderByIdDesc();
        byte[] prev = last.map(AuditChain::getHmac).orElse(new byte[0]);

        String json = toJson(meta);
        byte[] now = Instant.now().toString().getBytes(StandardCharsets.UTF_8);
        byte[] chain = hmac(key, prev, now, event.getBytes(StandardCharsets.UTF_8), new byte[]{(byte) (success ? 1 : 0)}, json.getBytes(StandardCharsets.UTF_8));

        var row = new AuditChain();
        row.setCitizenId(actor);
        row.setEvent(event);
        row.setSuccess(success);
        row.setMeta(json);
        row.setCreatedAt(Instant.now());
        row.setPrevHmac(prev.length == 0 ? null : prev);
        row.setHmac(chain);
        repo.save(row);
    }

    private static String toJson(Map<String, Object> m) {
        if (m == null || m.isEmpty()) return "{}";
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}