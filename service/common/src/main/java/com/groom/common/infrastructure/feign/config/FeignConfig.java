package com.groom.common.infrastructure.feign.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(
            5000, TimeUnit.MILLISECONDS,
            5000, TimeUnit.MILLISECONDS,
            true
        );
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            String targetUrl = template.feignTarget().url();

            // 내부 MSA 요청은 인증 불필요 (/internal/** 은 permitAll)
            template.removeHeader("Authorization");
            log.debug(">>> [Feign-Global] Internal API call to {}", targetUrl);
        };
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }
}
