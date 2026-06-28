package com.payflux.payment_orchestrator.infrastructure.web;

import com.payflux.core.tenant.TenantContext;
import com.payflux.payment_orchestrator.application.MerchantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
public class PublicApiTenantFilter extends OncePerRequestFilter implements Ordered {
    public static final String HEADER_CONSUMER_USERNAME = "X-Consumer-Username";
    public static final String HEADER_MERCHANT_ID = "X-Merchant-Id";

    private final MerchantResolver merchantResolver;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UUID merchantId = resolveMerchantId(request);
        try {
            TenantContext.callWhere(merchantId, () -> {
                filterChain.doFilter(request, response);
                return null;
            });
        } catch (ServletException | IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private UUID resolveMerchantId(HttpServletRequest request) {
        String direct = request.getHeader(HEADER_MERCHANT_ID);
        if(direct != null && !direct.isBlank()) {
            return UUID.fromString(direct);
        }
        String consumerUserName = request.getHeader(HEADER_CONSUMER_USERNAME);
        return merchantResolver.resolveFromConsumerUserName(consumerUserName);
    }
}
