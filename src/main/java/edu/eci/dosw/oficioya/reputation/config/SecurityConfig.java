package edu.eci.dosw.oficioya.reputation.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del microservicio: toda petición debe traer un JWT válido emitido por Identity
 * y reenviado por el Orchestrator. Los roles se leen del claim configurado
 * (por defecto "roles", con valores ROLE_CONTRATANTE, ROLE_TRABAJADOR o ROLE_ADMINISTRADOR).
 *
 * PENDIENTE de confirmar con Identity (Squad 1): algoritmo, secreto y nombre del claim de roles.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Documentación de la API (Swagger): pública; se apaga con SWAGGER_ENABLED=false.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Avisos de otros dominios vía Orchestrator. SUPUESTO: el Orchestrator usa un token con ROLE_SISTEMA.
                        .requestMatchers("/api/reputation/eventos/**").hasAnyRole("SISTEMA", "ADMINISTRADOR")
                        // Moderación y estadísticas: solo administradores.
                        .requestMatchers("/api/reputation/reportes/**", "/api/reputation/estadisticas/**")
                        .hasRole("ADMINISTRADOR")
                        // Calificar y reportar: solo las partes de un servicio.
                        .requestMatchers(HttpMethod.POST, "/api/reputation/resenas", "/api/reputation/resenas/*/reportes")
                        .hasAnyRole("TRABAJADOR", "CONTRATANTE")
                        // Consultas: cualquier usuario autenticado.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${oficioya.security.jwt-secret}") String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            @Value("${oficioya.security.roles-claim}") String rolesClaim) {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(rolesClaim);
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
