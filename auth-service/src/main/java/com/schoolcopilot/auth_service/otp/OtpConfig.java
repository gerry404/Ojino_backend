package com.schoolcopilot.auth_service.otp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtpConfig {

    /** N'est utilise que tant qu'aucun vrai fournisseur SMS n'est branche. */
    @Bean
    @ConditionalOnMissingBean(SmsSender.class)
    SmsSender loggingSmsSender() {
        return new LoggingSmsSender();
    }
}
