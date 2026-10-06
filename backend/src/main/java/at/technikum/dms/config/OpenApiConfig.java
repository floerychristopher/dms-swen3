package at.technikum.dms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadaten für die generierte OpenAPI-Spezifikation (/v3/api-docs, Swagger-UI unter /swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dmsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("DMS REST API")
                .description("Document Management System – SWEN3")
                .version("v1"));
    }
}

