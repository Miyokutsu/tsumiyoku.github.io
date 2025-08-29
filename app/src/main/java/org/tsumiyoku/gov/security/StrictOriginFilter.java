package org.tsumiyoku.gov.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

@Component
public class StrictOriginFilter implements Filter {
    private final Set<String> allowedHosts;

    public StrictOriginFilter(@Value("${security.csrf.allowed-hosts}") String allowed) {
        this.allowedHosts = java.util.Set.of(allowed.split(","));
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        String path = r.getRequestURI();
        String m = r.getMethod();
        boolean write = "POST".equals(m) || "PUT".equals(m) || "PATCH".equals(m) || "DELETE".equals(m);
        if (write && path.startsWith("/admin/approvals")) {
            String origin = r.getHeader("Origin");
            String referer = r.getHeader("Referer");
            boolean ok = false;
            if (origin != null) ok = allowedHosts.contains(hostOf(origin));
            else if (referer != null) ok = allowedHosts.contains(hostOf(referer));
            if (!ok) {
                ((HttpServletResponse) res).sendError(403);
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private static String hostOf(String url) {
        try {
            return new URI(url).getHost();
        } catch (Exception e) {
            return "";
        }
    }
}