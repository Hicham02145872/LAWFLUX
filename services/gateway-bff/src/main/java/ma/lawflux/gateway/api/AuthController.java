package ma.lawflux.gateway.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/me")
    public Profile me(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        List<String> roles = List.of();
        Object access = jwt.getClaim("realm_access");
        if (access instanceof java.util.Map<?, ?> map && map.get("roles") instanceof List<?> raw) {
            roles = raw.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        }
        return new Profile(jwt.getSubject(), jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"), roles);
    }

    public record Profile(String id, String username, String email, List<String> roles) {}
}
