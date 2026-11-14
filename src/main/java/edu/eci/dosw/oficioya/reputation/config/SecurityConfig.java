package edu.eci.dosw.oficioya.reputation.config;

import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del microservicio: toda petición debe traer un JWT válido emitido por Identity
 * y reenviado por el Orchestrator. Mismo formato que Identity (Squad 1) y los demás dominios:
 * HS256 con el secreto JWT_SECRET en Base64, el sub es el UUID del usuario y los roles vienen en el claim
 * "roles" sin prefijo (CONTRATANTE, TRABAJADOR, ADMIN; SISTEMA para los avisos entre dominios).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Documentación de la API (Swagger): pública; se apaga con SWAGGER_ENABLED=false.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Avisos de otros dominios vía Orchestrator. SUPUESTO: el Orchestrator usa un token con ROLE_SISTEMA.
                        .requestMatchers("/api/v1/reputation/eventos/**").hasAnyRole("SISTEMA", "ADMIN")
                        // Moderación y estadísticas: solo administradores.
                        .requestMatchers("/api/v1/reputation/reportes/**", "/api/v1/reputation/estadisticas/**")
                        .hasRole("ADMIN")
                        // Calificar y reportar: solo las partes de un servicio.
                        .requestMatchers(HttpMethod.POST, "/api/v1/reputation/resenas", "/api/v1/reputation/resenas/*/reportes")
                        .hasAnyRole("TRABAJADOR", "CONTRATANTE")
                        // Consultas: cualquier usuario autenticado.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${oficioya.security.jwt-secret}") String secret) {
        // Igual que Identity: el secreto se usa decodificado de Base64.
        SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(secret.trim()), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            @Value("${oficioya.security.roles-claim}") String rolesClaim) {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(rolesClaim);
        // Los roles llegan sin prefijo (por ejemplo "ADMIN"); Spring Security los usa como ROLE_ADMIN.
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
