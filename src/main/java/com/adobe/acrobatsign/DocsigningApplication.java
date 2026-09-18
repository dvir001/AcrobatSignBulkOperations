package com.adobe.acrobatsign;

import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class DocsigningApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(DocsigningApplication.class);
		application.setDefaultProperties(Collections.singletonMap("server.address", "127.0.0.1"));
		application.run(args);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/sendagreement").allowedOrigins("http://localhost");
			}
		};
	}
}
