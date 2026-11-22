package ma.lawflux.cases.domain;

import java.time.Instant;
import java.util.UUID;

public record CaseGrant(UUID userId, AccessKind kind, Instant grantedAt) {}
