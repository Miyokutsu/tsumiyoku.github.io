package org.tsumiyoku.gov.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/approvals")
@RequiredArgsConstructor
public class AdminApprovalController {
    private final AdminApprovalService service;
    private final AuditService audit;

    public record UpsertReq(UUID citizenId, List<String> roles,
                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX") Instant expiresAt) {
    }

    public record Item(UUID citizenId, List<String> roles, Instant expiresAt, String approvedBy) {
    }

    private static Item toItem(AdminApproval a) {
        return new Item(a.getCitizenId(), List.of(a.getRoles()), a.getExpiresAt(), a.getApprovedBy().toString());
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public List<Item> list(@AuthenticationPrincipal OAuth2User me, @RequestParam(required = false) String role) {
        audit.append(UUID.fromString((String) me.getAttributes().get("citizenId")), "ADMIN_APPROVAL_LIST", true, Map.of("role", role));
        return service.list(role).stream().map(AdminApprovalController::toItem).toList();
    }

    @GetMapping("/{citizenId}")
    @PreAuthorize("hasRole('OWNER')")
    public Item get(@AuthenticationPrincipal OAuth2User me, @PathVariable UUID citizenId) {
        audit.append(UUID.fromString((String) me.getAttributes().get("citizenId")), "ADMIN_APPROVAL_READ", true, Map.of("target", citizenId.toString()));
        return toItem(service.get(citizenId));
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public Item upsert(@AuthenticationPrincipal OAuth2User me, @RequestBody UpsertReq req) {
        var meId = UUID.fromString((String) me.getAttributes().get("citizenId"));
        var saved = service.upsert(req.citizenId(), meId, req.roles(), req.expiresAt());
        audit.append(meId, "ADMIN_APPROVAL_UPSERT", true, Map.of("target", req.citizenId().toString(), "roles", req.roles(), "expiresAt", req.expiresAt()));
        return toItem(saved);
    }

    @DeleteMapping("/{citizenId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal OAuth2User me, @PathVariable UUID citizenId) {
        var meId = UUID.fromString((String) me.getAttributes().get("citizenId"));
        service.delete(citizenId);
        audit.append(meId, "ADMIN_APPROVAL_DELETE", true, Map.of("target", citizenId.toString()));
        return ResponseEntity.noContent().build();
    }
}