package com.payflux.payment_orchestrator.presentation;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payflux.errorcode.CommonErrorCode;
import com.payflux.exception.BusinessException;

@RestController
public class DemoController {

    public record DemoPingResponse(String status, Instant timestamp) {
    }

    @GetMapping("/demo/ping")
    public DemoPingResponse ping() {
        return new DemoPingResponse("UP", Instant.now());
    }

    @GetMapping("/demo/error")
    public Void demoError() {
        throw new BusinessException(CommonErrorCode.COMMON_NOT_FOUND, "demo resources not found",
                Map.of("resourceId", "abc-123"));
    }
}
