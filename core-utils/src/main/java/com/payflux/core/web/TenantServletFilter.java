package com.payflux.core.web;

import com.payflux.core.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Extracts X-Merchant-Id from HTTP request and binds it to TenantContext
 * for the entire filter chain scope. When the request completes, ScopedValue
 * auto-cleans up — no manual remove() needed (unlike ThreadLocal).
 *
 * Does NOT reject requests without the header — health/actuator/demo endpoints
 * don't need tenant context. Service code calling TenantContext.get() without
 * a bound value will get MissingTenantContextException (401).
 */
public class TenantServletFilter extends OncePerRequestFilter implements Ordered {

    public static final String HEADER = "X-Merchant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String raw = req.getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            chain.doFilter(req, res);
            return;
        }

        UUID merchantId;
        try {
            merchantId = UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-Merchant-Id must be a valid UUID");
            return;
        }

        try {
            TenantContext.callWhere(merchantId, () -> {
                chain.doFilter(req, res);
                return null;
            });
        } catch (ServletException | IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
