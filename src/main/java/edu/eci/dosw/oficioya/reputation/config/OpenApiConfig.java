package edu.eci.dosw.oficioya.reputation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Documentación OpenAPI (Swagger). Todos los endpoints piden un JWT: en Swagger UI se pega en "Authorize".
 */
@Configuration
public class OpenApiConfig {

    static final String ESQUEMA_JWT = "bearer-jwt";

    @Bean
    public OpenAPI documentacion() {
        return new OpenAPI()
                .info(new Info()
                        .title("OficioYa · Reputation")
                        .version("sprint-3")
                        .description("Calificaciones mutuas, reseñas, reputación, pausado automático, penalizaciones y moderación. Microservicio del Squad 5 (puerto 8082). "
                                + "En producción se accede a través del Orchestrator."))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT emitido por Identity, con el claim 'roles'")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
