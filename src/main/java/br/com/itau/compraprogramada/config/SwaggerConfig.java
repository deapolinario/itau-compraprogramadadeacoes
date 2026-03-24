package br.com.itau.compraprogramada.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Compra Programada de Ações")
                        .description("API REST para o produto de compra programada de ações da Itau Corretora")
                        .version("1.0.0"));
    }
}
