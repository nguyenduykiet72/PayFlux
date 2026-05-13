package com.payflux.payment_orchestrator.presentation;

import com.payflux.errorcode.CommonErrorCode;
import com.payflux.exception.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DemoController {
    @GetMapping("/demo/error")
    public String demoError() {
        throw new BusinessException(CommonErrorCode.COMMON_NOT_FOUND, "demo resources not found",
                Map.of("resourceId","abc-123"));
    }
}
