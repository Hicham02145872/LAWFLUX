package ma.lawflux.platform.config;
import jakarta.servlet.DispatcherType;
import ma.lawflux.platform.web.CorrelationFilter;
import ma.lawflux.platform.grpc.ScaffoldGrpcBoundary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.grpc.server.GlobalServerInterceptor;

@Configuration(proxyBeanMethods = false)
public class PlatformConfiguration {
    @Bean
    CorrelationFilter correlationFilter() { return new CorrelationFilter(); }

    @Bean
    @GlobalServerInterceptor
    ScaffoldGrpcBoundary scaffoldGrpcBoundary() { return new ScaffoldGrpcBoundary(); }

    @Bean
    SecurityFilterChain scaffoldSecurity(HttpSecurity http,
            @Value("${lawflux.scaffold-events-enabled:false}") boolean eventsEnabled) throws Exception {
        // Aucune authentification métier simulée : tout est fermé sauf les sondes techniques.
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> {
                a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                a.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/api/system/services/*").permitAll();
                if (eventsEnabled) a.requestMatchers(HttpMethod.POST, "/api/system/events").permitAll();
                a.anyRequest().denyAll();
            })
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(401)));
        return http.build();
    }
}
