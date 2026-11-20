package ma.lawflux.documents.infrastructure;

import io.grpc.Status;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import ma.lawflux.documents.application.CallerContext;
import ma.lawflux.documents.application.CallerContextProvider;
import ma.lawflux.platform.grpc.ScaffoldGrpcBoundary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "lawflux.documents", name = "business-enabled", havingValue = "true")
public class DocumentJwtIdentity implements CallerContextProvider {
    private final JwtDecoder decoder;

    public DocumentJwtIdentity(JwtDecoder decoder) { this.decoder = decoder; }

    @Bean
    static JwtDecoder documentJwtDecoder(
            @Value("${lawflux.documents.jwk-set-uri}") String jwkSetUri,
            @Value("${lawflux.documents.issuer-uri}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        var issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(jwt -> {
            OAuth2TokenValidatorResult result = issuerValidator.validate(jwt);
            if (result.hasErrors()) return result;
            if (!"lawflux-web".equals(jwt.getClaimAsString("azp"))) {
                return OAuth2TokenValidatorResult.failure(new org.springframework.security.oauth2.core.OAuth2Error(
                        "invalid_token", "Client OIDC incorrect", null));
            }
            return OAuth2TokenValidatorResult.success();
        });
        return decoder;
    }

    public Jwt jwt() {
        String authorization = ScaffoldGrpcBoundary.AUTHORIZATION_CONTEXT.get();
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            throw Status.UNAUTHENTICATED.withDescription("Jeton requis").asRuntimeException();
        }
        try { return decoder.decode(authorization.substring(7)); }
        catch (JwtException e) {
            throw Status.UNAUTHENTICATED.withDescription("Jeton invalide").asRuntimeException();
        }
    }

    @Override
    public CallerContext current() {
        Jwt jwt = jwt();
        Object access = jwt.getClaim("realm_access");
        Object roles = access instanceof Map<?, ?> map ? map.get("roles") : null;
        List<?> roleList = roles instanceof List<?> list ? list : List.of();
        String method = ScaffoldGrpcBoundary.METHOD_CONTEXT.get();
        boolean staff = roleList.contains("cabinet_manager") || roleList.contains("lawyer")
                || roleList.contains("assistant");
        boolean client = roleList.contains("client");
        if (!staff && !client) throw Status.PERMISSION_DENIED.asRuntimeException();
        if (method != null && method.startsWith("lawflux.documents.v1.DocumentService/") && !staff)
            throw Status.PERMISSION_DENIED.asRuntimeException();
        if (method != null && (method.endsWith("/ShareDocument") || method.endsWith("/UnshareDocument"))
                && !(roleList.contains("cabinet_manager") || roleList.contains("lawyer")))
            throw Status.PERMISSION_DENIED.asRuntimeException();
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            UUID cabinetId = UUID.fromString(jwt.getClaimAsString("cabinet_id"));
            String name = jwt.getClaimAsString("name");
            if (name == null || name.isBlank()) name = jwt.getClaimAsString("preferred_username");
            if (name == null || name.isBlank()) name = userId.toString();
            return new CallerContext(userId, cabinetId, name);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw Status.PERMISSION_DENIED.withDescription("Appartenance au cabinet absente").asRuntimeException();
        }
    }
}
