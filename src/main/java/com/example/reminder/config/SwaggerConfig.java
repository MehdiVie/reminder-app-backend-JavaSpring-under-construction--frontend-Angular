package com.example.reminder.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI reminderOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Reminder API")
                        .description("REST API for Events management (Erinnerungskalender)")
                        .version("1.0.0")
                        .license(new License().name("MIT License")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project Repository")
                        .url("https://github.com/yourusername/reminder"));
    }
}
