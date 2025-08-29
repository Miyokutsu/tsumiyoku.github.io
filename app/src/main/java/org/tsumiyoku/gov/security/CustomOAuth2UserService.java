package org.tsumiyoku.gov.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.tsumiyoku.gov.admin.AdminApprovalRepo;
import org.tsumiyoku.gov.identity.AssuranceRepo;
import org.tsumiyoku.gov.identity.VCRepo;
import org.tsumiyoku.gov.user.CitizenRepo;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final CitizenRepo citizenRepo;
    private final AssuranceRepo assuranceRepo;
    private final VCRepo vcRepo;
    private final AdminApprovalRepo approvalRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        var delegate = new DefaultOAuth2UserService();
        var u = delegate.loadUser(req);

        String email = (String) u.getAttributes().get("email");
        if (email == null) throw new OAuth2AuthenticationException("Email scope is required");

        var citizen = citizenRepo.findByEmail(email)
                .orElseThrow(() -> new OAuth2AuthenticationException("No citizen for " + email));

        var as = assuranceRepo.findById(citizen.getId()).orElse(null);
        boolean ial2 = as != null && as.getIal() >= 2;
        boolean aal2 = as != null && as.getAal() >= 2;
        boolean hasVC = vcRepo.existsBySubject_IdAndTypeAndStatus(citizen.getId(), "CitizenCredential", "ACTIVE");

        var appr = approvalRepo.findById(citizen.getId()).orElse(null);
        List<GrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (appr != null && (appr.getExpiresAt() == null || appr.getExpiresAt().isAfter(Instant.now()))) {
            var list = Arrays.asList(appr.getRoles());
            if (list.contains("OWNER")) roles.add(new SimpleGrantedAuthority("ROLE_OWNER"));
            if (ial2 && aal2 && hasVC && list.contains("ADMIN")) roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", email);
        attrs.put("citizenId", citizen.getId().toString());
        return new DefaultOAuth2User(roles, attrs, "email");
    }
}