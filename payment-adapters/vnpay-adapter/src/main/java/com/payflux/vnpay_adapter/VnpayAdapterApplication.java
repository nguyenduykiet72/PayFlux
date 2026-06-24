package com.payflux.vnpay_adapter;

import com.payflux.vnpay_adapter.config.VnpayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(VnpayProperties.class)
public class VnpayAdapterApplication {

	public static void main(String[] args) {
		SpringApplication.run(VnpayAdapterApplication.class, args);
	}

}
