package com.bancacoxinha.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bancaCoxinhaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Banca de Coxinha - Caixa Eletronico de Salgados")
                .description("Back-end de demonstracao de Design Patterns: Repository, Strategy, Factory Method, Command e Facade")
                .version("1.0.0"));
    }
}
