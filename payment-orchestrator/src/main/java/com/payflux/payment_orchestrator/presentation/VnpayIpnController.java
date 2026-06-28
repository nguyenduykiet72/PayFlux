package com.payflux.payment_orchestrator.presentation;

import com.payflux.payment_orchestrator.application.PaymentIpnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/vnpay")
public class VnpayIpnController {
    private final PaymentIpnService paymentIpnService;
    public record IpnResponse(String RspCode, String Message) {}

    @GetMapping("/ipn")
    public IpnResponse handleIpn(@RequestParam Map<String,String> params) {
        PaymentIpnService.IpnResult result = paymentIpnService.handle(params);
        return new IpnResponse(result.rspCode(), result.message());
    }
}
