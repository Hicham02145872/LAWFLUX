package ma.lawflux.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class KeycloakSecurity {
    @Bean
    @Order(1)
    SecurityFilterChain authApi(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/auth/**", "/api/documents/**",
                "/api/dossiers/*/documents", "/api/dossiers/*/acces", "/api/dossiers/*/acces/*")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.GET, "/api/auth/me")
                        .authenticated()
                        .requestMatchers("/api/documents/**", "/api/dossiers/*/documents",
                                "/api/dossiers/*/acces", "/api/dossiers/*/acces/*")
                        .authenticated().anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(jwt -> {}))
                .build();
    }
}
