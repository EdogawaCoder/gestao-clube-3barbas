package br.com.clube3barbas.config;

import br.com.clube3barbas.security.DevelopmentAuthenticationFilter;
import br.com.clube3barbas.security.FirebaseAuthenticationFilter;
import br.com.clube3barbas.security.FirebaseTokenVerifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FirebaseProperties firebaseProperties,
            ApplicationSecurityProperties securityProperties,
            Environment environment,
            ObjectProvider<FirebaseTokenVerifier> tokenVerifierProvider,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/health", "/api/v1/health/firestore", "/actuator/health/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"status\":401,\"message\":\"Autenticacao obrigatoria.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"status\":403,\"message\":\"Acesso nao autorizado para este perfil.\"}");
                        }));

        if (firebaseProperties.enabled()) {
            var tokenVerifier = tokenVerifierProvider.getIfAvailable();
            if (tokenVerifier == null) {
                throw new IllegalStateException("Firebase esta habilitado, mas o verificador de token nao foi configurado.");
            }
            http.addFilterBefore(
                    new FirebaseAuthenticationFilter(tokenVerifier),
                    UsernamePasswordAuthenticationFilter.class
            );
        } else if (isLocalDevelopment(securityProperties, environment)) {
            http.addFilterBefore(new DevelopmentAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        } else {
            throw new IllegalStateException(
                    "Firebase desabilitado sem autenticacao local explicita; inicializacao recusada por seguranca."
            );
        }

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            CorsProperties properties,
            ApplicationSecurityProperties securityProperties,
            Environment environment
    ) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        var allowedHeaders = new ArrayList<>(List.of("Authorization", "Content-Type"));
        if (isLocalDevelopment(securityProperties, environment)) {
            allowedHeaders.add(DevelopmentAuthenticationFilter.ROLE_HEADER);
            allowedHeaders.add(DevelopmentAuthenticationFilter.USER_HEADER);
        }
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    static boolean isLocalDevelopment(
            ApplicationSecurityProperties properties,
            Environment environment
    ) {
        return properties.developmentAuthEnabled()
                && environment.acceptsProfiles(Profiles.of("local"))
                && !StringUtils.hasText(environment.getProperty("K_SERVICE"));
    }
}
