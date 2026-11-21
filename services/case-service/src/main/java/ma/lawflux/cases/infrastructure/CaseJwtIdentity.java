package ma.lawflux.cases.infrastructure;

import io.grpc.Status;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import ma.lawflux.cases.application.CaseCaller;
import ma.lawflux.platform.grpc.ScaffoldGrpcBoundary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
public class CaseJwtIdentity {
    private final JwtDecoder decoder;

    public CaseJwtIdentity(JwtDecoder decoder) { this.decoder = decoder; }

    @Bean
    static JwtDecoder caseJwtDecoder(@Value("${lawflux.cases.jwk-set-uri}") String keys,
                                     @Value("${lawflux.cases.issuer-uri}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(keys).build();
        var issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(jwt -> {
            OAuth2TokenValidatorResult result = issuerValidator.validate(jwt);
            if (result.hasErrors()) return result;
            return "lawflux-web".equals(jwt.getClaimAsString("azp"))
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Client OIDC incorrect", null));
        });
        return decoder;
    }

    public CaseCaller current() {
        String header = ScaffoldGrpcBoundary.AUTHORIZATION_CONTEXT.get();
        if (header == null || !header.startsWith("Bearer ") || header.length() <= 7)
            throw Status.UNAUTHENTICATED.withDescription("Jeton requis").asRuntimeException();
        Jwt jwt;
        try { jwt = decoder.decode(header.substring(7)); }
        catch (JwtException e) {
            throw Status.UNAUTHENTICATED.withDescription("Jeton invalide").asRuntimeException();
        }
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            UUID cabinetId = UUID.fromString(jwt.getClaimAsString("cabinet_id"));
            Object access = jwt.getClaim("realm_access");
            Object rawRoles = access instanceof Map<?, ?> map ? map.get("roles") : null;
            Set<String> roles = rawRoles instanceof List<?> list
                    ? list.stream().filter(String.class::isInstance).map(String.class::cast)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet())
                    : Set.of();
            return new CaseCaller(userId, cabinetId, roles);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw Status.PERMISSION_DENIED.withDescription("Identité de cabinet absente").asRuntimeException();
        }
    }
}
