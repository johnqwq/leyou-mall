package com.leyou.order.config;

import com.github.wxpay.sdk.WXPay;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayConfiguration {
    @Bean
    @ConfigurationProperties("ly.pay")
    public PayConfig payConfig() {
        return new PayConfig();
    }

    @Bean
    public WXPay wxPay(PayConfig payConfig) {
        return new WXPay(payConfig);
    }
}
