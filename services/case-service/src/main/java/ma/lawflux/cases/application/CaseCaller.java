package ma.lawflux.cases.application;

import java.util.Set;
import java.util.UUID;

public record CaseCaller(UUID userId, UUID cabinetId, Set<String> roles) {
    public CaseCaller { roles = Set.copyOf(roles); }
    public boolean hasRole(String role) { return roles.contains(role); }
}
