package com.adobe.acrobatsign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springdoc.core.models.GroupedOpenApi;

@Configuration
public class SpringFoxConfig {

	@Bean
    public GroupedOpenApi api() {
        return GroupedOpenApi.builder()
          .group("acrobatsign")
          .packagesToScan("com.adobe.acrobatsign.controller")
          .pathsToMatch("/**")
          .build();
    }
}
