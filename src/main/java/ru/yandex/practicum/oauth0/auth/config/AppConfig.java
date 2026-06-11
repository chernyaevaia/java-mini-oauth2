package ru.yandex.practicum.oauth0.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.oauth0.auth.util.JwtUtil;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AppConfig {

    @Bean
    public JwtUtil jwtUtil(AuthProperties props) {
        return new JwtUtil(props.getSecret(), props.getClockSkewSec());
    }
}