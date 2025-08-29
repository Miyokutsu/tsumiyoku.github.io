package org.tsumiyoku.gov.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket bucketFor(String key) {
        return buckets.computeIfAbsent(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1))))
                        .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofSeconds(10))))
                        .build());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();
        if (!(path.startsWith("/admin/approvals") || path.startsWith("/authz/canary"))) {
            chain.doFilter(request, response);
            return;
        }
        String ip = req.getRemoteAddr();
        String key = ip + ":" + path;
        if (bucketFor(key).tryConsume(1)) chain.doFilter(request, response);
        else ((HttpServletResponse) response).setStatus(429);
    }
}