package com.adobe.acrobatsign.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login").permitAll()
                        .anyRequest().hasAnyRole("OPERATOR", "ADMIN"))
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults())
                .csrf(Customizer.withDefaults())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService localOperator(
            @Value("${spring.security.user.name:${APP_USERNAME:operator}}") String username,
            @Value("${spring.security.user.password:${APP_PASSWORD:}}") String password,
            PasswordEncoder encoder) {
        if (password.isBlank()) {
            password = UUID.randomUUID().toString();
            LOGGER.warn("Generated local operator password: {}. Set APP_PASSWORD to replace it.", password);
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(encoder.encode(password)).roles("OPERATOR").build());
    }

    @Bean
    WebServerFactoryCustomizer<ConfigurableWebServerFactory> loopbackByDefault(Environment environment) {
        return factory -> {
            // External spring.config.location may replace the packaged application.yml.
            if (!environment.containsProperty("server.address")) {
                try {
                    factory.setAddress(InetAddress.getByName("127.0.0.1"));
                } catch (UnknownHostException exception) {
                    throw new IllegalStateException("Cannot configure loopback binding", exception);
                }
            }
        };
    }
}
