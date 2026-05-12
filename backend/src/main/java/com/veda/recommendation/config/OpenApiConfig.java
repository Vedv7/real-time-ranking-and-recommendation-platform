package com.veda.recommendation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI recommendationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Real-Time Ranking and Recommendation Platform API")
                        .version("0.1.0")
                        .description("Spring Boot APIs for event ingestion, feature updates, candidate generation, and feed ranking."));
    }
}
